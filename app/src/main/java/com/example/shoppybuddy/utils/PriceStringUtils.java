package com.example.shoppybuddy.utils;

import static com.example.shoppybuddy.utils.Constants.MAX_DOUBLE_SIZE;
import static com.example.shoppybuddy.utils.Constants.MAX_NUMBER_SIZE;

public class PriceStringUtils {

    public static boolean foundPriceInText(String filteredText)
    {
        boolean priceWasFound = false;
        boolean isNormalPrice = false;

        try {
            Double.parseDouble(filteredText);
            priceWasFound = true;
        } catch (NumberFormatException e)
        {
            //If we got here, price wasn't found in filtered text
        }

        isNormalPrice = filteredText.contains(".") ? filteredText.length() <= MAX_DOUBLE_SIZE : filteredText.length() <= MAX_NUMBER_SIZE;

        return priceWasFound && isNormalPrice && filteredText.charAt(0) != '0';
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

    public static int numberOfDigitsRightToPoint(String text)
    {
        int digitsCounter = 0;
        int i = text.indexOf(".") + 1;

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
