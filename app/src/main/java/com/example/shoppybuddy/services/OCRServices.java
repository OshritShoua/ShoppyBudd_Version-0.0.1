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

public class OCRServices {

    private static final String TAG = "ShoppyBuddy.java";
    private static HashMap<String, Character> _currencyCodesToSymbols = new HashMap<>();
    private static HashMap<Character, String> _currencySymbolsToCodes = new HashMap<>();
    private static HashMap<String, String> _similarCurrencyCodes = new HashMap<>();
    private static final int NO_DOUBLE_WERE_FOUND = -1;
    private static final int MORE_THAN_ONE_DOUBLE_WERE_FOUND = -2;
    private static final int UNVALID_INDEX = -1;


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
            }

    private String _currentTextCaptured = null;

    public String GetCurrentPriceCaptured() {
        return _currentPriceCaptured;
    }

    private String _currentPriceCaptured;

    public OCRServices()
    {
        init();
    }

    public String GetCurrentTextCaptured() {
        return _currentTextCaptured;
    }

    private void init()
    {
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

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
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

//    todo: think of adding some of the actions here to the method from CartReviewActivity
//    private Bitmap getAdjustedBitmapFromPhoto()
//    {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 4;
//
//        Bitmap bitmap = BitmapFactory.decodeFile(_imageFilePath, options);
//
//        try {
//            ExifInterface exif = new ExifInterface(_imageFilePath);
//            int orientationMode = exif.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_NORMAL);
//
//            Log.v(TAG, "Orient: " + orientationMode);
//
//            int rotate = 0;
//
//            switch (orientationMode) {
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    rotate = 90;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    rotate = 180;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    rotate = 270;
//                    break;
//            }
//
//            Log.v(TAG, "Rotation: " + rotate);
//
//            if (rotate != 0) {
//
//                // Getting width & height of the given image.
//                int w = bitmap.getWidth();
//                int h = bitmap.getHeight();
//
//                // Setting pre rotate
//                Matrix mtx = new Matrix();
//                mtx.preRotate(rotate);
//
//                // Rotating Bitmap
//                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
//            }
//
//            // Convert to ARGB_8888, required by tess
//            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        } catch (IOException e) {
//            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
//        }
//
//        return bitmap;
//    }

    public MutablePair<ArrayList<String>, Boolean> getOCRResult(String baseCurrencyCode)
    {
        MutablePair<ArrayList<String>, Boolean> OCRResults = new MutablePair(new ArrayList<String>(), false);
        ArrayList<String> pricesWithCurrencyInResults = new ArrayList<>();
        ArrayList<String> pricesInResults = new ArrayList<>();
        String filteredText = getFilteredText(_currentTextCaptured);
        ArrayList<String> results = new ArrayList<>(Arrays.asList(filteredText.split("[X ]", -1)));

        for(int i = 0; i < results.size(); i++)
        {
            String res = results.get(i);

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

            //todo: add hiuristics : search s/e, search $/.. in the res before
            int codeIndex;
            if((codeIndex = indexOfAny(res, _similarCurrencyCodes.keySet().toString())) != UNVALID_INDEX)
            {
                String resWithoutCodes = res.replaceAll(_similarCurrencyCodes.keySet().toString(), " ");
                if(foundPriceInText(resWithoutCodes) && resWithoutCodes.contains(".")) //Sanity check + make sure this price is double
                {
                   List<String> currencySymbols = Arrays.asList((_similarCurrencyCodes.get(String.valueOf(res.charAt(codeIndex)))).split(",", -1));
                   for(String currency : currencySymbols)
                   {
                       if(currency.isEmpty())
                       {
                           continue;
                       }

                       results.add(res.replace(res.charAt(codeIndex), currency.charAt(0)));
                   }
                   OCRResults.setRight(true);
                }
                continue;
            }

            int currencyIndex;
            int resIndex = results.indexOf(res);
            if(resIndex > 0)
            {
                if((currencyIndex = indexOfAny(results.get(resIndex -1), _currencyCodesToSymbols.values().toString())) != UNVALID_INDEX && results.get(resIndex -1).length() == 1)
                {
                    res = results.get(resIndex -1).charAt(currencyIndex) + res;
                    // todo: OCRResults.second = true;
                }
            }

            if((currencyIndex = indexOfAny(res, _currencyCodesToSymbols.values().toString())) != UNVALID_INDEX)
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
                    if(( index =  priceWithBaseCurrencyIndex(pricesWithCurrencyInResults, baseCurrencyCode)) != UNVALID_INDEX)
                    {
                        OCRResults.getLeft().add(pricesWithCurrencyInResults.get(index));
                    }
                    else
                    {
                        OCRResults.getLeft().add(pricesWithCurrencyInResults.get(0));
                    }
                }
                else
                {
                    int index;
                    if(( index =  priceWithBaseCurrencyIndex(pricesWithCurrencyInResults, baseCurrencyCode)) != UNVALID_INDEX)
                    {
                        for(String price : pricesWithCurrencyInResults)
                        {
                            if(price.contains(baseCurrencyCode))
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

        deleteDuplicateValues(OCRResults.getLeft());

        return OCRResults;
    }

    private int priceWithBaseCurrencyIndex(ArrayList<String> prices, String baseCurrency)
    {
        int result = UNVALID_INDEX;

        for(String price : prices)
        {
            if(price.contains(baseCurrency))
            {
                return prices.indexOf(price);
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
            if(foundedCurrencies.toString().indexOf(price.charAt(0)) == UNVALID_INDEX)
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

//    //todo: implement
//    private String applyHeuristicsOnText(String filteredText)
//    {
//        return filteredText;
//    }

    //todo: maybe remove to util class
    private boolean foundPriceInText(String filteredText)
    {
        boolean priceWasFound = false;

        try {
            Double.parseDouble(filteredText);
            priceWasFound = true;
        } catch (NumberFormatException e)
        {
           //If we got here, price wasn't found in filtered text
        }

        return priceWasFound;
    }

    //todo: maybe remove to util class
    private int numberOfDigitsRightToComma(String text)
    {
        int digitsCounter = 0;
        int i = text.indexOf(",") + 1;

        for(; i < text.length(); i++)
        {
            digitsCounter++;
        }

        return digitsCounter;
    }

    //todo: maybe remove to util class
    private static int indexOfAny(String str, String searchChars) {
        if (isEmpty(str) || isEmpty(searchChars)){
            return UNVALID_INDEX;
        }
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            for (int j = 0; j < searchChars.length(); j++) {
                if (searchChars.charAt(j) == ch) {
                    return i;
                }
            }
        }
        return UNVALID_INDEX;
    }

    //todo: maybe remove to util class
    private static boolean isEmpty(String array) {
        if (array == null || array.length() == 0) {
            return true;
        }
        return false;
    }
}
