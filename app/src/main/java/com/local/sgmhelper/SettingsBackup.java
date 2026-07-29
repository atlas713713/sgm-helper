package com.local.sgmhelper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 把设置备份到共享存储，卸载重装后自动读回来（app 私有目录会跟着卸载一起清掉）。
 *
 * <p>用 {@code getAll()} + JSON 而不是复制 shared_prefs 的 XML：进程里缓存着同一份
 * XML，直接盖文件会被随后的写入覆盖掉。按类型分桶存是因为 JSON 数字回来时分不清
 * int 和 long，混了以后 {@code getLong} 会 ClassCastException。
 */
final class SettingsBackup {
    private static final File FILE = new File(
            Environment.getExternalStorageDirectory(), "SgmHelper/settings.json");
    private static final String BOOLEANS = "bool";
    private static final String INTEGERS = "int";
    private static final String LONGS = "long";
    private static final String FLOATS = "float";
    private static final String STRINGS = "string";
    private static final String SETS = "set";

    private SettingsBackup() {
    }

    /** ponytail: 只做 Android 11+ 的“所有文件访问”，三台模拟器都是 13。 */
    static boolean canAccessStorage() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Environment.isExternalStorageManager();
    }

    static Intent permissionIntent(Context context) {
        return new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    static String location() {
        return FILE.getAbsolutePath();
    }

    static boolean export(Context context) {
        if (!canAccessStorage()) {
            return false;
        }
        JSONObject root = new JSONObject();
        try {
            for (String bucket : new String[] {
                    BOOLEANS, INTEGERS, LONGS, FLOATS, STRINGS, SETS}) {
                root.put(bucket, new JSONObject());
            }
            for (Map.Entry<String, ?> entry : preferences(context).getAll().entrySet()) {
                Object value = entry.getValue();
                String bucket = bucketOf(value);
                if (bucket == null) {
                    continue;
                }
                root.getJSONObject(bucket).put(entry.getKey(),
                        value instanceof Set ? new JSONArray((Set<?>) value) : value);
            }
            File parent = FILE.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                writer.write(root.toString());
            }
            return true;
        } catch (Exception error) {
            DiagnosticLog.error("BACKUP", "导出设置失败", error);
            return false;
        }
    }

    /** 只在本地一条设置都没有时导入，不覆盖当前配置。 */
    static void restoreIfEmpty(Context context) {
        SharedPreferences preferences = preferences(context);
        if (!preferences.getAll().isEmpty() || !canAccessStorage() || !FILE.isFile()) {
            return;
        }
        if (restore(context)) {
            DiagnosticLog.info("BACKUP", "已从 " + location() + " 恢复设置");
        }
    }

    static boolean restore(Context context) {
        if (!canAccessStorage() || !FILE.isFile()) {
            return false;
        }
        try {
            JSONObject root = new JSONObject(readFile());
            SharedPreferences.Editor editor = preferences(context).edit();
            copy(root, BOOLEANS, (key, json) -> editor.putBoolean(key, json.getBoolean(key)));
            copy(root, INTEGERS, (key, json) -> editor.putInt(key, json.getInt(key)));
            copy(root, LONGS, (key, json) -> editor.putLong(key, json.getLong(key)));
            copy(root, FLOATS,
                    (key, json) -> editor.putFloat(key, (float) json.getDouble(key)));
            copy(root, STRINGS, (key, json) -> editor.putString(key, json.getString(key)));
            copy(root, SETS,
                    (key, json) -> editor.putStringSet(key, toSet(json.getJSONArray(key))));
            editor.apply();
            return true;
        } catch (Exception error) {
            DiagnosticLog.error("BACKUP", "导入设置失败", error);
            return false;
        }
    }

    private static void copy(JSONObject root, String bucket, Entry entry) throws Exception {
        JSONObject values = root.optJSONObject(bucket);
        if (values == null) {
            return;
        }
        for (Iterator<String> keys = values.keys(); keys.hasNext(); ) {
            entry.put(keys.next(), values);
        }
    }

    private static String bucketOf(Object value) {
        if (value instanceof Boolean) {
            return BOOLEANS;
        }
        if (value instanceof Integer) {
            return INTEGERS;
        }
        if (value instanceof Long) {
            return LONGS;
        }
        if (value instanceof Float) {
            return FLOATS;
        }
        if (value instanceof String) {
            return STRINGS;
        }
        return value instanceof Set ? SETS : null;
    }

    private static Set<String> toSet(JSONArray array) throws Exception {
        Set<String> values = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }

    private static String readFile() throws Exception {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        return text.toString();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private interface Entry {
        void put(String key, JSONObject values) throws Exception;
    }
}
