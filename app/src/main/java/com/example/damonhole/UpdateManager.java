package com.example.damonhole;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/alanheng1106/DamonHole/6f428d7a5dd49e1d0bba266fa59adfe8ff8451be/update.json";

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkForUpdates(boolean manual) {
        if (manual) {
            Toast.makeText(context, R.string.checking_for_updates, Toast.LENGTH_SHORT).show();
        }

        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_JSON_URL).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    int latestVersionCode = json.getInt("versionCode");
                    String latestVersionName = json.getString("versionName");
                    String updateUrl = json.getString("updateUrl");

                    int currentVersionCode = getCurrentVersionCode();

                    if (latestVersionCode > currentVersionCode) {
                        mainHandler.post(() -> showUpdateDialog(latestVersionName, updateUrl));
                    } else if (manual) {
                        mainHandler.post(
                                () -> Toast.makeText(context, R.string.app_up_to_date, Toast.LENGTH_SHORT).show());
                    }
                } else if (manual) {
                    mainHandler.post(
                            () -> Toast.makeText(context, R.string.update_check_failed, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Update check failed", e);
                if (manual) {
                    mainHandler.post(
                            () -> Toast.makeText(context, R.string.update_check_failed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) pInfo.getLongVersionCode();
            } else {
                return pInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get package info", e);
            return 0;
        }
    }

    private void showUpdateDialog(String versionName, String updateUrl) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.update_available_title)
                .setMessage(context.getString(R.string.update_available_msg, versionName))
                .setPositiveButton(R.string.update_now, (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                    context.startActivity(browserIntent);
                })
                .setNegativeButton(R.string.later, null)
                .show();
    }
}
