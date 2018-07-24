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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

public class OCRServices {

    private static final String TAG = "ShoppyBuddy.java";
    private HashMap<Character, String> _currencySymbolsToCodes;//todo - this might change to a bimap
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
        _currencySymbolsToCodes = new HashMap<Character, String>();
        _currencySymbolsToCodes.put('€', "EUR");
        _currencySymbolsToCodes.put('₪', "ILS");
        _currencySymbolsToCodes.put('¥', "JPY");
        _currencySymbolsToCodes.put('£', "GBP");
        _currencySymbolsToCodes.put('$', "USD");
    }

    public void GetTextFromCapturedImage(Context appContext, Context context, Uri capturedImageUri)
    {
        TextRecognizer textDetector = new TextRecognizer.Builder(appContext).build();
        _currentTextCaptured = "";

        try {
            Bitmap bitmap = decodeBitmapUri(context, capturedImageUri);
            //todo: arrange
            if (textDetector.isOperational() && bitmap != null) {
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
                if (textBlocks.size() == 0) {
                    _currentTextCaptured = "Scan Failed: Found nothing to scan";
                } else {
//                        scanResults.setText(scanResults.getText() + "Blocks: " + "\n");
//                        scanResults.setText(scanResults.getText() + blocks + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                        scanResults.setText(scanResults.getText() + "Lines: " + "\n");
//                        scanResults.setText(scanResults.getText() + lines + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                        scanResults.setText(scanResults.getText() + "Words: " + "\n");
//                        scanResults.setText(scanResults.getText() + words + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
                    _currentTextCaptured = words;
                    System.out.println(words);
                }
            } else {
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

    public boolean parsePriceFromTextSucceeded()
    {
        String filteredText = getFilteredText(_currentTextCaptured);
        String text = null;
        //todo: implement
        String[] results = filteredText.split("[X ]", -1);
        ArrayList<String> pricesInResults = new ArrayList<>();
        for(String res : results)
        {
            if(res.isEmpty())
            {
                continue;
            }

            String[] isContainCurrencySymbol = res.split(_currencySymbolsToCodes.keySet().toString(), -1);

            if(isContainCurrencySymbol.length > 1)
            {
                res = res.replaceAll(_currencySymbolsToCodes.keySet().toString(), " ");
                if(foundPriceInText(res))
                {
                    text = res;
                    break;
                }
            }
            else if(foundPriceInText(res))
            {
                pricesInResults.add(res);
            }
        }

        if(pricesInResults.size() == 1)
        {
            text = pricesInResults.get(0);
        }
        else if(pricesInResults.size() > 0)
        {
            int indexInPrice;

            if((indexInPrice = isThereOnlyOneDoubleInPrices(pricesInResults)) != -1)
            {
                text = pricesInResults.get(indexInPrice);
            }
        }
        else
        {
            if(text == null)
            {
                System.out.println("couldn't find text");
            }
        }

        _currentPriceCaptured = filteredText =  "99.99";
        if (!foundPriceInText(filteredText)) {
            filteredText = applyHeuristicsOnText(filteredText);
            if (!foundPriceInText(filteredText)) {
                //todo - send message to the user to try and take a picture again, and send him to the camera again
                return false;
            }
        }

        return true;   //todo - if this is still 'false', change it
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
        ArrayList<Character> whitelist = new ArrayList<>(Chars.asList(Chars.concat(" .1234567890".toCharArray(), Chars.toArray(_currencySymbolsToCodes.keySet()))));
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

    //todo: implement - maybe remove to util class
    private String applyHeuristicsOnText(String filteredText)
    {
        return filteredText;
    }

    //todo: implement - maybe remove to util class
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
}
