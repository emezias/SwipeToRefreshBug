/*
 * Copyright © 2010 - 2017 Modo Labs Inc. All rights reserved.
 *
 * The license governing the contents of this file is located in the LICENSE
 * file located at the root directory of this distribution. If the LICENSE file
 * is missing, please contact sales@modolabs.com.
 *
 */

package android.modolabs.com.webviewbugdemo;

import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * This webview client is set like what we use in the Kuroogo Native Android app
 * Tapping the buttons on the page opens a new fragment and a new webview, added to the backstack
 * Using the back button to return from the new fragment demonstrates the issue with touch highlighting inside the webview
 */
public class KgouiWebViewClient extends WebViewClient {
    public static final String TAG = KgouiWebView.class.getSimpleName();
    final FragmentWebviewActivity mActivity; //in real life, a weak reference

    public KgouiWebViewClient(FragmentWebviewActivity activity) {
        mActivity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        Log.d(TAG, "should override " + url);
        final FragmentManager mgr = mActivity.getSupportFragmentManager();
        mgr.beginTransaction().hide(mActivity.mWebFragment).add(R.id.content_frame, KgoWebViewFragment.newInstance(url), url).addToBackStack(url).commit();
        mgr.executePendingTransactions();
        return true;
        //default to true and override loading to build Fragment cache
    }

}
