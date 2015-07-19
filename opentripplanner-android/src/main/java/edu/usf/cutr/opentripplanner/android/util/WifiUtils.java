package edu.usf.cutr.opentripplanner.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * @author Stratos Theodorou
 * @version 1.0
 * @since 19/07/2015
 */
public class WifiUtils {

    public static final String TXM_WIFI_SSID = "Transport Ex Machina";
    public static final String TXM_IP_ADDRESS = "http://192.168.77.1";

    public static boolean isTxmConnected(Context context) {
        String ssid = getSSID(context);
        return isConnectedViaWifi(context) && ssid != null && ssid.contains(TXM_WIFI_SSID);
    }

    public static String getSSID(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        return info != null ? info.getSSID() : null;
    }

    public static boolean isConnectedViaWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnectedOrConnecting();
    }
}
