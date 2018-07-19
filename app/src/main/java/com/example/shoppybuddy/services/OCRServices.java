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
    public HashMap<Character, String> _currencySymbolsToCodes;//todo - this might change to a bimap

    public void OCRServices()
    {
        init();
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

    public String getTextFromCapturedImage(Context appContext, Context context, Uri capturedImageUri)
    {
       String result = null;
        TextRecognizer textDetector = new TextRecognizer.Builder(appContext).build();

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
                            words = words + element.getValue() + ", ";
                        }
                    }
                }
                if (textBlocks.size() == 0) {
                    result = "Scan Failed: Found nothing to scan";
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
                    result = words;
                    System.out.println(words);
                }
            } else {
                result = "Could not set up the detector!";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Image");
        }

        return result;
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

    @NonNull
    private String getFilteredText(String rawRecognizedText)
    {
        Log.v(TAG, "OCRED TEXT: " + rawRecognizedText);

        rawRecognizedText = rawRecognizedText.trim();
        ArrayList<Character> whitelist = new ArrayList<>(Chars.asList(Chars.concat(" .,1234567890".toCharArray(), Chars.toArray(_currencySymbolsToCodes.keySet()))));
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

    private boolean parsePriceFromTextSucceeded(String rawRecognizedText)
    {
        String filteredText = getFilteredText(rawRecognizedText);
        if (!foundPriceInText(filteredText)) {
            filteredText = ApplyHeuristicsOnText(filteredText);
            if (!foundPriceInText(filteredText)) {
                //todo - send message to the user to try and take a picture again, and send him to the camera again
                return false;
            }
        }

        //_originalPrice = Double.parseDouble("526"); //filteredText
        return false;   //todo - if this is still 'false', change it
    }


    //todo: implement
    private String ApplyHeuristicsOnText(String filteredText)
    {
        return filteredText;
    }

    //todo: implement
    private boolean foundPriceInText(String filteredText)
    {
        return true;
    }
}
