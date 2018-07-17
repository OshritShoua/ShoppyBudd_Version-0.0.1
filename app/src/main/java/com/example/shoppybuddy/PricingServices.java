package com.example.shoppybuddy;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.primitives.Chars;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class PricingServices
{
    private static String _appDataPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/ShoppyBuddy/"; //// getExternalStorageDirectory()
    private static String _imageFilePath = _appDataPath + "captured_image.jpg";
    private static String TAG;
    private String _language = "eng";
    public HashMap<Character, String> _currencySymbolsToCodes;//todo - this might change to a bimap
    private double _originalPrice;
    private double _convertedPrice;
    private String _baseCurrencyCode = "USD";
    private String _targetCurrencyCode = "ILS";
    private double _euroToBaseCurrencyRate;
    private double _euroToTargetCurrencyRate;
    private Context _context;
    private boolean _parsingComplete;

    PricingServices(Context context)
    {
        init(context);
    }

    public static String GetImageFilePath(){return _imageFilePath;}
    public boolean IsPriceParsingComplete(){return _parsingComplete;}

    private void init(Context context)
    {
        _context = context;
        TAG = "ShoppyBuddy.java";
        _currencySymbolsToCodes = new HashMap<Character, String>();
        _currencySymbolsToCodes.put('€', "EUR");
        _currencySymbolsToCodes.put('₪', "ILS");
        _currencySymbolsToCodes.put('¥', "JPY");
        _currencySymbolsToCodes.put('£', "GBP");
        _currencySymbolsToCodes.put('$', "USD");

        deleteExistingFilesAndDirs();
        createAppDirsOnPublicStorage();
        copyTesseractTrainingFileToPublicStorage();
    }

    private void deleteExistingFilesAndDirs()
    {
        String[] paths = {_appDataPath + "tessdata/" + _language + ".traineddata", _appDataPath + "tessdata/", _appDataPath, _imageFilePath};
        for (String path : paths) {
            boolean b;
            File node = new File(path);
            if (node.exists()) {
                b = node.delete();
                Log.v(TAG, "deleted " + path);
            }
        }
    }

    //todo - This can be more readable if using the Files.copy() which requires a higher api level. Read about that error and see what it means.
    private void copyTesseractTrainingFileToPublicStorage()
    {
        if (!(new File(_appDataPath + "tessdata/" + _language + ".traineddata")).exists()) {
            try {
                AssetManager assetManager = _context.getAssets();
                InputStream in = assetManager.open("tessdata/" + _language + ".traineddata");
                OutputStream out = new FileOutputStream(new File(_appDataPath + "tessdata/" + _language + ".traineddata"));

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                Log.v(TAG, "Copied " + _language + " traineddata");
                File f = new File(_appDataPath + "tessdata/" + _language + ".traineddata");
                boolean b = f.exists();
                long size = f.length();
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + _language + " traineddata " + e.toString());
            }
        }
    }

    private void createAppDirsOnPublicStorage()
    {
        String[] paths = new String[]{_appDataPath, _appDataPath + "tessdata/"};
        String state = Environment.getExternalStorageState();
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!Environment.MEDIA_MOUNTED.equals(state)) {
                    Log.v(TAG, "ERROR: Creation of directories failed because external storage is not available for read/write");
                    return;
                }

                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " successfully");
                }
            }
        }
    }

    public interface RatesClientRequest
    {
        @GET("/latest")
        Call<ResponseBody> getRates(
                @Query("access_key") String apiKey,
                @Query("symbols") String requestedRates);
    }

    public String getConversionRatesFromApi() throws IOException
    {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Retrofit.Builder builder = new Retrofit.Builder().baseUrl("http://data.fixer.io/api/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        RatesClientRequest ratesProvider = retrofit.create(RatesClientRequest.class);
        Call<ResponseBody> call = ratesProvider.getRates("28b1f943a2bc43b31e27eda845458bb8",
                "USD,ILS,AUD,CAD,PLN,MXN");
        return call.execute().body().string();
    }


    public void parseRatesFromConversionApiResponse(String response) throws JSONException
    {
        JSONObject json = new JSONObject(response);

        if (!json.has("success") || json.getBoolean("success") != true || !json.has("rates"))
        {
            Log.v(TAG, "bad conversion url response");
            throw new JSONException("bad conversion url response");
        }

        double euroToBaseCurrencyRate = -1;
        double euroToTargetCurrencyRate = -1;
        JSONObject currencyCodesToRates = json.getJSONObject("rates");
        Iterator<String> keysIterator = currencyCodesToRates.keys();
        while(keysIterator.hasNext())
        {
            String currencyCode = (String)keysIterator.next();
            if(currencyCode.equals(_baseCurrencyCode))
                euroToBaseCurrencyRate = currencyCodesToRates.getDouble(currencyCode);
            if(currencyCode.equals(_targetCurrencyCode))
                euroToTargetCurrencyRate = currencyCodesToRates.getDouble(currencyCode);
        }

        if(euroToBaseCurrencyRate == -1 || euroToTargetCurrencyRate == -1)
        {
            Log.v(TAG, "response did not contain required rates");
            throw new JSONException("response did not contain required rates");
        }

        _euroToBaseCurrencyRate = euroToBaseCurrencyRate;
        _euroToTargetCurrencyRate = euroToTargetCurrencyRate;
    }

    public boolean TryGetConvertedPrice(final double[] priceHolder)
    {
          return true;
//        if (!parsePriceFromPhotoSucceeded())
//        try
//        {
//            String ratesResponse = getConversionRatesFromApi();
//            parseRatesFromConversionApiResponse(ratesResponse);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//        calculateConvertedPrice();
//        priceHolder[0] = _convertedPrice;
//        return true;
    }

    private void calculateConvertedPrice()
    {
        double priceInEuros = _originalPrice / _euroToBaseCurrencyRate;
        double priceInTargetCurrency = priceInEuros * _euroToTargetCurrencyRate;
        _convertedPrice = priceInTargetCurrency;
        _parsingComplete = true;
    }


    public boolean parsePriceFromPhotoSucceeded()
    {
        _parsingComplete = false;
        Bitmap bitmap = getAdjustedBitmapFromPhoto();
        String rawRecognizedText = getOCRedRawText(bitmap);
        return parsePriceFromTextSucceeded(rawRecognizedText);
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

        _originalPrice = Double.parseDouble("526"); //filteredText
        return false;   //todo - if this is still 'false', change it
    }

    private String ApplyHeuristicsOnText(String filteredText)
    {
        return filteredText;
    }

    private boolean foundPriceInText(String filteredText)
    {
        return true;
    }

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

    private String getOCRedRawText(Bitmap bitmap)
    {
        Log.v(TAG, "Before baseApi");
        //todo - add logic that improves the ocr in this func
        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(_appDataPath, _language);
        baseApi.setImage(bitmap);
        String rawRecognizedText = baseApi.getUTF8Text();
        baseApi.end();
        return rawRecognizedText;
    }

    private Bitmap getAdjustedBitmapFromPhoto()
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        Bitmap bitmap = BitmapFactory.decodeFile(_imageFilePath, options);

        try {
            ExifInterface exif = new ExifInterface(_imageFilePath);
            int orientationMode = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + orientationMode);

            int rotate = 0;

            switch (orientationMode) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        return bitmap;
    }

    public double GetConvertedPrice()
    {
        return _convertedPrice;
    }
}

