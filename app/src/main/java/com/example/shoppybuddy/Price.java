package com.example.shoppybuddy;

import com.example.shoppybuddy.services.OCRServices;

public class Price
{
    private double originalAmount;
    private double convertedAmount;
    private Character fromCurrencySymbol;

    public Price(double originalAmount, Character fromCurrencySymbol)
    {
        this.originalAmount = originalAmount;
        this.fromCurrencySymbol = fromCurrencySymbol;
    }

    public double getOriginalAmount()
    {
        return originalAmount;
    }

    public Character getFromCurrencySymbol()
    {
        return fromCurrencySymbol;
    }
    public void setFromCurrencySymbol(Character fromCurrencySymbol)
    {
        this.fromCurrencySymbol = fromCurrencySymbol;
    }

    public String get_fromCurrencyCode()
    {
        return OCRServices.getSymbolsToCodesMapping().get(fromCurrencySymbol);
    }


    public void setConvertedAmount(double convertedAmount)
    {
        this.convertedAmount = convertedAmount;
    }

    public double getConvertedAmount()
    {
        return convertedAmount;
    }

    @Override
    public String toString()
    {
        String originalAmount = String.format("%.2f", this.originalAmount);
        return originalAmount + Character.toString(fromCurrencySymbol);
    }
}
