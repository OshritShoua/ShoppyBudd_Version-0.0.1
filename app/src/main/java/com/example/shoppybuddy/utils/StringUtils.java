package com.example.shoppybuddy.utils;

import static com.example.shoppybuddy.utils.Constants.INVALID_INDEX;

public class StringUtils {

    public static int indexOfAny(String str, String searchChars) {
        if (isEmpty(str) || isEmpty(searchChars)){
            return INVALID_INDEX;
        }
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            for (int j = 0; j < searchChars.length(); j++) {
                if (searchChars.charAt(j) == ch) {
                    return i;
                }
            }
        }
        return INVALID_INDEX;
    }

    public static boolean isEmpty(String array) {
        if (array == null || array.length() == 0) {
            return true;
        }
        return false;
    }
}
