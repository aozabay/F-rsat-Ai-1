package com.firsatavcisi.ai;

import android.app.Activity;
import androidx.appcompat.app.CompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.view.View;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 100;
    private WebView webView;
    private ImageView splash;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(11, 16, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 16, 32));

        splash = new ImageView(this);
        splash.setBackgroundColor(Color.rgb(11, 16, 32));
        splash.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(
                getAssets().open("acilis.png")
            );
            splash.setImageBitmap(bitmap);
        } catch (Exception e) {
            splash.setVisibility(View.GONE);
        }

        setContentView(splash);

        handler.postDelayed(() -> {
            webView = new WebView(this);
            webView.setBackgroundColor(Color.rgb(11, 16, 32));

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);

            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.loadUrl("file:///android_asset/index.html");

            setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }, 2000);
    }
    private void izinleriIste() {
    String[] izinler = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    requestPermissions(izinler, REQUEST_PERMISSIONS);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
