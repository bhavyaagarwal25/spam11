package com.example.spam;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String CHANNEL_ID = "spam_channel_id";
    private static final Random random = new Random();

    // ✅ Update this with your actual backend IP
    private static final String BACKEND_URL = "http://192.168.32.17:5000/predict";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received");

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        String format = bundle.getString("format");
                        StringBuilder fullMessage = new StringBuilder();
                        String sender = null;

                        for (Object pdu : pdus) {
                            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                            sender = sms.getDisplayOriginatingAddress();
                            fullMessage.append(sms.getMessageBody());
                        }

                        String messageText = fullMessage.toString();
                        if (sender != null && !messageText.isEmpty()) {
                            sendSmsToBackend(context, sender, messageText);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing SMS", e);
                    showNotification(context, "Error", "Failed to process incoming SMS.");
                }
            }
        }
    }

    private void sendSmsToBackend(Context context, String sender, String message) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BACKEND_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("text", message);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }

                    JSONObject responseJson = new JSONObject(response.toString());
                    String prediction = responseJson.optString("prediction", "unknown");

                    String title = prediction.equalsIgnoreCase("spam")
                            ? "⚠ Spam Message Detected!"
                            : "✔ Message is Safe";
                    String body = "From: " + sender + "\n" + message;

                    showNotification(context, title, body);
                } else {
                    Log.e(TAG, "API call failed. HTTP code: " + code);
                    showNotification(context, "Error", "Failed to check spam status.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending to backend", e);
                showNotification(context, "Connection Error", "Unable to connect to spam detection service.");
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        createNotificationChannel(notificationManager);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                random.nextInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message.length() > 60 ? message.substring(0, 60) + "..." : message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{100, 200, 300, 400, 500})
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationManager.notify(random.nextInt(), builder.build());
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Spam Detection Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when spam messages are detected.");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Optional: Manual test
    public static void testNotification(Context context) {
        new SmsReceiver().showNotification(context, "Test", "This is a test notification from SpamShield.");
    }
}