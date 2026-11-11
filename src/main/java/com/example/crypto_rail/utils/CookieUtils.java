package com.example.crypto_rail.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

public class CookieUtils {

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }

        Cookie cookieFound =  Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieName))
                .findFirst()
                .orElse(null);

        if (cookieFound == null) {
            return "";
        }
        return cookieFound.getValue();
    }
}
