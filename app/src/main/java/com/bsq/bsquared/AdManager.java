package com.bsq.bsquared;

import android.app.Activity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class AdManager {
    static InterstitialAd ad;
    private Activity activity;

    public AdManager(Activity activity) {
        this.activity = activity;
        createAd();
    }

    public void createAd() {
        ad = new InterstitialAd(activity);
        ad.setAdUnitId("ca-app-pub-1980516351496812/5738181376");

        AdRequest adRequest = new AdRequest.Builder().build();
        //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
        //.addTestDevice(TEST_DEVICE_ID)

        ad.loadAd(adRequest);
    }

    static InterstitialAd getAd() {
        if (ad != null) {
            return ad;
        }
        else {
            return null;
        }
    }
}