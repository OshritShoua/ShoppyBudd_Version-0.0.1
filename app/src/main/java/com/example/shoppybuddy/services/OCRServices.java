package com.example.shoppybuddy.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.common.primitives.Chars;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class OCRServices {

    private static final String TAG = "ShoppyBuddy.java";
    private static HashMap<String, Character> _currencyCodesToSymbols = new HashMap<>();
    private static HashMap<Character, String> _currencySymbolsToCodes = new HashMap<>();

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

    public Pair<List<String>, Boolean> getOCRResult(String baseCurrencyCode)
    {
        return new Pair<>(Arrays.asList("-99.99"), true);//, "X87.45", "X50.00"
//        String[] OCRResults = {"99.99"};
//        String text = null;
//        List<String> pricesWithCurrencyInResults = new ArrayList<>();
//        ArrayList<String> pricesInResults = new ArrayList<>();
//        String filteredText = getFilteredText(_currentTextCaptured);
//        String[] results = filteredText.split("[X ]", -1);
//
//        for(String res : results)
//        {
//            if(res.isEmpty())
//            {
//                continue;
//            }
//            else if(res.contains("%"))
//            {
//                continue;
//            }
//
//            String[] isContainCurrencySymbol = res.split(_currencyCodesToSymbols.values().toString(), -1);
//
//            if(isContainCurrencySymbol.length > 1)
//            {
//                String resWithoutCurrency = res.replaceAll(_currencyCodesToSymbols.values().toString(), " ");
//                if(foundPriceInText(resWithoutCurrency))
//                {
//                    pricesWithCurrencyInResults.add(res);
//                }
//            }
//            else if(foundPriceInText(res))
//            {
//                pricesInResults.add(res);
//            }
//        }
//
//        if(pricesWithCurrencyInResults.size() > 0)
//        {
//            if(pricesWithCurrencyInResults.size() == 1)
//            {
//                text = pricesWithCurrencyInResults.get(0).replaceAll(_currencyCodesToSymbols.values().toString(), " ");;
//            }
//            else
//            {
//                for(String price : pricesWithCurrencyInResults)
//                {
//                    if(price.contains(_currencyCodesToSymbols.get(baseCurrencyCode).toString()))
//                    {
//                        text = price.replaceAll(_currencyCodesToSymbols.values().toString(), " ");
//                        //todo: what if there are two with same currency?
//                        break;
//                    }
//                }
//
//                if(text == null)
//                {
//                    //todo: if there are captured prices but not in the base currency?!
//                }
//            }
//
//        }
//        else
//        {
//            if(pricesInResults.size() == 1)
//            {
//                text = pricesInResults.get(0);
//            }
//            else if(pricesInResults.size() > 0)
//            {
//                int indexInPrice;
//
//                if((indexInPrice = isThereOnlyOneDoubleInPrices(pricesInResults)) != -1)
//                {
//                    text = pricesInResults.get(indexInPrice);
//                }
//                else
//                {
//                    //todo: complete - case with more then one double prices in picture
//                }
//            }
//
//            if(text == null)
//            {
//                // todo: maybe apply heuristics and check again before returning false
//                return OCRResults;
//            }
//        }
//
//        //Health check for all uncovered cases
//        if(text == null)
//        {
//            return OCRResults;
//        }
//
//        _currentPriceCaptured = text;
//
//        return OCRResults;
    }

    private int isThereOnlyOneDoubleInPrices(ArrayList<String> pricesList)
    {
        Object[] priceArray = pricesList.toArray();
        boolean doublePriceWasFound = false;
        int result = -1;

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
                    break;
                }

            }
        }

        return result;
    }

    @NonNull
    private String getFilteredText(String rawRecognizedText)
    {
        Log.v(TAG, "OCRED TEXT: " + rawRecognizedText);

        rawRecognizedText = rawRecognizedText.trim();
        ArrayList<Character> whitelist = new ArrayList<>(Chars.asList(Chars.concat(" %.,1234567890".toCharArray(), Chars.toArray(_currencyCodesToSymbols.values()))));

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

//    //todo: implement - maybe remove to util class
//    private String applyHeuristicsOnText(String filteredText)
//    {
//        return filteredText;
//    }

    //todo: maybe remove to util class
    private boolean foundPriceInText(String filteredText)
    {
        //todo: add support in ',' - if more then 2 from right - thousands, else double(.)
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
}
