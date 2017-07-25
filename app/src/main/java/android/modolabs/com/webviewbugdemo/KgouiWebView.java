/*
 * Copyright © 2010 - 2017 Modo Labs Inc. All rights reserved.
 *
 * The license governing the contents of this file is located in the LICENSE
 * file located at the root directory of this distribution. If the LICENSE file
 * is missing, please contact sales@modolabs.com.
 *
 */

package android.modolabs.com.webviewbugdemo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;

/**
 * This subclass of Android's WebView routes HTTP requests through HttpResponseCache.
 * <p>
 * This class exists because WebViewClient.shouldInterceptRequest() is only available
 * starting in API level 11.
 * Now it's legacy code build in to the app.
 */
public class KgouiWebView extends WebView {

    WebViewClient mWebViewClient;
    private boolean mHideAddressBarCalled = false;
    private WeakReference<SwipeRefreshLayout> mSwipeRefreshLayout;


    public KgouiWebView(Context context) {
        super(context);
        init();
    }

    public KgouiWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KgouiWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public KgouiWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Toggle web settings so we can standardize behavior across webviews.
     * Many of these functions are just setting values to the default, because we don't know if
     * the default value will change in future versions of Android.
     *
     * @param webview
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void enableDefaultWebSettings(WebView webview) {
        final WebSettings settings = webview.getSettings();

        /// settings where we are just enforcing the default
        // in case the default changes in future Android releases

        settings.setBlockNetworkImage(false); // default is false
        settings.setBlockNetworkLoads(false); // default is false (with INTERNET manifest permission)
        settings.setGeolocationEnabled(true); // default is true, callback implemented in DefaultWebChromeClient
        settings.setJavaScriptCanOpenWindowsAutomatically(false); // default is false
        settings.setLoadsImagesAutomatically(true); // default is true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(true); // default is true
        }

        settings.setNeedInitialFocus(true); // default is true
        settings.setSaveFormData(false); // default is true, bad UX with saved data scrolling under keyboard
        settings.setSupportMultipleWindows(false); // default is false
        settings.setSupportZoom(true); // default is true
        settings.setLoadWithOverviewMode(true); // default is false
        settings.setUseWideViewPort(true); // default unspecified in documentation, appears to be false
        settings.setAllowContentAccess(false); // default is true
        settings.setAllowFileAccess(false); // default is true
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        // HTML5 application cache API. default is false, set to true to match iOS default
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // default is false, but documentation recommends always setting to true
        settings.setBuiltInZoomControls(true);

        // HTML5 database API. default is false, set to true to match iOS default.
        settings.setDatabaseEnabled(true);
        settings.setDisplayZoomControls(false);
        // HTML5 DOM storage API. default is false, set to true to match iOS default
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true); // default is false

        // this is for playing certain videos
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            settings.setPluginState(WebSettings.PluginState.ON); // setPluginState deprecated in API 18
        }
        // In Lollipop, the default is to block mixed content.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
    }

    private void init() {
        setFocusable(true);
        setAccessibilityDelegate(new AccessibilityDelegate());
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(client);
        mWebViewClient = client;
    }

    @TargetApi(19)
    private void kitkatLoadJavascript(String javaScript) {
        evaluateJavascript(javaScript, null);
    }

    public void loadJavascript(String javaScript) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            kitkatLoadJavascript(javaScript);
        } else {
            loadUrl("javascript: " + javaScript);
        }
    }


    public void setParentSwipeRefreshLayout(SwipeRefreshLayout layout) {
        mSwipeRefreshLayout = new WeakReference<SwipeRefreshLayout>(layout);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (mSwipeRefreshLayout == null) return;
        final SwipeRefreshLayout pullLayout = mSwipeRefreshLayout.get();
        if (pullLayout == null) {
            return;
        }
        if (mSwipeRefreshLayout != null && clampedY) {
            pullLayout.setEnabled(true);
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    //TODO make sibling view into a weak reference or set this code into the fragment

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSwipeRefreshLayout == null) return super.onTouchEvent(event);
        final SwipeRefreshLayout pullLayout = mSwipeRefreshLayout.get();
        if (pullLayout == null) {
            return super.onTouchEvent(event);
        }
        int action = event.getActionMasked();
        if (mSwipeRefreshLayout != null && action == MotionEvent.ACTION_UP) {
            pullLayout.setEnabled(false);
        }
        return super.onTouchEvent(event);
    }
}
