package com.ghostsignal.imsidetector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static MainActivity instance;

    private static final int REQ_PERMS = 1001;

    private TextView tvPermissionStatus;
    private TextView tvDebug;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        setContentView(R.layout.activity_main);

        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        tvDebug = findViewById(R.id.tvDebug);
        btnStart = findViewById(R.id.btnStart);

        appendDebug("MainActivity chargee");
        checkPermissions();

        btnStart.setOnClickListener(v -> {
            if (hasPermissions()) {
                tvPermissionStatus.setText("OK");
                appendDebug("Demarrage du service");
                startService(new Intent(this, CellScanService.class));
            } else {
                appendDebug("Permissions manquantes");
                requestRequiredPermissions();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }

    private void checkPermissions() {
        if (hasPermissions()) {
            tvPermissionStatus.setText("Permissions OK");
            appendDebug("Permissions OK");
        } else {
            tvPermissionStatus.setText("Permissions refusees");
            appendDebug("Permissions refusees");
            requestRequiredPermissions();
        }
    }

    private boolean hasPermissions() {
        boolean fineLocation =
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        boolean phoneState =
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED;

        return fineLocation && phoneState;
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    },
                    REQ_PERMS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQ_PERMS) {
            return;
        }

        if (hasPermissions()) {
            tvPermissionStatus.setText("Permissions acceptees");
            appendDebug("Permissions acceptees");
        } else {
            tvPermissionStatus.setText("Bloque - ouvre Parametres");
            appendDebug("Permissions bloquees");

            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    public void updateScanResult(String status, String detail) {
        runOnUiThread(() -> logToScreen(status + " | " + detail));
    }

    public void logToScreen(String msg) {
        runOnUiThread(() -> {
            String old = tvDebug.getText().toString();
            if (old.isEmpty()) {
                tvDebug.setText(msg);
            } else {
                tvDebug.setText(old + "\n" + msg);
            }
        });
    }

    private void appendDebug(String msg) {
        String old = tvDebug.getText().toString();
        if (old.isEmpty()) {
            tvDebug.setText(msg);
        } else {
            tvDebug.setText(old + "\n" + msg);
        }
    }
}
