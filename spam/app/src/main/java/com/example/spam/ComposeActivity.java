package com.example.spam;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/* Very simple compose screen so system sees SENDTO/sms: intent handled */
public class ComposeActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_compose);   // create a simple XML with two EditTexts & button

        EditText to   = findViewById(R.id.toNumber);
        EditText body = findViewById(R.id.messageBody);
        Button   send = findViewById(R.id.sendBtn);

        send.setOnClickListener(v -> {
            String num = to.getText().toString().trim();
            String msg = body.getText().toString().trim();
            if (num.isEmpty() || msg.isEmpty()) {
                Toast.makeText(this, "Number or message empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            SmsManager.getDefault().sendTextMessage(num, null, msg, null, null);
            Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
            finish();
        });

        /* If launched via smsto: scheme, pre-fill number */
        Intent i = getIntent();
        Uri data = i.getData();
        if (data != null) to.setText(data.getSchemeSpecificPart());
    }
}
