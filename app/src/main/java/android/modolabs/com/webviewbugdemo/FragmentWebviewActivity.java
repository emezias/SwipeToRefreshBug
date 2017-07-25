package android.modolabs.com.webviewbugdemo;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class FragmentWebviewActivity extends AppCompatActivity {
    KgoWebViewFragment mWebFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_webview);
        mWebFragment = KgoWebViewFragment.newInstance("https://lxs-qa-test.modolabs.net/default/kitchensink/webkit_tap_color_test");
        final FragmentManager mgr = getSupportFragmentManager();
        mgr.beginTransaction().replace(R.id.content_frame, mWebFragment, "").commit();
        Log.i(KgoWebViewFragment.TAG, "set home fragment");
        mgr.executePendingTransactions();
    }
}
