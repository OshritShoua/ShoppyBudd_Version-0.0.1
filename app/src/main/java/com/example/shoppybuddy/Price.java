package com.example.shoppybuddy;

public class Price
{
    private double originalAmount;
    private double convertedAmount;
    private Character currencySymbol;

    public Price(double originalAmount, Character currencySymbol)
    {
        this.originalAmount = originalAmount;
        this.currencySymbol = currencySymbol;
    }

    public double getOriginalAmount()
    {
        return originalAmount;
    }

    public Character getCurrencySymbol()
    {
        return currencySymbol;
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
        return originalAmount + Character.toString(currencySymbol);
    }
}
