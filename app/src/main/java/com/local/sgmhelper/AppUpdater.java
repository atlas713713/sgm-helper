package com.local.sgmhelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AppUpdater {
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/atlas713713/sgm-helper/releases/latest";
    private static final String APK_MIME = "application/vnd.android.package-archive";

    private final Activity activity;
    private BroadcastReceiver downloadReceiver;
    private long downloadId = -1;
    private boolean checking;

    AppUpdater(Activity activity) {
        this.activity = activity;
    }

    void check() {
        if (checking) {
            return;
        }
        checking = true;
        Toast.makeText(activity, R.string.update_checking, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Release release = fetchLatestRelease();
                activity.runOnUiThread(() -> showResult(release));
            } catch (Exception error) {
                activity.runOnUiThread(() -> {
                    checking = false;
                    Toast.makeText(activity,
                            activity.getString(R.string.update_check_failed, error.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "release-check").start();
    }

    void close() {
        unregisterDownloadReceiver();
    }

    private Release fetchLatestRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL)
                .openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("GitHub HTTP " + status);
            }
            JSONObject json = new JSONObject(readFully(connection.getInputStream()));
            String tag = json.getString("tag_name");
            JSONArray assets = json.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getString("name");
                if (name.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    return new Release(tag, asset.getString("browser_download_url"));
                }
            }
            throw new IllegalStateException("Release 中没有 APK");
        } finally {
            connection.disconnect();
        }
    }

    private void showResult(Release release) {
        checking = false;
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!isNewerVersion(release.tag, BuildConfig.VERSION_NAME)) {
            Toast.makeText(activity,
                    activity.getString(R.string.update_current, BuildConfig.VERSION_NAME),
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.update_available,
                        release.tag, BuildConfig.VERSION_NAME))
                .setNegativeButton(R.string.update_cancel, null)
                .setPositiveButton(R.string.update_download,
                        (dialog, which) -> download(release))
                .show();
    }

    private void download(Release release) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(activity, R.string.update_permission, Toast.LENGTH_LONG).show();
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())));
            return;
        }

        DownloadManager manager = (DownloadManager) activity.getSystemService(
                Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show();
            return;
        }

        registerDownloadReceiver(manager);
        String fileName = "sgm-helper-" + release.tag.replaceAll("[^A-Za-z0-9._-]", "_")
                + "-" + System.currentTimeMillis() + ".apk";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(release.apkUrl))
                .setTitle("三国M助手 " + release.tag)
                .setMimeType(APK_MIME)
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                        activity, Environment.DIRECTORY_DOWNLOADS, fileName);
        downloadId = manager.enqueue(request);
        Toast.makeText(activity,
                activity.getString(R.string.update_downloading, release.tag),
                Toast.LENGTH_LONG).show();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerDownloadReceiver(DownloadManager manager) {
        unregisterDownloadReceiver();
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) {
                    return;
                }
                Uri apk = completedDownloadUri(manager);
                unregisterDownloadReceiver();
                if (apk == null) {
                    Toast.makeText(activity, R.string.update_download_failed,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                install(apk);
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(downloadReceiver, filter);
        }
    }

    private Uri completedDownloadUri(DownloadManager manager) {
        try (Cursor cursor = manager.query(
                new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    return manager.getUriForDownloadedFile(downloadId);
                }
            }
        }
        return null;
    }

    private void install(Uri apk) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setData(apk)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(activity, R.string.update_installer_missing, Toast.LENGTH_LONG).show();
        }
    }

    private void unregisterDownloadReceiver() {
        if (downloadReceiver == null) {
            return;
        }
        try {
            activity.unregisterReceiver(downloadReceiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered by the system.
        }
        downloadReceiver = null;
    }

    private static String readFully(InputStream input) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    static boolean isNewerVersion(String remote, String current) {
        List<Integer> remoteParts = versionParts(remote);
        List<Integer> currentParts = versionParts(current);
        int size = Math.max(remoteParts.size(), currentParts.size());
        for (int i = 0; i < size; i++) {
            int left = i < remoteParts.size() ? remoteParts.get(i) : 0;
            int right = i < currentParts.size() ? currentParts.get(i) : 0;
            if (left != right) {
                return left > right;
            }
        }
        return false;
    }

    private static List<Integer> versionParts(String version) {
        List<Integer> parts = new ArrayList<>();
        for (String part : version.replaceFirst("^[vV]", "").split("\\.")) {
            String digits = part.replaceFirst("^(\\d+).*$", "$1");
            if (!digits.matches("\\d+")) {
                break;
            }
            parts.add(Integer.parseInt(digits));
        }
        return parts;
    }

    private record Release(String tag, String apkUrl) {
    }
}
