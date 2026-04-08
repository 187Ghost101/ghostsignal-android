package com.ghostsignal.imsidetector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int PERM_CODE = 100;

    private final String[] PERMS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
    };

    public static MainActivity instance;

    private TextView statusText;
    private TextView resultText;
    private TextView scoreText;
    private TextView flagsText;
    private TextView lastScanText;
    private TextView debugText;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 80, 48, 48);
        layout.setBackgroundColor(Color.parseColor("#0A0A0A"));

        TextView title = new TextView(this);
        title.setText("GhostSignal");
        title.setTextSize(32);
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setTypeface(null, Typeface.BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Detecteur IMSI Catcher - Donnees reelles");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.parseColor("#888888"));
        subtitle.setPadding(0, 8, 0, 24);
        layout.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Service inactif");
        statusText.setTextSize(15);
        statusText.setTextColor(Color.parseColor("#FF4444"));
        layout.addView(statusText);

        Button btn = new Button(this);
        btn.setText("DEMARRER LA SURVEILLANCE");
        btn.setBackgroundColor(Color.parseColor("#00E5FF"));
        btn.setTextColor(Color.BLACK);
        btn.setOnClickListener(v -> {
            logToScreen("Bouton clique");
            if (hasPerms()) {
                logToScreen("Permissions OK");
                startScan();
            } else {
                logToScreen("Permissions manquantes - demande envoyee");
                requestPermissions(PERMS, PERM_CODE);
            }
        });
        layout.addView(btn);

        resultText = new TextView(this);
        resultText.setText("En attente...");
        resultText.setTextSize(24);
        resultText.setTextColor(Color.parseColor("#AAAAAA"));
        resultText.setTypeface(null, Typeface.BOLD);
        resultText.setPadding(0, 30, 0, 10);
        layout.addView(resultText);

        scoreText = new TextView(this);
        scoreText.setText("");
        scoreText.setTextSize(14);
        scoreText.setTextColor(Color.parseColor("#00E5FF"));
        layout.addView(scoreText);

        flagsText = new TextView(this);
        flagsText.setText("");
        flagsText.setTextSize(13);
        flagsText.setTextColor(Color.parseColor("#FFAA00"));
        flagsText.setPadding(0, 8, 0, 8);
        layout.addView(flagsText);

        lastScanText = new TextView(this);
        lastScanText.setText("Aucun scan recu");
        lastScanText.setTextSize(12);
        lastScanText.setTextColor(Color.parseColor("#888888"));
        layout.addView(lastScanText);

        debugText = new TextView(this);
        debugText.setText("\n--- DEBUG ---\n");
        debugText.setTextSize(12);
        debugText.setTextColor(Color.parseColor("#CCCCCC"));
        debugText.setPadding(0, 30, 0, 0);
        layout.addView(debugText);

        scroll.addView(layout);
        setContentView(scroll);

        logToScreen("MainActivity chargee");

        if (hasPerms()) {
            logToScreen("Permissions deja accordees");
            startScan();
        } else {
            logToScreen("Permissions a demander au lancement");
            requestPermissions(PERMS, PERM_CODE);
        }
    }

    public void updateScanResult(String status, int score, String flags, String network, int cellId) {
        handler.post(() -> {
            logToScreen("updateScanResult appelee: " + status);

            int color = Color.parseColor("#00FF88");
            if ("DANGER".equals(status)) color = Color.parseColor("#FF3333");
            else if ("SUSPICIOUS".equals(status)) color = Color.parseColor("#FFAA00");

            resultText.setText(status);
            resultText.setTextColor(color);
            scoreText.setText("Score: " + score + "/100 | Reseau: " + network + " | CellID: " + cellId);
            flagsText.setText("Flags: " + (flags == null ? "aucun" : flags));
            lastScanText.setText("Dernier scan: " +
                    new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(new java.util.Date()));
        });
    }

    public void logToScreen(String msg) {
        handler.post(() -> {
            String current = debugText.getText().toString();
            String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            debugText.setText(current + "\n[" + time + "] " + msg);
        });
    }

    private boolean hasPerms() {
        for (String p : PERMS) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startScan() {
        try {
            startForegroundService(new Intent(this, CellScanService.class));
            statusText.setText("Surveillance active - donnees envoyees au cloud");
            statusText.setTextColor(Color.parseColor("#00FF88"));
            logToScreen("Foreground service demarre");
        } catch (Exception e) {
            logToScreen("Erreur startScan: " + e.getMessage());
            statusText.setText("Erreur lancement service");
            statusText.setTextColor(Color.RED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (hasPerms()) {
            logToScreen("Permissions acceptees");
            startScan();
        } else {
            logToScreen("Permissions refusees");
            statusText.setText("Permissions refusees");
            statusText.setTextColor(Color.RED);
        }
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
