
package com.firsatavcisi.ai;

import android.app.Activity;
import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.PermissionRequest;
import android.webkit.GeolocationPermissions;
import android.widget.ImageView;
import android.webkit.ValueCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int FILE_CHOOSER_REQUEST = 101;

    private WebView webView;
    private ImageView splash;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingWebRequest;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private boolean permissionDialogInProgress = false;

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
            settings.setGeolocationEnabled(true);

            webView.setWebViewClient(new WebViewClient());

            webView.setWebChromeClient(new WebChromeClient() {

                @Override
                public void onPermissionRequest(PermissionRequest request) {
                    runOnUiThread(() -> {
                        pendingWebRequest = request;
                        handleWebPermissionRequest();
                    });
                }

                @Override
                public void onGeolocationPermissionsShowPrompt(
                        String origin,
                        GeolocationPermissions.Callback callback) {

                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;

                    if (hasPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION)
                        || hasPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {

                        pendingGeoCallback.invoke(
                            pendingGeoOrigin, true, false
                        );
                        pendingGeoCallback = null;
                        pendingGeoOrigin = null;

                    } else {
                        requestNeededPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        );
                    }
                }

                @Override
                public boolean onShowFileChooser(
                        WebView view,
                        ValueCallback<Uri[]> callback,
                        FileChooserParams params) {

                    if (fileCallback != null) {
                        fileCallback.onReceiveValue(null);
                    }

                    fileCallback = callback;

                    Intent intent = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                    );
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    try {
                        startActivityForResult(
                            intent, FILE_CHOOSER_REQUEST
                        );
                    } catch (Exception e) {
                        fileCallback.onReceiveValue(null);
                        fileCallback = null;
                        return false;
                    }

                    return true;
                }
            });

            setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));

            izinleriIste();

            webView.loadUrl(
                "file:///android_asset/index.html"
            );

        }, 2000);
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < 23
            || checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void izinleriIste() {
        List<String> permissions = new ArrayList<>();

        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        requestNeededPermissions(
            permissions.toArray(new String[0])
        );
    }

    private void requestNeededPermissions(String... permissions) {
        if (Build.VERSION.SDK_INT < 23) {
            handleWebPermissionRequest();
            return;
        }

        List<String> missing = new ArrayList<>();

        for (String permission : permissions) {
            if (!hasPermission(permission)
                    && !missing.contains(permission)) {
                missing.add(permission);
            }
        }

        if (missing.isEmpty()) {
            handleWebPermissionRequest();
            handleGeoResult();
            return;
        }

        if (permissionDialogInProgress) {
            return;
        }

        permissionDialogInProgress = true;

        requestPermissions(
            missing.toArray(new String[0]),
            REQUEST_PERMISSIONS
        );
    }

    private void handleWebPermissionRequest() {
        if (pendingWebRequest == null) {
            return;
        }

        List<String> allowed = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String resource : pendingWebRequest.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                if (hasPermission(Manifest.permission.CAMERA)) {
                    allowed.add(resource);
                } else {
                    missing.add(Manifest.permission.CAMERA);
                }
            } else if (
                PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
            ) {
                if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    allowed.add(resource);
                } else {
                    missing.add(Manifest.permission.RECORD_AUDIO);
                }
            }
        }

        if (!missing.isEmpty()) {
            requestNeededPermissions(
                missing.toArray(new String[0])
            );
            return;
        }

        PermissionRequest request = pendingWebRequest;
        pendingWebRequest = null;

        if (allowed.isEmpty()) {
            request.deny();
        } else {
            request.grant(allowed.toArray(new String[0]));
        }
    }

    private void handleGeoResult() {
        if (pendingGeoCallback == null) {
            return;
        }

        boolean granted =
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        pendingGeoCallback.invoke(
            pendingGeoOrigin, granted, false
        );

        pendingGeoCallback = null;
        pendingGeoOrigin = null;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == REQUEST_PERMISSIONS) {
            permissionDialogInProgress = false;
            handleWebPermissionRequest();
            handleGeoResult();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST
                && fileCallback != null) {

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();

                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
