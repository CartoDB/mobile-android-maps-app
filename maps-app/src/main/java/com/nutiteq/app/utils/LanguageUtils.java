package com.nutiteq.app.utils;

/**
 * Created by aareundo on 07/10/16.
 */

public class LanguageUtils {

    public static String getLanguage(String language) {

        if (language.equals(Const.LANG_AUTOMATIC)) {
            return Const.MAP_LANGUAGE_AUTOMATIC;
        }

        if (language.equals(Const.LANG_LOCAL)) {
            return Const.MAP_LANGUAGE_LOCAL;
        }

        if (language.equals(Const.LANG_ENGLISH)) {
            return Const.MAP_LANGUAGE_ENGLISH;
        }

        if (language.equals(Const.LANG_GERMAN)) {
            return Const.MAP_LANGUAGE_GERMAN;
        }

        if (language.equals(Const.LANG_FRENCH)) {
            return Const.MAP_LANGUAGE_FRENCH;
        }

        if (language.equals(Const.LANG_RUSSIAN)) {
            return Const.MAP_LANGUAGE_RUSSIAN;
        }

        if (language.equals(Const.LANG_CHINESE)) {
            return Const.MAP_LANGUAGE_CHINESE;
        }

        if (language.equals(Const.LANG_SPANISH)) {
            return Const.MAP_LANGUAGE_SPANISH;
        }

        if (language.equals(Const.LANG_ITALIAN)) {
            return Const.MAP_LANGUAGE_ITALIAN;
        }

        if (language.equals(Const.LANG_ESTONIAN)) {
            return Const.MAP_LANGUAGE_ESTONIAN;
        }

        return "";
    }
}
