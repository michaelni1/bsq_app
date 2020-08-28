
package com.bsq.bsquared;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize ads
        MobileAds.initialize(SplashActivity.this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        new AdManager(this);
        final InterstitialAd ad = AdManager.getAd();
        ad.setAdListener(
                new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        //try reloading
                        AdManager admanager = new AdManager(SplashActivity.this);
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }});
    }
}