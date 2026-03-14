package com.example.RakshaSOS;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PhoneActivity extends AppCompatActivity {

    EditText phoneInput, otpInput;
    Button sendOtpBtn, verifyBtn;

    String phoneNumber;
    int generatedOTP;

    private static final int PERMISSION_CODE = 101;

    String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        boolean registered = prefs.getBoolean("isRegistered", false);

        if (registered) {
            startActivity(new Intent(this, MainActivity2.class));
            finish();
        }

        setContentView(R.layout.activity_phone2);

        phoneInput = findViewById(R.id.phoneInput);
        otpInput = findViewById(R.id.otpInput);
        sendOtpBtn = findViewById(R.id.sendOtpBtn);
        verifyBtn = findViewById(R.id.verifyBtn);

        sendOtpBtn.setEnabled(false);

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            sendOtpBtn.setEnabled(true);
        }

        sendOtpBtn.setOnClickListener(v -> sendOTP());
        verifyBtn.setOnClickListener(v -> verifyOTP());
    }

    private boolean checkPermissions() {

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {

        ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_CODE
        );
    }

    private void sendOTP() {

        String number = phoneInput.getText().toString().trim();

        if (number.isEmpty()) {
            Toast.makeText(this,"Enter phone number",Toast.LENGTH_SHORT).show();
            return;
        }

        phoneNumber = "+91" + number;

        generatedOTP = (int)(Math.random()*900000)+100000;

        String message = "Your RakshaSOS OTP is: " + generatedOTP;

        try {

            SmsManager smsManager;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = this.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            smsManager.sendTextMessage(phoneNumber,null,message,null,null);

            Toast.makeText(this,"OTP Sent",Toast.LENGTH_SHORT).show();

        } catch (Exception e) {

            Toast.makeText(this,"SMS Failed: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void verifyOTP() {

        String enteredOTP = otpInput.getText().toString();

        if (enteredOTP.equals(String.valueOf(generatedOTP))) {

            Toast.makeText(this,"OTP Verified",Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();

        } else {

            Toast.makeText(this,"Invalid OTP",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {

            boolean granted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                }
            }

            if (granted) {

                sendOtpBtn.setEnabled(true);

            } else {

                Toast.makeText(this,
                        "Permissions required",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}