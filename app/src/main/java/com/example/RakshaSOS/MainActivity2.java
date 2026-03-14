package com.example.RakshaSOS;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity implements SensorEventListener {

    FusedLocationProviderClient fusedLocationClient;
    Button sosBtn, policeBtn, hospitalBtn, flashBtn, resetBtn;

    double latitude, longitude;
    String userPincode;
    SQLiteDatabase db;
    String fam1, fam2, fam3;

    // Flashlight Variables
    CameraManager cameraManager;
    String cameraId;
    boolean isFlashing = false;
    boolean flashState = false;
    Handler flashHandler = new Handler();

    // Gyroscope Variables
    SensorManager sensorManager;
    Sensor gyroSensor;
    private static final float GYRO_THRESHOLD = 3.0f; // Sensitivity: Higher = harder movement needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // 1. Initialize Views
        sosBtn = findViewById(R.id.sosBtn);
        policeBtn = findViewById(R.id.policeBtn);
        hospitalBtn = findViewById(R.id.hospitalBtn);
        flashBtn = findViewById(R.id.flashBtn);
        resetBtn = findViewById(R.id.resetBtn);

        // 2. Initialize Location and DB
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = openOrCreateDatabase("UserStore.db", MODE_PRIVATE, null);

        // 3. Load Contact Data
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        fam1 = prefs.getString("phone1", "");
        fam2 = prefs.getString("phone2", "");
        fam3 = prefs.getString("phone3", "");

        // 4. Initialize Camera for Flashlight
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 5. Initialize Gyroscope
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // 6. Get Current Location
        getUserLocation();

        // 7. Click Listeners
        sosBtn.setOnClickListener(v -> showConfirmation("SOS EMERGENCY"));
        policeBtn.setOnClickListener(v -> showConfirmation("POLICE HELP NEEDED"));
        hospitalBtn.setOnClickListener(v -> showConfirmation("MEDICAL EMERGENCY"));
        flashBtn.setOnClickListener(v -> toggleFlashSOS());
        resetBtn.setOnClickListener(v -> resetApp());
    }

    // --- SENSOR METHODS ---

    @Override
    protected void onResume() {
        super.onResume();
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double angularSpeed = Math.sqrt(x * x + y * y + z * z);

            if (angularSpeed > GYRO_THRESHOLD) {
                // To prevent spamming SMS, we unregister temporarily
                sensorManager.unregisterListener(this);
                sendEmergencySMS("SUDDEN MOVEMENT DETECTED (GYRO)");

                Toast.makeText(this, "Movement Triggered SOS!", Toast.LENGTH_SHORT).show();

                // Restart sensor listening after 10 seconds
                new Handler().postDelayed(() -> {
                    sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }, 10000);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // --- CORE FUNCTIONALITY ---

    private void resetApp() {
        new AlertDialog.Builder(this)
                .setTitle("Reset App")
                .setMessage("All saved emergency contacts will be removed. Continue?")
                .setPositiveButton("YES", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    db.execSQL("DELETE FROM contacts");

                    Intent intent = new Intent(MainActivity2.this, PhoneActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showConfirmation(String type) {
        new AlertDialog.Builder(this)
                .setTitle("Emergency Alert")
                .setMessage("Are you sure you want to send an emergency alert?")
                .setPositiveButton("YES", (dialog, which) -> sendEmergencySMS(type))
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        convertLocationToAddress(location);
                    }
                });
    }

    private void convertLocationToAddress(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                userPincode = addresses.get(0).getPostalCode();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmergencySMS(String type) {
        try {
            // Improved Google Maps Link
            String message = type + "\n\nI need help.\n\nMy location:\n"
                    + "https://www.google.com/maps?q=" + latitude + "," + longitude;

            SmsManager smsManager = SmsManager.getDefault();
            String[] familyNumbers = {fam1, fam2, fam3};

            // Send to family
            for (String number : familyNumbers) {
                if (number != null && !number.isEmpty()) {
                    smsManager.sendTextMessage(number, null, message, null, null);
                }
            }

            // Send to nearby contacts based on Pincode
            if (userPincode != null) {
                Cursor cursor = db.rawQuery("SELECT phone1, phone2, phone3 FROM contacts WHERE pincode=?", new String[]{userPincode});
                if (cursor.moveToFirst()) {
                    for (int i = 0; i < 3; i++) {
                        String number = cursor.getString(i);
                        if (number != null && !number.isEmpty()) {
                            smsManager.sendTextMessage(number, null, message, null, null);
                        }
                    }
                }
                cursor.close();
            }
            Toast.makeText(this, "Emergency Alert Sent", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleFlashSOS() {
        if (!isFlashing) {
            isFlashing = true;
            flashBtn.setText("STOP FLASH");
            flashHandler.post(flashRunnable);
        } else {
            isFlashing = false;
            flashHandler.removeCallbacks(flashRunnable);
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            flashBtn.setText("FLASH SOS");
        }
    }

    Runnable flashRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFlashing) return;
            try {
                flashState = !flashState;
                cameraManager.setTorchMode(cameraId, flashState);
            } catch (Exception e) {
                e.printStackTrace();
            }
            flashHandler.postDelayed(this, 400);
        }
    };
}