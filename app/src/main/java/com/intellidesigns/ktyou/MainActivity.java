package com.intellidesigns.ktyou;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.intellidesigns.ktyou.util.IabBroadcastReceiver;
import com.intellidesigns.ktyou.util.IabHelper;
import com.intellidesigns.ktyou.util.IabResult;
import com.intellidesigns.ktyou.util.Inventory;
import com.intellidesigns.ktyou.util.NetworkHandler;
import com.intellidesigns.ktyou.util.PermissionUtil;
import com.intellidesigns.ktyou.util.Pref;
import com.intellidesigns.ktyou.util.ProgressDialogHelper;
import com.intellidesigns.ktyou.util.Purchase;
import com.intellidesigns.ktyou.util.UrlHander;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DownloadListener, IabBroadcastReceiver.IabBroadcastListener {

    /* URL saved to be loaded after fb login */
    private static String target_url, target_url_prefix;

    private Context mContext;
    private WebView mWebview, mWebviewPop;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private static final int REQUEST_SELECT_FILE = 2;

    private FrameLayout mContainer;
    private ImageView mImageViewSplash;
    private ImageView mBack;
    private ImageView mForward;
    private ImageView mBilling;
    private boolean show_content = true, showToolBar = true;

    private AdView mAdView;
    private String urlData, currentUrl, contentDisposition, mimeType;
    private AdMob admob;

    //PAYMENT
    IabHelper mHelper;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    private String ITEM_SKU = "";
    private boolean isPurchased = false;

    //DATA FOR GEOLOCAION REQUEST
    String geoLocationOrigin;
    GeolocationPermissions.Callback geoLocationCallback;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        checkURL(getIntent());
        initPayment();
        initComponents();
        initBrowser(savedInstanceState);


        if (savedInstanceState != null) {
            showContent();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showContent();
                }
            }, 5000);
        }
    }

    private void checkURL(Intent intent) {
        if (intent != null) {
            if ("text/plain".equals(intent.getType()) && !TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                target_url = intent.getStringExtra(Intent.EXTRA_TEXT);
                target_url_prefix = Uri.parse(target_url).getHost();
                currentUrl = target_url;
                return;
            }
        }

        target_url = getString(R.string.target_url);

        if (TextUtils.isEmpty(target_url)) {
            target_url = "file:///android_asset/index.html";
            target_url_prefix = "android_asset";
        } else {
            target_url_prefix = Uri.parse(target_url).getHost();
        }

        currentUrl = target_url;

        if (mWebview != null) {
            if (mWebviewPop != null) {
                mWebviewPop.setVisibility(View.GONE);
                mContainer.removeView(mWebviewPop);
                mWebviewPop = null;
            }
            mWebview.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SuperViewWeb.activityResumed();
        hideStatusBar();
        checkURL(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        SuperViewWeb.activityPaused();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebview.saveState(outState);
    }

    private void removeAds() {
        mAdView.setVisibility(View.GONE);
        if (admob != null) {
            admob.stopRepeatingTask();
        }
        mBilling.setVisibility(View.GONE);
    }

    private void initPayment() {
        mBilling = (ImageView)findViewById(R.id.billing);
        isPurchased = Pref.getValue(this, ITEM_SKU, false);

        ITEM_SKU = getString(R.string.item_sku);
        String base64EncodedPublicKey = getString(R.string.public_key);
        if (!TextUtils.isEmpty(ITEM_SKU) && !TextUtils.isEmpty(base64EncodedPublicKey)) {
            mHelper = new IabHelper(this, base64EncodedPublicKey);
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isFailure()) {
                        Log.v("Purches", "isFailure");
                    } else {
                        mBroadcastReceiver = new IabBroadcastReceiver(MainActivity.this);
                        IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                        registerReceiver(mBroadcastReceiver, broadcastFilter);
                        if (mHelper != null) {
                            mHelper.queryInventoryAsync(mGotInventoryListener);
                        }
                    }
                }
            });
        } else {
            mBilling.setVisibility(View.GONE);
        }
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                return;
            }

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(ITEM_SKU);
            isPurchased = (premiumPurchase != null);
            Pref.setValue(MainActivity.this, ITEM_SKU, isPurchased);
            if (isPurchased) {
                removeAds();
            }
        }
    };

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isSuccess()) {
                Pref.setValue(MainActivity.this, ITEM_SKU, true);
                removeAds();
            }
        }
    };

    private void initComponents() {
        mContext = this.getApplicationContext();
        mImageViewSplash = (ImageView) findViewById(R.id.image_splash);
        mAdView = (AdView) findViewById(R.id.adView);
        if (isPurchased) {
            mAdView.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(getString(R.string.toolbar))) {
            showToolBar = false;
        }

        if (showToolBar) {
            mBack = (ImageView) findViewById(R.id.back);
            mForward = (ImageView) findViewById(R.id.forward);
            ImageView mRefresh = (ImageView) findViewById(R.id.refresh);

            mBack.setOnClickListener(this);
            mForward.setOnClickListener(this);
            mRefresh.setOnClickListener(this);
            //if app isn't buy
            if (!isPurchased) {
                mBilling.setOnClickListener(this);
            } else {
                mBilling.setVisibility(View.GONE);
            }
        } else {
            LinearLayout llToolbarContainer = (LinearLayout) findViewById(R.id.toolbar_footer);
            if (llToolbarContainer != null) {
                llToolbarContainer.setVisibility(View.GONE);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mAdView.getLayoutParams();
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            }
        }
    }

    private void hideStatusBar() {
        if (!TextUtils.isEmpty(getString(R.string.hide_status_bar))) {
            if (Build.VERSION.SDK_INT < 16) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                View decorView = getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
                ActionBar actionBar = getActionBar();
                if (actionBar != null) {
                    actionBar.hide();
                }
            }
        }
    }

    public void showContent() {
        if (show_content) {
            PermissionUtil.checkPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CALL_PHONE,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.ACCESS_NETWORK_STATE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.INTERNET
            });

            show_content = false;
            if (!isPurchased) {
                admob = new AdMob(this, mAdView);
                admob.requestAdMob();
            }
            mImageViewSplash.setVisibility(View.GONE);
            mContainer.setVisibility(View.VISIBLE);
            ProgressDialogHelper.dismissProgress();
        }
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    private void initBrowser(Bundle savedInstanceState) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        mWebview = (WebView) findViewById(R.id.webview);
        mContainer = (FrameLayout) findViewById(R.id.webview_frame);
        WebSettings webSettings = mWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setGeolocationDatabasePath(getFilesDir().getPath());

        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        int a = WebSettings.TextSize.SMALLER.ordinal();
        mWebview.setWebViewClient(new UriWebViewClient());
        mWebview.setWebChromeClient(new UriChromeClient());
        mWebview.setDownloadListener(this);
        mWebview.addJavascriptInterface(new WebAppInterface(this, ITEM_SKU, mWebview), "android");

        if (Build.VERSION.SDK_INT >= 19) {
            mWebview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if(Build.VERSION.SDK_INT >=15 && Build.VERSION.SDK_INT < 19) {
            mWebview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        if (!TextUtils.isEmpty(getString(R.string.zoom))) {
            mWebview.getSettings().setSupportZoom(true);
            mWebview.getSettings().setBuiltInZoomControls(true);
            mWebview.getSettings().setDisplayZoomControls(false);
        }
        if (savedInstanceState != null) {
            mWebview.restoreState(savedInstanceState);
        } else {
            mWebview.loadUrl(target_url);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                if (mWebview.canGoBack()) {
                    mWebview.goBack();
                }
                break;
            case R.id.forward:
                if (mWebview.canGoForward()) {
                    mWebview.goForward();
                }
                break;
            case R.id.refresh:
                mWebview.loadUrl(target_url);
                if (!show_content) {
                    ProgressDialogHelper.showProgress(MainActivity.this);
                }
                break;
            case R.id.billing:
                if (mHelper != null) {
                    mHelper.launchPurchaseFlow(this, ITEM_SKU, 10001, mPurchaseFinishedListener, "");
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE || requestCode == REQUEST_SELECT_FILE ) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (requestCode == REQUEST_SELECT_FILE) {
                    if (uploadMessage == null)
                        return;

                    Uri uri[] = null;
                    if (data != null) {
                        if (data.getClipData() != null) {
                            uri = new Uri[data.getClipData().getItemCount()];
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                uri[i] = data.getClipData().getItemAt(i).getUri();
                            }
                        }  else {
                            uri = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                        }
                    }

                    uploadMessage.onReceiveValue(uri);
                    uploadMessage = null;
                }
            } else if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                if (null == mUploadMessage) return;
                // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
                // Use RESULT_OK only if you're implementing WebView inside an Activity
                Uri result = data == null || resultCode != MainActivity.RESULT_OK ? null : data.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            } else {
                Toast.makeText(MainActivity.this.getApplicationContext(), R.string.failed_to_upload_image, Toast.LENGTH_LONG).show();
            }
        } else {
            if (mHelper != null) {
                if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
                    super.onActivityResult(requestCode, resultCode, data);
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void receivedBroadcast() {
        try {
            if (mHelper != null) {
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebview != null) {
            mWebview.destroy();
        }
        if (mWebviewPop != null) {
            mWebviewPop.destroy();
        }
        if (admob != null) {
            admob.stopRepeatingTask();
        }
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if (mHelper != null) {
            try {
                mHelper.dispose();
            }catch (Exception ex) {
                ex.printStackTrace();
            }
            mHelper = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebview.canGoBack()) {
            mWebview.goBack();
        } else {
            super.onBackPressed();
        }
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Checking the request code of our request
        if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_CALL) {
            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                UrlHander.call(MainActivity.this, urlData);
            }
        } else if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                UrlHander.sms(MainActivity.this, urlData);
            }
        } else if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_DOWNLOAD) {
            UrlHander.download(MainActivity.this, urlData, contentDisposition, mimeType);
        } else if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_GEOLOCATION) {
            if (geoLocationCallback != null) {
                geoLocationCallback.invoke(geoLocationOrigin, true, false);
            }
        }
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long l) {
        this.contentDisposition = contentDisposition;
        this.mimeType = mimeType;
        UrlHander.downladLink(this, url, contentDisposition, mimeType);
    }

    private void setToolbarButtonColor() {
        if (showToolBar) {
            if (mWebview.canGoBack()) {
                mBack.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
            } else {
                mBack.setColorFilter(ContextCompat.getColor(this, R.color.gray));
            }
            if (mWebview.canGoForward()) {
                mForward.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
            } else {
                mForward.setColorFilter(ContextCompat.getColor(this, R.color.gray));
            }
        }
    }


    private class UriWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            urlData = url;
            if (target_url_prefix.equals(host)) {
                if (mWebviewPop != null) {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop = null;
                }
                return false;
            }

            boolean result = UrlHander.checkUrl(MainActivity.this, url);
            if (result) {
                ProgressDialogHelper.dismissProgress();
            } else {
                currentUrl = url;
                if (!show_content) {
                    ProgressDialogHelper.showProgress(MainActivity.this);
                }
            }
            return result;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (!NetworkHandler.isNetworkAvailable(view.getContext())) {
                view.loadUrl("file:///android_asset/NoInternet.html");
            }
            hideStatusBar();
            ProgressDialogHelper.dismissProgress();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (!NetworkHandler.isNetworkAvailable(view.getContext())) {
                view.loadUrl("file:///android_asset/NoInternet.html");
            }
            hideStatusBar();
            ProgressDialogHelper.dismissProgress();
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (!NetworkHandler.isNetworkAvailable(view.getContext())) {
                view.loadUrl("file:///android_asset/NoInternet.html");
            }
            hideStatusBar();
            ProgressDialogHelper.dismissProgress();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            showContent();
            setToolbarButtonColor();
            hideStatusBar();
            ProgressDialogHelper.dismissProgress();
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);
            setToolbarButtonColor();
            hideStatusBar();
            ProgressDialogHelper.dismissProgress();
        }
    }

    class UriChromeClient extends WebChromeClient {

        @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            mWebviewPop = new WebView(mContext);
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebViewClient(new UriWebViewClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setSavePassword(false);
            mWebviewPop.getSettings().setAppCacheEnabled(true);
            mWebviewPop.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            mWebviewPop.getSettings().setSupportMultipleWindows(true);
            mWebviewPop.getSettings().setGeolocationEnabled(true);
            mWebviewPop.getSettings().setDomStorageEnabled(true);
            mWebviewPop.getSettings().setDatabaseEnabled(true);
            mWebviewPop.getSettings().setGeolocationEnabled(true);
            mWebviewPop.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());
            mWebviewPop.addJavascriptInterface(new WebAppInterface(MainActivity.this, ITEM_SKU, mWebviewPop), "android");
            mWebviewPop.getSettings().setLoadWithOverviewMode(true);
            mWebviewPop.getSettings().setAllowFileAccess(true);
            mWebviewPop.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebviewPop);
            mWebviewPop.setDownloadListener(MainActivity.this);

            if (Build.VERSION.SDK_INT >= 19) {
                mWebviewPop.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else if(Build.VERSION.SDK_INT >=15 && Build.VERSION.SDK_INT < 19) {
                mWebviewPop.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.v("TEST", "onCloseWindow");
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            MainActivity.this.geoLocationOrigin = origin;
            MainActivity.this.geoLocationCallback = callback;
            PermissionUtil.geoLocationPermission(MainActivity.this, origin, callback);
        }

        // openFileChooser for Android 3.0+
        protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;
            List<Intent> cameraIntents = getCameraIntents();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("image/*");
            try {
                Intent chooserIntent = Intent.createChooser(intent, getString(R.string.file_browser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
                startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(MainActivity.this.getApplicationContext(),
                        R.string.cannot_open_file_chooser,
                        Toast.LENGTH_LONG).show();
            }
        }

        // For Lollipop 5.0+ Devices
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams)
        {
            if (mUploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }

            uploadMessage = filePathCallback;

            List<Intent> cameraIntents = getCameraIntents();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            try {
                Intent chooserIntent = Intent.createChooser(intent, getString(R.string.file_browser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
                startActivityForResult(chooserIntent, REQUEST_SELECT_FILE);
            } catch (ActivityNotFoundException e) {
                uploadMessage = null;
                Toast.makeText(MainActivity.this.getApplicationContext(),
                        R.string.cannot_open_file_chooser,
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        // openFileChooser for Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            mUploadMessage = uploadMsg;
            List<Intent> cameraIntents = getCameraIntents();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("image/*");
            try {
                Intent chooserIntent = Intent.createChooser(intent, getString(R.string.file_browser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
                startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(MainActivity.this.getApplicationContext(),
                        R.string.cannot_open_file_chooser,
                        Toast.LENGTH_LONG).show();
            }
        }

        //For Android 4.1 only
        protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
        {
            mUploadMessage = uploadMsg;
            List<Intent> cameraIntents = getCameraIntents();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("image/*");
            try {
                Intent chooserIntent = Intent.createChooser(intent, getString(R.string.file_browser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
                startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(MainActivity.this.getApplicationContext(),
                        R.string.cannot_open_file_chooser,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public List<Intent> getCameraIntents() {
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            i.setPackage(packageName);
            cameraIntents.add(i);
        }
        return cameraIntents;
    }
}