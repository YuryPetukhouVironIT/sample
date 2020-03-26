package com.cephx.def.service;

import java.util.HashMap;
import java.util.Map;

public class LanguageService {
    private static Map<String, String> languages;

    static {
        languages = new HashMap<>();
        languages.put("en", "English");
//        languages.put("ru", "Russian");
        languages.put("fr", "French");
        languages.put("zh", "Chinese");
        languages.put("ja", "Japanese");
        languages.put("de", "German");
        languages.put("pt", "Portugues");
        languages.put("es", "Spanish");
        languages.put("it", "Italian");
    }

    public static String getLanguageByCode(String languageCode) {
        return languages.get(languageCode);
    }

    public static boolean isValidLanguageCode(String languageCode) {
        return languages.containsKey(languageCode);
    }

    public static Map<String, String> getLanguages() {
        return languages;
    }
}
