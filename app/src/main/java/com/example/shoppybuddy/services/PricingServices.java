package com.example.shoppybuddy.services;

import android.os.StrictMode;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
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
    private String _baseCurrencyCode = "USD";
    private String _targetCurrencyCode = "ILS";
    private double _euroToBaseCurrencyRate;
    private double _euroToTargetCurrencyRate;
    private boolean _parsingComplete;

    public double GetConvertedPrice() {
        return _convertedPrice;
    }

    public String get_baseCurrencyCode() {
        return _baseCurrencyCode;
    }

    public String get_targetCurrencyCode() {
        return _targetCurrencyCode;
    }


    private double _convertedPrice;

    public boolean IsPriceParsingComplete(){return _parsingComplete;}

    public interface RatesClientRequest
    {
        @GET("/latest")
        Call<ResponseBody> getRates(
                @Query("access_key") String apiKey,
                @Query("symbols") String requestedRates);
    }

    public boolean ConvertPrice(double priceToConvert)
    {
        try
        {
            String ratesResponse = getConversionRatesFromApi();
            parseRatesFromConversionApiResponse(ratesResponse);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

        calculateConvertedPrice(priceToConvert);
        return true;
    }

    private String getConversionRatesFromApi() throws IOException
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


    private void parseRatesFromConversionApiResponse(String response) throws JSONException
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

    private void calculateConvertedPrice(double priceToCalc)
    {
        double priceInEuros = priceToCalc / _euroToBaseCurrencyRate;
        double priceInTargetCurrency = priceInEuros * _euroToTargetCurrencyRate;
        _convertedPrice = Double.parseDouble(new DecimalFormat("##.##")

                .format(priceInTargetCurrency));
        _parsingComplete = true;
    }
}

