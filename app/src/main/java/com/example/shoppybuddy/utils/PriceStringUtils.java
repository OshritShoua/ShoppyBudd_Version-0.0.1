package com.example.shoppybuddy.utils;

public class PriceStringUtils {

    public static boolean foundPriceInText(String filteredText)
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

    public static boolean foundDoublePriceInText(String filteredText)
    {
        return foundPriceInText(filteredText) && filteredText.contains(".") && Double.parseDouble(filteredText) != 0;
    }

    public static int numberOfDigitsRightToComma(String text)
    {
        int digitsCounter = 0;
        int i = text.indexOf(",") + 1;

        for(; i < text.length(); i++)
        {
            if(Character.isDigit(text.charAt(i))) //Sanity check
            {
                digitsCounter++;
            }
        }

        return digitsCounter;
    }
}
