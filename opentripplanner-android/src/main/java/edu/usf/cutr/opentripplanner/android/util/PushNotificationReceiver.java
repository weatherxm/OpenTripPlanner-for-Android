package edu.usf.cutr.opentripplanner.android.util;

import android.content.Context;
import android.content.Intent;

import com.parse.Parse;
import com.parse.ParsePushBroadcastReceiver;

import timber.log.Timber;

public class PushNotificationReceiver extends ParsePushBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String data = intent.getStringExtra("com.parse.Data");
        Timber.d("Received new push notification [%s]", data);

        LocationEvent event = LocationEvent.fromJson(data);
        AndroidBus.getInstance().post(event);

        // Override default push notification handling
        // super.onReceive(context, intent);
    }

}
