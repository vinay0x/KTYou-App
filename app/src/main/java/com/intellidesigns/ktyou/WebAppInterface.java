package com.intellidesigns.ktyou;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.intellidesigns.ktyou.util.LocalNotification;
import com.intellidesigns.ktyou.util.Pref;
import com.intellidesigns.ktyou.util.ProgressDialogHelper;
import com.google.firebase.iid.FirebaseInstanceId;

import hotchemi.android.rate.AppRate;



public class WebAppInterface {
    private Context mContext;
    private String productId;
    private WebView mWebview;
    /** Instantiate the interface and set the context */
    WebAppInterface(Context mContext, String productId, WebView mWebview) {
        this.mContext = mContext;
        this.productId = productId;
        this.mWebview = mWebview;
    }

    @JavascriptInterface
    public String getFirebaseToken() {
        return  FirebaseInstanceId.getInstance().getToken();
    }

    @JavascriptInterface
    public boolean isProductPurchased() {
        return Pref.getValue(mContext, productId, false);
    }


    @JavascriptInterface
    public void createNotification(String displayName, String message) {
        LocalNotification.createNotification(mContext, displayName, message);
    }

    @JavascriptInterface
    public void showLoader() {
        ProgressDialogHelper.showProgress(mContext);
    }

    @JavascriptInterface
    public void hideLoader() {
        ProgressDialogHelper.dismissProgress();
    }

    @JavascriptInterface
    public void fontSizeNormal() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.NORMAL);
            }
        });
    }

    @JavascriptInterface
    public void fontSizeLarger() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.LARGER);
            }
        });
    }

    @JavascriptInterface
    public void fontSizeLargest() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.LARGEST);
            }
        });
    }

    @JavascriptInterface
    public void fontSizeSmaller() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.SMALLER);
            }
        });

    }

    @JavascriptInterface
    public void fontSizeSmallest() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.SMALLEST);
            }
        });
    }

    @JavascriptInterface
    public void rateApp() {
        AppRate.with(mContext).showRateDialog((MainActivity)mContext);
    }

}

