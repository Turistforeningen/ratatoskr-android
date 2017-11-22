package no.dnt.medlem;

import java.io.File;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.view.Window;
import android.os.Build;

public class MainActivity extends AppCompatActivity {
    private WebView myWebView;
    private ConstraintLayout splashContainer;
    private ConstraintLayout noInternetConnectionContainer;
    private Boolean appInitialized = false;
    private Boolean inForeground = true;
    private Boolean isLoading = false;
    public static final String webviewCacheSubdir = "no.turistforeningen.ratatoskr.webViewAppCache";

    public void hideSplashContainer() {
        splashContainer.setVisibility(View.GONE);
        noInternetConnectionContainer.setVisibility(View.GONE);
        myWebView.refreshDrawableState();
    }

    public void showNoInternetConnectionContainer() {
        noInternetConnectionContainer.setVisibility(View.VISIBLE);
    }

    public void loadRatatoskr(Bundle savedInstanceState) {
        if (savedInstanceState == null && !isLoading) {
            isLoading = true;
            Log.d("loadRatatoskr", "attempting to load Ratatoskr");
            myWebView.stopLoading();
            myWebView.loadUrl("https://medlem-native.dnt.no");
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d("onCreate", appInitialized.toString());
        super.onCreate(savedInstanceState);
        inForeground = true;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        splashContainer = findViewById(R.id.splash_container);
        noInternetConnectionContainer = findViewById(R.id.no_internet_connection);

        myWebView = (WebView)findViewById(R.id.webView);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Set cache size to 8 mb by default. should be more than enough
        webSettings.setAppCacheMaxSize(1024*1024*8);

        File cachePath = new File(getCacheDir(), webviewCacheSubdir);
        webSettings.setAppCachePath(cachePath.getAbsolutePath());
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        webSettings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webSettings.setDatabasePath("/data/data/" + myWebView.getContext().getPackageName() + "/databases/");
        }

        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);

        myWebView.addJavascriptInterface(new WebAppInterface(this), "AndroidApp");


        myWebView.setWebViewClient(new WebViewClient() {

            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.d("onReceivedError", error.toString());
                Log.d("onReceivedError", appInitialized.toString());
                isLoading = false;

                if (!appInitialized) {
                    showNoInternetConnectionContainer();

                    if (inForeground) {
                        new android.os.Handler().postDelayed(
                                new Runnable() {
                                    public void run() {
                                        loadRatatoskr(savedInstanceState);
                                    }
                                },
                                1000);
                    }
                }
            }

            //Show loader on url load
            public void onLoadResource (WebView view, String url) {
                // TODO: Add timeout and loading-in-progress after 3000ms...?
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        loadRatatoskr(savedInstanceState);
    }

    @Override
    // Detect when the back button is pressed
    public void onBackPressed() {
        if(myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            // Let the system handle the back button
            super.onBackPressed();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("onStart", appInitialized.toString());
        inForeground = true;

        if (!appInitialized) {
            loadRatatoskr(null);
        } else {
            hideSplashContainer();
            myWebView.refreshDrawableState();
        }
    }

    @Override
    protected void onPause() {
        Log.d("onPause", appInitialized.toString());
        super.onPause();
        inForeground = false;
    }

    @Override
    protected void onResume() {
        Log.d("onResume", appInitialized.toString());
        super.onResume();
        inForeground = true;

        if (!appInitialized) {
            loadRatatoskr(null);
        } else {
            hideSplashContainer();
            myWebView.refreshDrawableState();
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void onInstalled() {
            Log.d("webcache", "onInstalled");
        }

        @JavascriptInterface
        public void onUpdating() {
            Log.d("webcache", "onUpdating");
        }

        @JavascriptInterface
        public void onUpdateReady() {
            Log.d("webcache", "onUpdateReady");
        }

        @JavascriptInterface
        public void onUpdated() {
            Log.d("webcache", "onUpdated");
        }

        @JavascriptInterface
        public void onAppMounted() {
            Log.d("javascript-app", "onAppMounted");
            appInitialized = true;
            isLoading = false;

            runOnUiThread(new Runnable(){
                public void run(){
                    hideSplashContainer();
                }
            });
            myWebView.refreshDrawableState();
        }

    }
}
