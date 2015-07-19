package edu.usf.cutr.opentripplanner.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

/**
 * @author Stratos Theodorou
 * @version 1.0
 * @since 19/07/2015
 */
public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("Received connection event [%s]", intent.getAction());
        boolean isConnected = WifiUtils.isTxmConnected(context);
        AndroidBus.getInstance().post(new TxmConnectionEvent(isConnected));
    }

}
