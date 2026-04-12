package com.ghostsignal.imsidetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 1001;

    private TextView tvPermissionStatus;
    private TextView tvDebug;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        tvDebug = findViewById(R.id.tvDebug);
        btnStart = findViewById(R.id.btnStart);

        appendDebug("MainActivity chargee");

        checkPermissionsOnLaunch();

        btnStart.setOnClickListener(v -> {
            appendDebug("Bouton clique");

            if (hasRequiredPermissions()) {
                tvPermissionStatus.setText("Permissions OK");
                startService(new Intent(this, CellScanService.class));
            } else {
                appendDebug("Permissions manquantes - demande envoyee");
                requestRequiredPermissions();
            }
        });
    }

    private void checkPermissionsOnLaunch() {
        appendDebug("Permissions a demander au lancement");

        if (hasRequiredPermissions()) {
            tvPermissionStatus.setText("Permissions OK");
            appendDebug("Permissions acceptees");
        } else {
            tvPermissionStatus.setText("Permissions refusees");
            requestRequiredPermissions();
        }
    }

    private boolean hasRequiredPermissions() {
        boolean fineLocation =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        boolean phoneState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED;

        return fineLocation && phoneState;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE
                },
                REQ_PERMS
        );
    }

    @Override
 public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
)
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQ_PERMS) return;

        if (hasRequiredPermissions()) {
            tvPermissionStatus.setText("Permissions acceptees");
            appendDebug("Permissions acceptees");
            startService(new Intent(this, CellScanService.class));
            return;
        }

        boolean showFine =
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean showPhone =
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE);

        if (!showFine || !showPhone) {
            tvPermissionStatus.setText("Permissions bloquees - ouvrir Parametres");
            appendDebug("Permissions bloquees par Android - ouverture des parametres");

            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            tvPermissionStatus.setText("Permissions refusees");
            appendDebug("Permissions refusees");
        }
    }

    private void appendDebug(String msg) {
        String old = tvDebug.getText().toString();
        String next = old + "\n" + msg;
        tvDebug.setText(next);
    }
}
