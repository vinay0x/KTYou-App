package com.intellidesigns.ktyou.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Pref {

    private static final String LOG_TAG = Pref.class.getSimpleName();
    private static String fileNameForShare = "ShareData";
    private static SharedPreferences mSharedPreferences = null;
    private static void openShare(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(fileNameForShare, Context.MODE_PRIVATE);
    }

    private static void closeShare() {
        mSharedPreferences = null;
    }

    public static void setValue(Context mContext, String key, String value) {
        openShare(mContext);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString(key, value);
        mEditor.commit();
        mEditor = null;
        closeShare();
    }

    public static void setValue(Context mContext, String key, Boolean value) {
        openShare(mContext);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putBoolean(key, value);
        mEditor.commit();
        mEditor = null;
        closeShare();
    }

    public static String getValue(Context mContext, String key, String value) {
        openShare(mContext);
        String result = mSharedPreferences.getString(key, value);
        closeShare();
        return result;
    }

    public static boolean getValue(Context mContext, String key, Boolean value) {
        openShare(mContext);
        boolean result = mSharedPreferences.getBoolean(key, value);
        closeShare();
        return result;
    }
}
