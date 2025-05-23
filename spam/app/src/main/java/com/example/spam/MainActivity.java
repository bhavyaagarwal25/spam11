package com.example.spam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    /* ---------------- constants ---------------- */
    private static final String KEY_TEXT       = "text";
    private static final String KEY_PREDICTION = "prediction";
    private static final String API_URL        = "http://192.168.34.17:5000/predict";

    /* runtime-permissions */
    private static final String[] CORE_PERMS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS   // Android 13+ notifications
    };

    /* ---------------- members ---------------- */
    private ActivityResultLauncher<String[]> permissionLauncher;
    private OkHttpClient client = new OkHttpClient();

    /* ---------------- life-cycle ---------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* ask for permissions first run */
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                res -> checkDefaultSmsApp()               // after user answers → check default
        );
        requestMissingPerms();                           // may open permission sheet

        /* test button */
        Button testBtn = findViewById(R.id.testNotificationButton);
        testBtn.setOnClickListener(v ->
                sendToApi("Congratulations! You have won a free prize."));

        /* also re-check default every time activity resumes */
        checkDefaultSmsApp();
    }

    /* -------- default SMS prompt -------- */
    private void checkDefaultSmsApp() {
        String current = Telephony.Sms.getDefaultSmsPackage(this);
        if (!getPackageName().equals(current)) {
            new AlertDialog.Builder(this)
                    .setTitle("Set Spam Shield as default")
                    .setMessage("Spam Shield must be the default SMS app to scan every message.")
                    .setPositiveButton("Set now", (d, w) -> {
                        Intent i = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                        i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                        startActivity(i);
                    })
                    .setNegativeButton("Not now", null)
                    .show();
        }
    }

    /* -------- permission helpers -------- */
    private void requestMissingPerms() {
        List<String> missing = new ArrayList<>();
        for (String p : CORE_PERMS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            permissionLauncher.launch(missing.toArray(new String[0]));
        }
    }

    /* -------- call Flask backend -------- */
    private void sendToApi(@NonNull String msg) {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_TEXT, msg);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            Request req = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "API error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException {
                    String jsonResp = r.body() != null ? r.body().string() : "{}";
                    runOnUiThread(() -> {
                        try {
                            String pred = new JSONObject(jsonResp)
                                    .optString(KEY_PREDICTION, "unknown");
                            Toast.makeText(MainActivity.this,
                                    "Prediction = " + pred, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this,
                                    "Response error", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
