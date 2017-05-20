package com.intellidesigns.ktyou;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;


public class AdMob {
    private static final String TAG = "ADMOB";
    private InterstitialAd mInterstitialAd;
    private Context mContext;
    private AdView adView;
    private Handler mHandler;
    private Runnable mHandlerTask;
    private static int INTERVAL = 1000 * 60 * 2;

    public AdMob(Context mContext, AdView adView) {
        this.mContext = mContext;
        this.adView = adView;
        String interval = mContext.getString(R.string.interval_time);
        if (!TextUtils.isEmpty(interval)) {
            try {
                INTERVAL = Integer.parseInt(interval);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        initInterstitialAd();
        initBannerAds();
    }

    public void initInterstitialAd() {
        String interstitialId = mContext.getString(R.string.interstitial_ad_unit_id);
        if (!TextUtils.isEmpty(interstitialId)) {
            mInterstitialAd = new InterstitialAd(mContext);
            mInterstitialAd.setAdUnitId(interstitialId);
//            mInterstitialAd.loadAd(getAdRequest());
            mInterstitialAd.setAdListener(new AdListener() {
                public void onAdLoaded() {
                    if (mInterstitialAd != null) {
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        }
                    }
                }

                public void onAdClosed() {
                    mHandler.postDelayed(mHandlerTask, INTERVAL);
                }
            });
        }
    }

    public void initBannerAds() {
        MobileAds.initialize(mContext, mContext.getString(R.string.ad_app_id));
        if (adView != null) {
            String bannerId = mContext.getString(R.string.banner_ad_unit_id);
            if (!TextUtils.isEmpty(bannerId)) {
                adView.setVisibility(View.VISIBLE);
//                adView.loadAd(getAdRequest());
            } else {
                adView.setVisibility(View.GONE);
            }
        }
    }

    public void requestInterstitialAd() {
        if (mInterstitialAd != null) {
            String interstitialId = mContext.getString(R.string.interstitial_ad_unit_id);
            if (!TextUtils.isEmpty(interstitialId)) {
                if (SuperViewWeb.isActivityVisible()) {
                    mInterstitialAd.loadAd(getAdRequest());
                } else {
                    if (mHandler != null) {
                        mHandler.postDelayed(mHandlerTask, INTERVAL);
                    }
                }
            }
        }
    }

    public void requestBannerAds() {
        if (adView != null) {
            String bannerId = mContext.getString(R.string.banner_ad_unit_id);
            if (!TextUtils.isEmpty(bannerId)) {
                adView.loadAd(getAdRequest());
            }
        }
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder()
                .addTestDevice(mContext.getString(R.string.test_device_id))
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
    }

    public void requestAdMob() {
        mHandler = new Handler();
        requestBannerAds();
        mHandlerTask = new Runnable() {
            @Override
            public void run() {
                requestInterstitialAd();
                requestBannerAds();
            }
        };
        mHandler.postDelayed(mHandlerTask, INTERVAL);
//        mHandlerTask.run();
    }

    public void stopRepeatingTask() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mHandlerTask);
        }
    }


}