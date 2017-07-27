package android.modolabs.com.webviewbugdemo;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by emezias on 4/6/17.
 * Setting up a single connection client to be used with all HTTP calls
 */


public class OKConnection {
    public static final String TAG = "OKConnection";

    //CONSTANTS
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_TYPE = "text/html,application/xhtml+xml,application/xml";
    public static final String KGO_NAV_VHEADER = "X-Kurogo-Navigation-Version";
    public static final String KGO_NAV_VERSION = "3";


    static OkHttpClient sClient;
    static CacheControl sNoCache;

    public static final OkHttpClient getClient(Context ctx) {
        if (sClient == null || sClient.cache().isClosed()) {
            int cacheSize = 16 * 1024 * 1024; // 16 MiB
            sClient = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .cache(new Cache(ctx.getExternalCacheDir(), cacheSize))
                    .connectTimeout(45, TimeUnit.SECONDS)
                    .build();
            sNoCache = new CacheControl.Builder().noCache().build();
        }
        return sClient;
    }

    static Request.Builder defHeaders(Request.Builder builder) {
        builder.header(ACCEPT, ACCEPT_TYPE)
                .header(KGO_NAV_VHEADER, KGO_NAV_VERSION);
        return builder;
    }

    static final Request.Builder getRequestBuilder(boolean forceNetwork) {
        Request.Builder builder;
        if (forceNetwork) {
            builder = new Request.Builder().cacheControl(new CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build());
            Log.d(TAG, "force network, no cache");
        } else {
            builder = new Request.Builder();
            Log.d(TAG, "use cache");
        }
        builder = defHeaders(builder);
        return builder;
    }

    //any Blocking*Request needs to be called from bg thread
    public static Response makeBlockingRequest(Context ctx, boolean forceNetwork, String requestUrl) {
        final OkHttpClient client = getClient(ctx);
        Request request = getRequestBuilder(forceNetwork)
                .url(requestUrl)
                //.tag(), set ID to choose error
                .build();
        try {
            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Request.Builder getRequest(boolean forceNetwork, String requestUrl) {
        Request.Builder builder = getRequestBuilder(forceNetwork);
        return builder.url(requestUrl);
        //client.newCall(request).enqueue(callback);
    }

}
