package com.example.shoppybuddy.services;

import android.os.StrictMode;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class PricingServices
{
    private static final String TAG = "ShoppyBuddy.java";
    private double _originalPrice = 5.26;
    private double _convertedPrice;
    private String _baseCurrencyCode = "USD";
    private String _targetCurrencyCode = "ILS";
    private double _euroToBaseCurrencyRate;
    private double _euroToTargetCurrencyRate;
    private boolean _parsingComplete;

    public boolean IsPriceParsingComplete(){return _parsingComplete;}

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
          //todo: implement
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

    public double GetConvertedPrice()
    {
        return _convertedPrice;
    }
}

