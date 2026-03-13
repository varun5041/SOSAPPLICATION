package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText phone1, phone2, phone3;
    Button saveBtn;

    boolean permissionsGranted;

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionsGranted = getIntent().getBooleanExtra("permissionsGranted", false);

        phone1 = findViewById(R.id.phone1);
        phone2 = findViewById(R.id.phone2);
        phone3 = findViewById(R.id.phone3);
        saveBtn = findViewById(R.id.saveBtn);

        // Create database
        db = openOrCreateDatabase("UserStore.db", MODE_PRIVATE, null);

        // Create table
        db.execSQL("CREATE TABLE IF NOT EXISTS contacts(" +
                "pincode TEXT PRIMARY KEY," +
                "phone1 TEXT," +
                "phone2 TEXT," +
                "phone3 TEXT)");

        saveBtn.setOnClickListener(v -> saveContacts());
    }

    private void saveContacts() {

        String p1 = phone1.getText().toString();
        String p2 = phone2.getText().toString();
        String p3 = phone3.getText().toString();

        if(p1.isEmpty() || p2.isEmpty() || p3.isEmpty()){
            Toast.makeText(this,"Enter all numbers",Toast.LENGTH_SHORT).show();
            return;
        }

        // Save user numbers in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("phone1", p1);
        editor.putString("phone2", p2);
        editor.putString("phone3", p3);
        editor.putBoolean("isRegistered", true);

        editor.apply();

        // Insert fixed emergency numbers into database
        try {

            db.execSQL("INSERT OR IGNORE INTO contacts VALUES('401404','9221740188','9321661326','9867904968')");
            db.execSQL("INSERT OR IGNORE INTO contacts VALUES('401102','9221740188','9321661326','9867904968')");
            db.execSQL("INSERT OR IGNORE INTO contacts VALUES('401303','9221740188','9321661326','9867904968')");
            db.execSQL("INSERT OR IGNORE INTO contacts VALUES('401202','9221740188','9321661326','9867904968')");

        } catch (Exception e) {

            Toast.makeText(this,"Database Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }

        Toast.makeText(this, "Contacts Saved Successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        intent.putExtra("permissionsGranted", permissionsGranted);

        startActivity(intent);
        finish();
    }
}