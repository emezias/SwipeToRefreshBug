/*
 * Copyright © 2010 - 2017 Modo Labs Inc. All rights reserved.
 *
 * The license governing the contents of this file is located in the LICENSE
 * file located at the root directory of this distribution. If the LICENSE file
 * is missing, please contact sales@modolabs.com.
 *
 */

package android.modolabs.com.webviewbugdemo;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Response;


/**
 * Superclass for Browser and KGOWebView fragment, both show a web view
 * Streamlining a little boilerplate to make the fragment logic easier to find and follow
 */
public class KgoWebViewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = KgouiWebView.class.getSimpleName();
    //Constants
    public static final String JS_EVENT_PREFIX = "var evt = document.createEvent('Event');\nevt.initEvent('";
    public static final String JS_EVENT_SUFFIX = "', false, false);\nwindow.dispatchEvent(evt);";
    public static final String JS_HIDE_EVENT = "pagehide";
    public static final String JSCRIPT = "javascript: ";
    public WebView mWebView;
    public SwipeRefreshLayout mPullToRefreshLayout;

    public KgoWebViewFragment() {
        // Required empty public constructor, needed when Fragment class is set in xml layouts
    }

    public static KgoWebViewFragment newInstance(String urlToLoad) {
        final KgoWebViewFragment fragment = new KgoWebViewFragment();
        final Bundle args = new Bundle();
        args.putString(TAG, urlToLoad);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View v = inflater.inflate(R.layout.fragment_kgo, container, false);
        mWebView = (KgouiWebView) v.findViewById(R.id.boringWebView);
        mPullToRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.pullToRefreshContainer);
        mPullToRefreshLayout.setOnRefreshListener(this);
        ((KgouiWebView) mWebView).setParentSwipeRefreshLayout(mPullToRefreshLayout);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Inflate the layout for this fragment, most of the layout is gone, to be set visible based on args
        KgouiWebView.enableDefaultWebSettings(mWebView);
        final KgouiWebViewClient mWebViewClient = new KgouiWebViewClient((FragmentWebviewActivity) getActivity());
        //moved the request to onResume also so that clients are all set into the webview
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setWebViewClient(mWebViewClient);
        final String url = getArguments().getString(TAG);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "Error reading url", Toast.LENGTH_SHORT).show();
        } else new LoadPage().execute(url);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            //this is to make sure the app will show any banner in the registrar
            mWebView.scrollTo(0, 0);
            mWebView.onResume();
            mWebView.resumeTimers();
        } else {
            mWebView.onPause();
            mWebView.pauseTimers();
        }
    } //was hiding loader on hidden, leads to homescreen showing artifacts

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebView != null) {
            mWebView.onResume();
            mWebView.resumeTimers();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWebView != null) {
            mWebView.onPause();
            mWebView.pauseTimers();
            forceWindowEvent(JS_HIDE_EVENT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    public void forceWindowEvent(String eventName) {
        if (!isVisible() || mWebView == null) return;
        loadJavascript(JS_EVENT_PREFIX + eventName + JS_EVENT_SUFFIX);
    }

    public void loadJavascript(String javaScript) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            kitkatLoadJavascript(javaScript);
        } else {
            mWebView.loadUrl(JSCRIPT + javaScript);
        }
    }

    @TargetApi(19)
    private void kitkatLoadJavascript(String javaScript) {
        mWebView.evaluateJavascript(javaScript, null);
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "calling onRefresh");
        //URL Module title was getting dropped
        mWebView.setTag(((FragmentWebviewActivity) getActivity()).getSupportActionBar().getTitle());
        // Use the current page in case JS has updated the page parameters in any way
        if (!TextUtils.isEmpty(mWebView.getUrl()) && URLUtil.isValidUrl(mWebView.getUrl()) && !URLUtil.isDataUrl(mWebView.getUrl())) {
            new LoadPage().execute(mWebView.getUrl());
        } else {
            new LoadPage().execute(getArguments().getString(TAG));
        }
    }

    /**
     * This class runs the API call in the background
     * The connection listener methods finish the activity
     */
    private class LoadPage extends AsyncTask<String, Void, Response> {

        Snackbar loaderView;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //webview client will hide the loader after page is loaded
            if (!mPullToRefreshLayout.isRefreshing()) {
                loaderView = Snackbar.make(getView(), "loading page", Snackbar.LENGTH_INDEFINITE);
                loaderView.show();
            }
        }

        protected Response doInBackground(String... params) {
            //3 different places call connect on mUrl.getExternals String, set true so they don't double
            final String initialUrl = params[0];
            if (!TextUtils.isEmpty(initialUrl)) {
                Log.d(TAG, "background connect call " + initialUrl);
                //making blocking call, waiting for response
                final Response response = OKConnection.makeBlockingRequest(getContext(), true, initialUrl);
                return response;
            } else {
                Log.e(TAG, "problem calling connect:" + initialUrl);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            if (isCancelled() || getActivity() == null || response == null) {
                Log.e(TAG, "post execute cancels internal login");
                return;
            }
            try {
                final String data = response.body().string();
                final String[] params = splitContentTypeAndEncoding(response.body().contentType().toString());
                final String urlString = response.request().url().toString();
                mWebView.loadDataWithBaseURL(urlString, data, params[0], params[1], urlString);
                ((FragmentWebviewActivity) getActivity()).mWebFragment = KgoWebViewFragment.this;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!mPullToRefreshLayout.isRefreshing()) {
                loaderView.dismiss();
            } else {
                mPullToRefreshLayout.setRefreshing(false);
            }
        }

        String[] splitContentTypeAndEncoding(String contentType) {
            String[] parts = contentType.split(";");
            String[] result = new String[2];
            if (parts.length == 2) {
                result[0] = parts[0].trim();
                String[] encodingParts = parts[1].split("=");
                if (encodingParts.length == 2) {
                    result[1] = encodingParts[1].trim();
                }
            } else if (parts.length == 1) {
                result[0] = contentType;
            }
            return result;
        }
    }

}
