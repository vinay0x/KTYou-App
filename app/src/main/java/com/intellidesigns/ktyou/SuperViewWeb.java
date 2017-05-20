package com.intellidesigns.ktyou;

import android.app.Application;
import android.content.Intent;

import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;


public class SuperViewWeb extends Application implements OneSignal.NotificationOpenedHandler{

    private static boolean activityVisible;

    @Override
    public void onCreate() {
        super.onCreate();
        OneSignal
            .startInit(this)
            .setNotificationOpenedHandler(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .init();
    }

    @Override
    public void notificationOpened(OSNotificationOpenResult result) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (result.notification.payload.launchURL != null) {
            intent.putExtra(Intent.EXTRA_TEXT, result.notification.payload.launchURL);
            intent.setType("text/plain");
        }
        startActivity(intent);
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }


}
