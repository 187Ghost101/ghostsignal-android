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
    private TextView tvStatus;
    private TextView tvScore;
    private TextView tvLastScan;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        setContentView(R.layout.activity_main);

        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        tvDebug = findViewById(R.id.tvDebug);
        tvStatus = findViewById(R.id.tvStatus);
        tvScore = findViewById(R.id.tvScore);
        tvLastScan = findViewById(R.id.tvLastScan);
        btnStart = findViewById(R.id.btnStart);

        logToScreen("MainActivity chargee");

        checkPermissions();

        btnStart.setOnClickListener(v -> {
            if (hasPermissions()) {
                tvPermissionStatus.setText("Permissions OK");
                logToScreen("Demarrage service");

                Intent intent = new Intent(this, CellScanService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }

            } else {
                requestPermissions();
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
        } else {
            tvPermissionStatus.setText("Permissions refusees");
            requestPermissions();
        }
    }

    private boolean hasPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
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
        if (requestCode != REQ_PERMS) return;

        if (hasPermissions()) {
            tvPermissionStatus.setText("Permissions OK");
        } else {
            tvPermissionStatus.setText("Bloque - ouvre Parametres");

            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    // 🔥 VERSION FINALE COMPATIBLE
    public void updateScanResult(String status, int score, String detail) {
        runOnUiThread(() -> {
            tvStatus.setText("Status: " + status);
            tvScore.setText("Score: " + score + "/100");
            tvLastScan.setText(detail);

            logToScreen(status + " | Score: " + score + " | " + detail);
        });
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
}
