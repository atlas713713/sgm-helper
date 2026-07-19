package com.local.sgmhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class DiagnosticLog {
    private static final String TAG = "SgmHelper";
    private static final File SHARED_LOG_DIRECTORY =
            new File("/mnt/macos/BstSharedFolder", "SGMHelperLogs");

    private static File sharedLogFile;
    private static File fallbackLogFile;

    private DiagnosticLog() {
    }

    static synchronized void initialize(Context context) {
        if (sharedLogFile != null) {
            return;
        }
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String fileName = logFileName(androidId);
        sharedLogFile = new File(SHARED_LOG_DIRECTORY, fileName);
        fallbackLogFile = new File(context.getFilesDir(), fileName);
        info("APP", "started version=" + BuildConfig.VERSION_NAME
                + " device=" + Build.MANUFACTURER + "/" + Build.MODEL
                + " androidId=" + androidId);
    }

    static String logFileName(String androidId) {
        String safeId = androidId == null
                ? "unknown"
                : androidId.replaceAll("[^A-Za-z0-9._-]", "_");
        return "sgmhelper-" + simulatorName(androidId) + "-" + safeId + ".log";
    }

    private static String simulatorName(String androidId) {
        if ("077e5a706d186981".equals(androidId)) {
            return "地球瘦子";
        }
        if ("d30b62f2263b6d49".equals(androidId)) {
            return "栗威";
        }
        if ("60377771f3d25b63".equals(androidId)) {
            return "地球";
        }
        return "未知模拟器";
    }

    static void info(String area, String message) {
        Log.i(TAG, area + ": " + message);
        append("INFO", area, message, null);
    }

    static void warn(String area, String message) {
        Log.w(TAG, area + ": " + message);
        append("WARN", area, message, null);
    }

    static void error(String area, String message) {
        Log.e(TAG, area + ": " + message);
        append("ERROR", area, message, null);
    }

    static void error(String area, String message, Throwable error) {
        Log.e(TAG, area + ": " + message, error);
        append("ERROR", area, message, error);
    }

    static synchronized void saveScreenshot(Bitmap bitmap, String label) {
        if (bitmap == null || sharedLogFile == null) {
            return;
        }
        String timestamp = new SimpleDateFormat(
                "yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date());
        String safeLabel = label.replaceAll("[^A-Za-z0-9._-]", "_");
        String logName = sharedLogFile.getName().replaceFirst("\\.log$", "");
        File sharedFile = new File(
                SHARED_LOG_DIRECTORY, logName + "-" + safeLabel + "-" + timestamp + ".png");
        File fallbackFile = new File(fallbackLogFile.getParentFile(), sharedFile.getName());
        if (!writePng(sharedFile, bitmap)) {
            writePng(fallbackFile, bitmap);
        }
    }

    private static synchronized void append(
            String level, String area, String message, Throwable error) {
        if (sharedLogFile == null) {
            return;
        }
        String timestamp = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        StringBuilder entry = new StringBuilder()
                .append(timestamp).append(' ')
                .append(level).append(' ')
                .append('[').append(area).append("] ")
                .append(message).append('\n');
        if (error != null) {
            StringWriter stackTrace = new StringWriter();
            error.printStackTrace(new PrintWriter(stackTrace));
            entry.append(stackTrace);
        }
        if (!write(sharedLogFile, entry.toString())) {
            write(fallbackLogFile, entry.toString());
        }
    }

    private static boolean write(File file, String entry) {
        if (file == null) {
            return false;
        }
        File parent = file.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            return false;
        }
        parent.setReadable(true, false);
        parent.setWritable(true, false);
        parent.setExecutable(true, false);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            writer.write(entry);
            file.setReadable(true, false);
            file.setWritable(true, false);
            return true;
        } catch (IOException | SecurityException error) {
            Log.e(TAG, "Unable to write diagnostic log: " + file, error);
            return false;
        }
    }

    private static boolean writePng(File file, Bitmap bitmap) {
        File parent = file.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            return false;
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            boolean saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            if (saved) {
                file.setReadable(true, false);
                file.setWritable(true, false);
                info("SCREENSHOT", "saved " + file.getAbsolutePath());
            }
            return saved;
        } catch (IOException | SecurityException error) {
            Log.e(TAG, "Unable to save diagnostic screenshot: " + file, error);
            return false;
        }
    }
}
