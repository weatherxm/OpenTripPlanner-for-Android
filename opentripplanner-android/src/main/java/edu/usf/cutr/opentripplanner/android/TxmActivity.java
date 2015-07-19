package edu.usf.cutr.opentripplanner.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import edu.usf.cutr.opentripplanner.android.util.WifiUtils;

/**
 * @author Stratos Theodorou
 * @version 1.0
 * @since 19/07/2015
 */
public class TxmActivity extends Activity {

    WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txm);

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(WifiUtils.TXM_IP_ADDRESS);
    }
}
