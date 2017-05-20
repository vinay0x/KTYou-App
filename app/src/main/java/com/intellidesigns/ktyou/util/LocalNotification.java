package com.intellidesigns.ktyou.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.intellidesigns.ktyou.MainActivity;
import com.intellidesigns.ktyou.R;

/**
 * Created by DRAGAN on 3/5/2017.
 */

public class LocalNotification {

    public static void createNotification(Context context,String displayName, String message) {

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(displayName)
                        .setContentText(message);

        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setContentIntent(resultPendingIntent);

        nm.notify(0, builder.build());
    }
}
