package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class MainActivity2 extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationClient;

    Button sosBtn, policeBtn, hospitalBtn, flashBtn, resetBtn;

    double latitude;
    double longitude;

    String userPincode;

    SQLiteDatabase db;

    String fam1, fam2, fam3;

    CameraManager cameraManager;
    String cameraId;

    boolean isFlashing = false;
    boolean flashState = false;

    Handler flashHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        sosBtn = findViewById(R.id.sosBtn);
        policeBtn = findViewById(R.id.policeBtn);
        hospitalBtn = findViewById(R.id.hospitalBtn);
        flashBtn = findViewById(R.id.flashBtn);
        resetBtn = findViewById(R.id.resetBtn);   // FIXED CRASH

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        db = openOrCreateDatabase("UserStore.db", MODE_PRIVATE, null);

        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        fam1 = prefs.getString("phone1","");
        fam2 = prefs.getString("phone2","");
        fam3 = prefs.getString("phone3","");

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }

        getUserLocation();

        sosBtn.setOnClickListener(v -> showConfirmation("SOS EMERGENCY"));

        policeBtn.setOnClickListener(v -> showConfirmation("POLICE HELP NEEDED"));

        hospitalBtn.setOnClickListener(v -> showConfirmation("MEDICAL EMERGENCY"));

        flashBtn.setOnClickListener(v -> toggleFlashSOS());

       resetBtn.setOnClickListener(v -> resetApp());
    }

    // Reset App
    private void resetApp() {
        new AlertDialog.Builder(this)
                .setTitle("Reset App")
                .setMessage("All saved emergency contacts will be removed. Continue?")
                .setPositiveButton("YES", (dialog, which) -> {

                    // 1. Clear SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();

                    // 2. Clear SQLite Database (Optional but recommended for a full reset)
                    db.execSQL("DELETE FROM contacts");

                    // 3. Redirect to Registration Page
                    // CHANGE 'RegistrationActivity' to your actual first activity name
                    Intent intent = new Intent(MainActivity2.this, PhoneActivity.class);

                    // This clears the backstack so the user can't press 'Back' to return to the SOS page
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    Toast.makeText(this, "App Reset Complete", Toast.LENGTH_LONG).show();
                    finish(); // Close the current activity

                })
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Confirmation popup
    private void showConfirmation(String type){

        new AlertDialog.Builder(this)
                .setTitle("Emergency Alert")
                .setMessage("Are you sure you want to send an emergency alert?")
                .setPositiveButton("YES",(dialog,which)-> sendEmergencySMS(type))
                .setNegativeButton("NO",(dialog,which)-> dialog.dismiss())
                .show();
    }

    // Get GPS location
    private void getUserLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this,"Location permission missing",Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null) {

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                convertLocationToAddress(location);

            } else {

                Toast.makeText(this,"Unable to get location",Toast.LENGTH_SHORT).show();
            }

        });
    }

    // Convert location to pincode
    private void convertLocationToAddress(Location location) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {

            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {

                Address address = addresses.get(0);

                userPincode = address.getPostalCode();

                Toast.makeText(this,"Detected Pincode: "+userPincode,Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {

            Toast.makeText(this,"Geocoder error",Toast.LENGTH_SHORT).show();
        }
    }

    // Send Emergency SMS
    private void sendEmergencySMS(String type) {

        try {

            String message = type + "\n\nI need help.\n\nMy location:\n"
                    + "https://maps.google.com/?q=" + latitude + "," + longitude;

            SmsManager smsManager = SmsManager.getDefault();

            String[] familyNumbers = {fam1, fam2, fam3};

            for(String number : familyNumbers){

                if(number != null && !number.isEmpty()){

                    smsManager.sendTextMessage(number,null,message,null,null);
                }
            }

            if(userPincode != null){

                Cursor cursor = db.rawQuery(
                        "SELECT phone1, phone2, phone3 FROM contacts WHERE pincode=?",
                        new String[]{userPincode}
                );

                if(cursor.moveToFirst()){

                    for(int i=0;i<3;i++){

                        String number = cursor.getString(i);

                        if(number != null && !number.isEmpty()){

                            smsManager.sendTextMessage(number,null,message,null,null);
                        }
                    }
                }

                cursor.close();
            }

            Toast.makeText(this,"Emergency Alert Sent",Toast.LENGTH_LONG).show();

        } catch (Exception e) {

            Toast.makeText(this,"SMS Failed: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    // Flash SOS
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