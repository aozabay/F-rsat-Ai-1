
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
    private File pendingUpdateFile;

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
                getAssets().open("logo.png")
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
            checkForUpdates();

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
        private void checkForUpdates() {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                    "https://api.github.com/repos/aozabay/F-rsat-Ai-1/releases/latest"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty(
                    "Accept", "application/vnd.github+json"
                );
                connection.setRequestProperty(
                    "User-Agent", "Firsat-Avcisi-AI"
                );

                int responseCode = connection.getResponseCode();

                if (responseCode != 200) {
                    return;
                }

                java.util.Scanner scanner =
                    new java.util.Scanner(connection.getInputStream())
                        .useDelimiter("\\A");

                String response = scanner.hasNext()
                    ? scanner.next() : "";
                scanner.close();

                JSONObject release = new JSONObject(response);
                String latestVersion =
                    release.optString("tag_name", "").replaceFirst("^v", "");

                String currentVersion = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;

                if (compareVersions(latestVersion, currentVersion) <= 0) {
                    return;
                }

                JSONArray assets = release.optJSONArray("assets");
                String downloadUrl = "";

                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");

                        if (name.toLowerCase().endsWith(".apk")) {
                            downloadUrl = asset.optString(
                                "browser_download_url", ""
                            );
                            break;
                        }
                    }
                }

                if (downloadUrl.isEmpty()) {
                    return;
                }

                final String apkUrl = downloadUrl;

                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Yeni sürüm mevcut")
                    .setMessage(
                        "Fırsat Avcısı AI " + latestVersion
                        + " sürümü yayımlandı. Güncellemek ister misin?"
                    )
                    .setNegativeButton("Daha sonra", null)
                    .setPositiveButton("İndir", (dialog, which) ->
                        downloadUpdate(apkUrl)
                    )
                    .show());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                    this,
                    "Güncelleme hatası: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private int compareVersions(String first, String second) {
        try {
            String[] a = first.split("\\.");
            String[] b = second.split("\\.");
            int length = Math.max(a.length, b.length);

            for (int i = 0; i < length; i++) {
                int x = i < a.length ? Integer.parseInt(a[i]) : 0;
                int y = i < b.length ? Integer.parseInt(b[i]) : 0;

                if (x != y) {
                    return Integer.compare(x, y);
                }
            }
        } catch (Exception e) {
            return first.compareTo(second);
        }

        return 0;
    }

    private void downloadUpdate(String apkUrl) {
        Toast.makeText(
            this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                File folder = new File(getCacheDir(), "updates");
                if (!folder.exists() && !folder.mkdirs()) {
                    throw new Exception("Klasör oluşturulamadı");
                }

                File apkFile = new File(folder, "app-update.apk");

                connection = (HttpURLConnection)
                    new URL(apkUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);

                if (connection.getResponseCode() != 200) {
                    throw new Exception("İndirme başarısız");
                }

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output =
                         new FileOutputStream(apkFile)) {

                    byte[] buffer = new byte[8192];
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }

                runOnUiThread(() -> {
                    pendingUpdateFile = apkFile;
                    installDownloadedUpdate();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                    this,
                    "İndirme başarısız oldu. Tekrar deneyebilirsin.",
                    Toast.LENGTH_LONG
                ).show());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void installDownloadedUpdate() {
        if (pendingUpdateFile == null || !pendingUpdateFile.exists()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 26
                && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                .setTitle("Kurulum izni gerekli")
                .setMessage(
                    "Güncellemeyi kurabilmek için bu uygulamaya "
                    + "bilinmeyen uygulama yükleme izni vermelisin."
                )
                .setNegativeButton("İptal", null)
                .setPositiveButton("Ayarları aç", (dialog, which) -> {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                })
                .show();
            return;
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                pendingUpdateFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(
                apkUri, "application/vnd.android.package-archive"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                this,
                "Kurulum ekranı açılamadı.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (pendingUpdateFile != null
                && Build.VERSION.SDK_INT >= 26
                && getPackageManager().canRequestPackageInstalls()) {
            installDownloadedUpdate();
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
