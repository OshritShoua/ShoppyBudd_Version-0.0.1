package com.example.shoppybuddy.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.common.primitives.Chars;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.shoppybuddy.utils.Constants.INVALID_INDEX;
import static com.example.shoppybuddy.utils.Constants.MORE_THAN_ONE_DOUBLE_WERE_FOUND;
import static com.example.shoppybuddy.utils.Constants.NO_DOUBLE_WERE_FOUND;
import static com.example.shoppybuddy.utils.Constants.TAG;
import static com.example.shoppybuddy.utils.PriceStringUtils.foundDoublePriceInText;
import static com.example.shoppybuddy.utils.PriceStringUtils.foundPriceInText;
import static com.example.shoppybuddy.utils.PriceStringUtils.numberOfDigitsRightToComma;
import static com.example.shoppybuddy.utils.PriceStringUtils.numberOfDigitsRightToPoint;
import static org.apache.commons.lang3.StringUtils.indexOfAny;

public class OCRServices {
    private static HashMap<String, Character> _currencyCodesToSymbols = new HashMap<>();
    private static HashMap<Character, String> _currencySymbolsToCodes = new HashMap<>();
    private static HashMap<String, String> _similarCurrencyCodes = new HashMap<>();
    private static String _similarCurrencyCodesString;
    private static String _currencyCodesToSymbolsString;
    private String _currentTextCaptured = null;
    private boolean _currentTextSuccess = true;
    private String filteredText;
    private ArrayList<String> plaitedFilteredText;
    private ArrayList<MutablePair<String, Boolean>>  plaitedTextPairs;
    private String currentBaseCurrency;
    private MutablePair<ArrayList<String>, Boolean> OCRResults;
    private ArrayList<MutablePair<String, Boolean>> OCRResultsTemp;
    private ArrayList<MutablePair<String, Boolean>> pricesWithCurrencyInResults;
    private ArrayList<MutablePair<String, Boolean>> pricesInResults;
    static
    {
        _currencySymbolsToCodes.put('€', "EUR");
        _currencySymbolsToCodes.put('₪', "ILS");
        _currencySymbolsToCodes.put('¥', "JPY");
        _currencySymbolsToCodes.put('£', "GBP");
        _currencySymbolsToCodes.put('$', "USD");


        _currencyCodesToSymbols.put("EUR", '€');
        _currencyCodesToSymbols.put("ILS", '₪');
        _currencyCodesToSymbols.put("JPY", '¥');
        _currencyCodesToSymbols.put("GBP", '£');
        _currencyCodesToSymbols.put("USD", '$');

        _similarCurrencyCodes.put("E", "£,€");
        _similarCurrencyCodes.put("S", "$,");
        _similarCurrencyCodes.put("n", "₪,");
        _similarCurrencyCodes.put("N", "₪,");
        _similarCurrencyCodes.put("R", "₪,");
        _similarCurrencyCodes.put("D", "₪,");
        _similarCurrencyCodes.put("Y", "¥,");
        _similarCurrencyCodes.put("y", "¥,");
        _similarCurrencyCodes.put("f", "£,");

        _similarCurrencyCodesString = _similarCurrencyCodes.keySet().toString().replaceAll(" ", "");
        _currencyCodesToSymbolsString = _currencyCodesToSymbols.values().toString().replaceAll(" ", "");

    }

    public static HashMap<Character, String> getSymbolsToCodesMapping()
    {
        return _currencySymbolsToCodes;
    }

    public static HashMap<String, Character> getCodesToSymbolsMapping()
    {
        return _currencyCodesToSymbols;
    }

    public void GetTextFromCapturedImage(Context appContext, Context context, Uri capturedImageUri)
    {
        TextRecognizer textDetector = new TextRecognizer.Builder(appContext).build();
        _currentTextCaptured = "";
        _currentTextSuccess = true;

        try {
            Bitmap bitmap = decodeBitmapUri(context, capturedImageUri);
            if (textDetector.isOperational() && bitmap != null)
            {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = textDetector.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + " ";
                        }
                    }
                }
                if (textBlocks.size() == 0)
                {
                    _currentTextSuccess = false;
                    _currentTextCaptured = "Scan Failed: Found nothing to scan";
                } else {
                    _currentTextCaptured = words;
                    System.out.println(words);
                }
            }
            else
            {
                _currentTextSuccess = false;
                _currentTextCaptured = "Could not set up the detector!";
            }
        } catch (Exception e) {
            _currentTextSuccess = false;
            Log.e(TAG, "Failed to load Image");
        }
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException
    {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    public MutablePair<ArrayList<String>, Boolean> getOCRResult(String baseCurrencyCode)
    {
        OCRResults = new MutablePair(new ArrayList<String>(), false);
        if(_currentTextSuccess)
        {
            currentBaseCurrency = baseCurrencyCode;
            OCRResultsTemp = new ArrayList<>();
            pricesWithCurrencyInResults = new ArrayList<>();
            pricesInResults = new ArrayList<>();
            filteredText = getFilteredText(_currentTextCaptured);

            plaitedFilteredText = new ArrayList<>(Arrays.asList(filteredText.split("((?<=X)|(?=X)|(?<= )|(?= ))", - 1)));
            plaitedTextPairs = initTextResults(plaitedFilteredText);

            sortResultsFromPlaitedTextPairs();
            reviewResultsLists();
            checkForHeuristicResults();
            deleteDuplicateValues(OCRResults.getLeft());
        }

        return OCRResults;
    }

    private Boolean heuristicInPrices()
    {
        Boolean isThereHeuristic = false;

        for(MutablePair<String, Boolean> res : pricesWithCurrencyInResults)
        {
            if(res.right == true)
            {
                isThereHeuristic = true;
            }
        }

      return isThereHeuristic;
    }

    private void checkForHeuristicResults()
    {
        Boolean isThereHeuristic = false;

        for(MutablePair<String, Boolean> res : OCRResultsTemp)
        {
            if(res.right == true)
            {
                isThereHeuristic = true;
            }

            OCRResults.left.add(res.left);
        }

        OCRResults.setRight(isThereHeuristic);
    }

    private ArrayList<MutablePair<String, Boolean>> initTextResults(ArrayList<String> plaitedText)
    {
        ArrayList<MutablePair<String, Boolean>> result = new ArrayList<>();

        for(String res : plaitedText)
        {
            if(!res.isEmpty() && !res.equals(" "))
            {
                result.add(new MutablePair<>(res.replaceAll(" ", ""), false));
            }
        }

        return result;
    }

    private void sortResultsFromPlaitedTextPairs()
    {
        for(int i = 0; i < plaitedTextPairs.size(); i++)
        {
            String res = plaitedTextPairs.get(i).left;
            Boolean isHeuristicRes = plaitedTextPairs.get(i).right;

            if(res.isEmpty() || res.contains("%") || res.contains("X"))
            {
                continue;
            }

            // If '.' was recognized as ','
            if(res.contains("."))
            {
                if(numberOfDigitsRightToPoint(res) > 2)
                {
                    res = res.replace(".", ",");
                }
            }

            // ',' - if more than 2 from right - thousands, else double(.)
            if(res.contains(","))
            {
                if(numberOfDigitsRightToComma(res) == 2)
                {
                    res = res.replace(",", ".");
                }
                else
                {
                    res = res.replace(",", "");
                }
            }


            boolean shouldContinue = applyHeuristicsOnText(res, i);
            if(shouldContinue)
            {
                continue;
            }

            int currencyIndex;
            // ****************     Filling the results lists     ****************
            if((currencyIndex = indexOfAny(res, _currencyCodesToSymbolsString)) != INVALID_INDEX)
            {
                String resWithoutCurrency = res.replaceAll(_currencyCodesToSymbolsString, " ");
                if(foundPriceInText(resWithoutCurrency)) //Sanity check
                {
                    pricesWithCurrencyInResults.add(new MutablePair<>(res.charAt(currencyIndex) + resWithoutCurrency, isHeuristicRes));
                }
            }
            else if(foundPriceInText(res))
            {
                pricesInResults.add(new MutablePair<>("-" + res, isHeuristicRes));
            }
        }
    }

    private void reviewResultsLists()
    {
        if(pricesWithCurrencyInResults.size() > 0)
        {
            if(pricesWithCurrencyInResults.size() == 1)
            {
                OCRResultsTemp.add(pricesWithCurrencyInResults.get(0));
            }
            else
            {
                if(numberOfCurrenciesInPrices(pricesWithCurrencyInResults) == 1)
                {
                    for(MutablePair<String, Boolean> price : pricesWithCurrencyInResults)
                    {
                        OCRResultsTemp.add(price);
                    }
                }
                else if(numberOfCurrenciesInPrices(pricesWithCurrencyInResults) == pricesWithCurrencyInResults.size())
                {
                    int index;
                    if(( index =  priceWithBaseCurrencyIndex(pricesWithCurrencyInResults, currentBaseCurrency)) != INVALID_INDEX)
                    {
                        OCRResultsTemp.add(pricesWithCurrencyInResults.get(index));
                    }
                    else
                    {
                        if(heuristicInPrices())
                        {
                            //If heuristic was used - return all results
                            for(MutablePair<String, Boolean> price : pricesWithCurrencyInResults)
                            {
                                OCRResultsTemp.add(price);
                            }
                        }
                        else
                        {
                            OCRResultsTemp.add(pricesWithCurrencyInResults.get(0));
                        }
                    }
                }
                else
                {
                    if((priceWithBaseCurrencyIndex(pricesWithCurrencyInResults, currentBaseCurrency)) != INVALID_INDEX)
                    {
                        for(MutablePair<String, Boolean> price : pricesWithCurrencyInResults)
                        {
                            if(price.left.contains(currentBaseCurrency))
                            {
                                OCRResultsTemp.add(price);
                            }
                        }
                    }
                    else
                    {
                        for(MutablePair<String, Boolean> price : pricesWithCurrencyInResults)
                        {
                            OCRResultsTemp.add(price);
                        }
                    }
                }
            }
        }
        else
        {
            if(pricesInResults.size() == 1)
            {
                OCRResultsTemp.add(pricesInResults.get(0));
            }
            else if(pricesInResults.size() > 0)
            {
                int indexInPrices = isThereOnlyOneDoubleInPrices(pricesInResults);

                //case with no doubles
                if(indexInPrices == NO_DOUBLE_WERE_FOUND)
                {
                    for(MutablePair<String, Boolean> price : pricesInResults)
                    {
                        OCRResultsTemp.add(price);
                    }
                }
                //case with more than one double
                else if(indexInPrices == MORE_THAN_ONE_DOUBLE_WERE_FOUND)
                {
                    for(MutablePair<String, Boolean> price : pricesInResults)
                    {
                        if(price.left.contains("."))
                        {
                            OCRResultsTemp.add(price);
                        }
                    }
                }
                //case with just one double
                else
                {
                    OCRResultsTemp.add(pricesInResults.get(indexInPrices));
                }
            }
        }
    }

    private int priceWithBaseCurrencyIndex(ArrayList<MutablePair<String, Boolean>> prices, String baseCurrency)
    {
        int result = INVALID_INDEX;

        for(MutablePair<String, Boolean> price : prices)
        {
            if(price.left.contains(baseCurrency))
            {
                result = prices.indexOf(price);
            }
        }

        return result;
    }

    private void deleteDuplicateValues(ArrayList<String> prices)
    {
        // add elements to hs, including duplicates
        Set<String> hs = new HashSet<>();
        hs.addAll(prices);
        prices.clear();
        prices.addAll(hs);
        if(prices.size() > 1)
        {
            deleteDuplicateValuesWithNotSameStringValue(prices);
        }
    }

    private void deleteDuplicateValuesWithNotSameStringValue(ArrayList<String> prices)
    {
        for(int i = 0; i < prices.size(); i++)
        {
            for(int j = 0; j < prices.size(); j++)
            {
                if(i != j)
                {
                    Double price1 = Double.valueOf(prices.get(i).substring(1));
                    Double price2 = Double.valueOf(prices.get(j).substring(1));
                    if(price1.equals(price2))
                    {
                        prices.remove(prices.get(j));
                    }
                }
            }
        }
    }

    private int numberOfCurrenciesInPrices(ArrayList<MutablePair<String, Boolean>> prices)
    {
        StringBuilder foundedCurrencies = new StringBuilder();

        for(MutablePair<String, Boolean> price : prices)
        {
            if(foundedCurrencies.toString().indexOf(price.left.charAt(0)) == INVALID_INDEX)
            {
                foundedCurrencies.append(price.left.charAt(0));
            }
        }

        return foundedCurrencies.length();
    }

    private int isThereOnlyOneDoubleInPrices(ArrayList<MutablePair<String, Boolean>> pricesList)
    {
        Object[] priceArray = pricesList.toArray();
        boolean doublePriceWasFound = false;
        int result = NO_DOUBLE_WERE_FOUND;

        for(int i = 0; i < priceArray.length; i++)
        {
            if(((MutablePair<String, Boolean>)priceArray[i]).left.contains("."))
            {
                if(!doublePriceWasFound)
                {
                    doublePriceWasFound = true;
                    result = i;
                }
                else
                {
                    result = MORE_THAN_ONE_DOUBLE_WERE_FOUND;
                    break;
                }
            }
        }

        //return values mean: - 1: double was not founded, -2: there are more than one double
        return result;
    }

    @NonNull
    private String getFilteredText(String rawRecognizedText)
    {
        Log.v(TAG, "OCRED TEXT: " + rawRecognizedText);

        rawRecognizedText.toUpperCase();
        rawRecognizedText = rawRecognizedText.trim();
        ArrayList<Character> whitelist = new ArrayList<>(Chars.asList(Chars.concat(" -%.,1234567890".toCharArray(), Chars.toArray(_currencyCodesToSymbols.values()), _similarCurrencyCodesString.toCharArray())));

        StringBuilder builder = new StringBuilder();
        boolean foundMatch;
        for (char recognizedChar : rawRecognizedText.toCharArray()) {
            foundMatch = false;
            for (char approvedChar : whitelist) {
                if (recognizedChar == approvedChar) {
                    builder.append(recognizedChar);
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch)
                builder.append('X');
        }

        return addSpacesBetweenWhitelistSigns(builder.toString());
    }

    private String addSpacesBetweenWhitelistSigns(String text)
    {
        String result = text;

       for(Object sign : _similarCurrencyCodes.keySet().toArray())
       {
           result = result.replaceAll((String)sign, " " + sign + " ");
       }

       return result;
    }

    private boolean applyHeuristicsOnText(String text, int index)
    {
        boolean shouldContinue = false;
        int codeIndex;
        int currencyIndex;

//         //NOTE : This section is in comment because it is not relevant any more.. no way that similar is together with price
//        // heuristic : search for similar currency codes in current result - and save the result with the relevant currencies
//        if((codeIndex = indexOfAny(text, _similarCurrencyCodesString)) != INVALID_INDEX)
//        {
//            String resWithoutCodes = text.replaceAll(_similarCurrencyCodesString, " ");
//            if(foundDoublePriceInText(resWithoutCodes)) //Sanity check
//            {
//                List<String> currencySymbols = Arrays.asList((_similarCurrencyCodes.get(String.valueOf(text.charAt(codeIndex)))).split(",", - 1));
//                for(String currency : currencySymbols)
//                {
//                    if(currency.isEmpty())
//                    {
//                        continue;
//                    }
//
//                    plaitedTextPairs.add(new MutablePair<>(text.replace(text.charAt(codeIndex), currency.charAt(0)), true));
//                }
//                shouldContinue = true;
//            }
//        }

        // heuristic : if no currency in current result & and the result is a price & its not the first result
        if((currencyIndex = indexOfAny(text, _currencyCodesToSymbolsString)) == INVALID_INDEX && foundPriceInText(text) && index > 0)
        {
            // check if the previous result contains one currency code
            if((currencyIndex = indexOfAny(plaitedTextPairs.get(index - 1).left, _currencyCodesToSymbolsString)) != INVALID_INDEX && plaitedTextPairs.get(index - 1).left.length() == 1)
            {
                plaitedTextPairs.add(new MutablePair<>(plaitedTextPairs.get(index - 1).left.charAt(currencyIndex) + text, true));
                shouldContinue = true;
            }
            // or if the previous result contains one character that is similar to currency code
            else if((codeIndex = indexOfAny(plaitedTextPairs.get(index - 1).left, _similarCurrencyCodesString)) != INVALID_INDEX && plaitedTextPairs.get(index - 1).left.length() == 1)
            {
                List<String> currencySymbols = Arrays.asList((_similarCurrencyCodes.get(String.valueOf(plaitedTextPairs.get(index - 1).left.charAt(codeIndex)))).split(",", - 1));
                for(String currency : currencySymbols)
                {
                    if(currency.isEmpty())
                    {
                        continue;
                    }

                    String resWithoutCodes = text.replaceAll(_similarCurrencyCodesString, " ");
                    if(foundDoublePriceInText(resWithoutCodes)) //Sanity check
                    {
                        plaitedTextPairs.add(new MutablePair<>(currency + text, true));
                        shouldContinue = true;
                    }
                }
            }
        }

        return shouldContinue;
    }
}
