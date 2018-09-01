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
import static org.apache.commons.lang3.StringUtils.indexOfAny;

public class OCRServices {
    private static HashMap<String, Character> _currencyCodesToSymbols = new HashMap<>();
    private static HashMap<Character, String> _currencySymbolsToCodes = new HashMap<>();
    private static HashMap<String, String> _similarCurrencyCodes = new HashMap<>();
    private String _currentTextCaptured = null;
    private String filteredText;
    private ArrayList<String> plaitedFilteredText;
    private String currentBaseCurrency;
    private MutablePair<ArrayList<String>, Boolean> OCRResults;
    private ArrayList<String> pricesWithCurrencyInResults;
    private ArrayList<String> pricesInResults;
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
                    //todo: other value  - send error
                    _currentTextCaptured = "Scan Failed: Found nothing to scan";
                } else {
                    _currentTextCaptured = words;
                    System.out.println(words);
                }
            }
            else
            {
                _currentTextCaptured = "Could not set up the detector!";
            }
        } catch (Exception e) {
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
        currentBaseCurrency = baseCurrencyCode;
        OCRResults = new MutablePair(new ArrayList<String>(), false);
        pricesWithCurrencyInResults = new ArrayList<>();
        pricesInResults = new ArrayList<>();
        filteredText = getFilteredText(_currentTextCaptured);
        plaitedFilteredText = new ArrayList<>(Arrays.asList(filteredText.split("[X ]", -1)));

        sortResultsFromPlaitedFilteredText();
        reviewResultsLists();
        deleteDuplicateValues(OCRResults.getLeft());

        return OCRResults;
    }

    private void sortResultsFromPlaitedFilteredText()
    {
        for(int i = 0; i < plaitedFilteredText.size(); i++)
        {
            String res = plaitedFilteredText.get(i);

            if(res.isEmpty())
            {
                continue;
            }
            else if(res.contains("%"))
            {
                continue;
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
            if((currencyIndex = indexOfAny(res, _currencyCodesToSymbols.values().toString())) != INVALID_INDEX)
            {
                String resWithoutCurrency = res.replaceAll(_currencyCodesToSymbols.values().toString(), " ");
                if(foundPriceInText(resWithoutCurrency)) //Sanity check
                {
                    pricesWithCurrencyInResults.add(res.charAt(currencyIndex) + resWithoutCurrency);
                }
            }
            else if(foundPriceInText(res))
            {
                pricesInResults.add("-" + res);
            }
        }
    }

    private void reviewResultsLists()
    {
        if(pricesWithCurrencyInResults.size() > 0)
        {
            if(pricesWithCurrencyInResults.size() == 1)
            {
                OCRResults.getLeft().add(pricesWithCurrencyInResults.get(0));
            }
            else
            {
                if(numberOfCurrenciesInPrices(pricesWithCurrencyInResults) == 1)
                {
                    for(String price : pricesWithCurrencyInResults)
                    {
                        OCRResults.getLeft().add(price);
                    }
                }
                else if(numberOfCurrenciesInPrices(pricesWithCurrencyInResults) == pricesWithCurrencyInResults.size())
                {
                    int index;
                    if(( index =  priceWithBaseCurrencyIndex(pricesWithCurrencyInResults, currentBaseCurrency)) != INVALID_INDEX)
                    {
                        OCRResults.getLeft().add(pricesWithCurrencyInResults.get(index));
                    }
                    else
                    {
                        //todo: maybe return all when heuristic is true
                        OCRResults.getLeft().add(pricesWithCurrencyInResults.get(0));
                    }
                }
                else
                {
                    if((priceWithBaseCurrencyIndex(pricesWithCurrencyInResults, currentBaseCurrency)) != INVALID_INDEX)
                    {
                        for(String price : pricesWithCurrencyInResults)
                        {
                            if(price.contains(currentBaseCurrency))
                            {
                                OCRResults.getLeft().add(price);
                            }
                        }
                    }
                    else
                    {
                        for(String price : pricesWithCurrencyInResults)
                        {
                            OCRResults.getLeft().add(price);
                        }
                    }
                }
            }
        }
        else
        {
            if(pricesInResults.size() == 1)
            {
                OCRResults.getLeft().add(pricesInResults.get(0));
            }
            else if(pricesInResults.size() > 0)
            {
                int indexInPrices = isThereOnlyOneDoubleInPrices(pricesInResults);

                //case with no doubles
                if(indexInPrices == NO_DOUBLE_WERE_FOUND)
                {
                    for(String price : pricesInResults)
                    {
                        OCRResults.getLeft().add(price);
                    }
                }
                //case with more than one double
                else if(indexInPrices == MORE_THAN_ONE_DOUBLE_WERE_FOUND)
                {
                    for(String price : pricesInResults)
                    {
                        if(price.contains("."))
                        {
                            OCRResults.getLeft().add(price);
                        }
                    }
                }
                //case with just one double
                else
                {
                    OCRResults.getLeft().add(pricesInResults.get(indexInPrices));
                }
            }
        }
    }

    private int priceWithBaseCurrencyIndex(ArrayList<String> prices, String baseCurrency)
    {
        int result = INVALID_INDEX;

        for(String price : prices)
        {
            if(price.contains(baseCurrency))
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
    }

    private int numberOfCurrenciesInPrices(ArrayList<String> prices)
    {
        StringBuilder foundedCurrencies = new StringBuilder();

        for(String price : prices)
        {
            if(foundedCurrencies.toString().indexOf(price.charAt(0)) == INVALID_INDEX)
            {
                foundedCurrencies.append(price.charAt(0));
            }
        }

        return foundedCurrencies.length();
    }

    private int isThereOnlyOneDoubleInPrices(ArrayList<String> pricesList)
    {
        Object[] priceArray = pricesList.toArray();
        boolean doublePriceWasFound = false;
        int result = NO_DOUBLE_WERE_FOUND;

        for(int i = 0; i < priceArray.length; i++)
        {
            if(((String)priceArray[i]).contains("."))
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

        //return values mean: -1: double was not founded, -2: there are more than one double
        return result;
    }

    @NonNull
    private String getFilteredText(String rawRecognizedText)
    {
        Log.v(TAG, "OCRED TEXT: " + rawRecognizedText);

        rawRecognizedText.toUpperCase();
        rawRecognizedText = rawRecognizedText.trim();
        ArrayList<Character> whitelist = new ArrayList<>(Chars.asList(Chars.concat(" %.,1234567890".toCharArray(), Chars.toArray(_currencyCodesToSymbols.values()), _similarCurrencyCodes.keySet().toString().toCharArray())));

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

        return builder.toString();
    }

    private boolean applyHeuristicsOnText(String text, int index)
    {
        boolean shouldContinue = false;

        // heuristic : search for similar currency codes in current result - and save the result with the relevant currencies
        int codeIndex;
        if((codeIndex = indexOfAny(text, _similarCurrencyCodes.keySet().toString())) != INVALID_INDEX)
        {
            String resWithoutCodes = text.replaceAll(_similarCurrencyCodes.keySet().toString(), " ");
            if(foundDoublePriceInText(resWithoutCodes)) //Sanity check
            {
                List<String> currencySymbols = Arrays.asList((_similarCurrencyCodes.get(String.valueOf(text.charAt(codeIndex)))).split(",", -1));
                for(String currency : currencySymbols)
                {
                    if(currency.isEmpty())
                    {
                        continue;
                    }

                    plaitedFilteredText.add(text.replace(text.charAt(codeIndex), currency.charAt(0)));
                }

                OCRResults.setRight(true);
                shouldContinue = true;
            }
        }

        // heuristic : if no currency in current result & and the result is a price & its not the first result
        int currencyIndex;
        if((currencyIndex = indexOfAny(text, _currencyCodesToSymbols.values().toString())) == INVALID_INDEX && foundPriceInText(text) && index > 0)
        {
            // check if the previous result contains one currency code
            if((currencyIndex = indexOfAny(plaitedFilteredText.get(index -1), _currencyCodesToSymbols.values().toString())) != INVALID_INDEX && plaitedFilteredText.get(index -1).length() == 1)
            {
                plaitedFilteredText.add(plaitedFilteredText.get(index -1).charAt(currencyIndex) + text);
                OCRResults.setRight(true);
                shouldContinue = true;
            }
            // or if the previous result contains one character that is similar to currency code
            else if((codeIndex = indexOfAny(plaitedFilteredText.get(index -1), _similarCurrencyCodes.keySet().toString())) != INVALID_INDEX && plaitedFilteredText.get(index -1).length() == 1)
            {
                List<String> currencySymbols = Arrays.asList((_similarCurrencyCodes.get(String.valueOf(plaitedFilteredText.get(index -1).charAt(codeIndex)))).split(",", -1));
                for(String currency : currencySymbols)
                {
                    if(currency.isEmpty())
                    {
                        continue;
                    }

                    String resWithoutCodes = text.replaceAll(_similarCurrencyCodes.keySet().toString(), " ");
                    if(foundDoublePriceInText(resWithoutCodes)) //Sanity check
                    {
                        plaitedFilteredText.add(currency + text);
                    }
                }

                OCRResults.setRight(true);
                shouldContinue = true;
            }
        }

        return shouldContinue;
    }
}
