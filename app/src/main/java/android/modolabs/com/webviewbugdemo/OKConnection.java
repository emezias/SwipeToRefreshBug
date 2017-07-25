package android.modolabs.com.webviewbugdemo;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Callback;
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
    public static final String REFERER = "Referer";
    public static final String ACCEPT = "Accept";
    public static final String C_CONTROL = "Cache-Control";
    public static final String AGE_0 = "max-age=0";
    public static final String ACCEPT_TYPE = "text/html,application/xhtml+xml,application/xml";
    public static final String USERAGENT = "User-Agent";
    public static final String KGO_VERSION = "X-Kurogo-Version";
    public static final String KGO_NAV_VHEADER = "X-Kurogo-Navigation-Version";
    public static final String KGO_NAV_VERSION = "3";
    public static final String KGO_URL_HEADER = "X-Kurogo-BaseURL";
    public static final String KGO_DEVICE_HEADER = "X-Kurogo-Device";
    public static final String COOKIE = "Cookie";

    static final Set<String> validRequestHeaders = new HashSet<>(Arrays.asList(
            new String[]{"Accept", "Accept-Charset", "Accept-Encoding", "Accept-Language", "Accept-Datetime", "Authorization", C_CONTROL,
                    "Connection", COOKIE, "Content-Length", "Content-MD5", "Content-Type", "Date", "Expect", "From", "Host", "If-Match",
                    "If-Modified-Since", "If-None-Match", "If-Range", "If-Unmodified-Since", "Max-Forwards", "Origin", "Pragma",
                    "Proxy-Authorization", "Range", "Referer", "TE", "User-Agent", "Upgrade", "Via", "Warning", "X-Requested-With", "DNT",
                    "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Proto", "Front-End-Https", "X-Http-Method-Override", "X-ATT-DeviceId",
                    "X-Wap-Profile", "Proxy-Connection", "X-UIDH", "X-Csrf-Token", KGO_VERSION, KGO_NAV_VHEADER}));

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

    public static Request addHeader(boolean forceNetwork, String url, String header, String value) {
        if (!validRequestHeaders.contains(header))
            return OKConnection.getRequest(forceNetwork, url).build();
        Request.Builder builder = OKConnection.getRequest(forceNetwork, url);
        builder = defHeaders(builder);
        builder.header(header, value);
        return builder.build();
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

    public static void makeAsyncRequest(Context ctx, boolean forceNetwork, String requestUrl, Callback callback) {
        final OkHttpClient client = getClient(ctx);
        Request.Builder builder = getRequestBuilder(forceNetwork);
        Request request = builder.url(requestUrl).build();
        client.newCall(request).enqueue(callback);
    }

    public static Request.Builder getRequest(boolean forceNetwork, String requestUrl) {
        Request.Builder builder = getRequestBuilder(forceNetwork);
        return builder.url(requestUrl);
        //client.newCall(request).enqueue(callback);
    }

}
