package com.ghostsignal.imsidetector;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import android.view.View;
import android.Manifest;

public class MainActivity extends Activity {

    private static final int PERM_CODE = 100;
    private final String[] PERMS = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    };
    private TextView statusText;
    private TextView resultText;
    private TextView scoreText;
    private TextView flagsText;
    private TextView lastScanText;
    private Handler handler = new Handler(Looper.getMainLooper());

    public static MainActivity instance;

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
        subtitle.setText("Detecteur IMSI Catcher v3.0 - 10s scan");
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
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 20, 0, 24);
        btn.setLayoutParams(p);
        btn.setOnClickListener(v -> {
            if (hasPerms()) startScan();
            else requestPermissions(PERMS, PERM_CODE);
        });
        layout.addView(btn);

        // Separator
        View sep = new View(this);
        sep.setBackgroundColor(Color.parseColor("#222222"));
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2);
        sepParams.setMargins(0, 0, 0, 16);
        sep.setLayoutParams(sepParams);
        layout.addView(sep);

        TextView resultsTitle = new TextView(this);
        resultsTitle.setText("DERNIER SCAN");
        resultsTitle.setTextSize(11);
        resultsTitle.setTextColor(Color.parseColor("#555555"));
        resultsTitle.setTypeface(null, Typeface.BOLD);
        resultsTitle.setPadding(0, 0, 0, 8);
        layout.addView(resultsTitle);

        resultText = new TextView(this);
        resultText.setText("En attente du premier scan...");
        resultText.setTextSize(22);
        resultText.setTextColor(Color.parseColor("#AAAAAA"));
        resultText.setTypeface(null, Typeface.BOLD);
        layout.addView(resultText);

        scoreText = new TextView(this);
        scoreText.setText("");
        scoreText.setTextSize(14);
        scoreText.setTextColor(Color.parseColor("#00E5FF"));
        scoreText.setPadding(0, 4, 0, 4);
        layout.addView(scoreText);

        flagsText = new TextView(this);
        flagsText.setText("");
        flagsText.setTextSize(12);
        flagsText.setTextColor(Color.parseColor("#FFAA00"));
        flagsText.setPadding(0, 4, 0, 4);
        layout.addView(flagsText);

        lastScanText = new TextView(this);
        lastScanText.setText("Premier scan dans ~30 secondes...");
        lastScanText.setTextSize(11);
        lastScanText.setTextColor(Color.parseColor("#888888"));
        lastScanText.setPadding(0, 8, 0, 0);
        layout.addView(lastScanText);

        scroll.addView(layout);
        setContentView(scroll);

        if (hasPerms()) startScan();
        else requestPermissions(PERMS, PERM_CODE);
    }

    public void updateScanResult(String status, int score, String flags, String network, int cellId) {
        handler.post(() -> {
            String emoji = "DANGER".equals(status) ? "DANGER" : "SUSPICIOUS".equals(status) ? "SUSPECT" : "SECURISE";
            int color = "DANGER".equals(status) ? Color.parseColor("#FF3333") :
                        "SUSPICIOUS".equals(status) ? Color.parseColor("#FFAA00") :
                        Color.parseColor("#00FF88");
            resultText.setText(emoji);
            resultText.setTextColor(color);
            scoreText.setText("Score: " + score + "/100  |  Reseau: " + network + "  |  Cell: " + cellId);
            flagsText.setText(flags.isEmpty() ? "Aucune anomalie detectee" : "Anomalies: " + flags);
            flagsText.setTextColor(flags.isEmpty() ? Color.parseColor("#00FF88") : Color.parseColor("#FFAA00"));
            lastScanText.setText("Mis a jour: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        });
    }

    private boolean hasPerms() {
        for (String p : PERMS)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    private void startScan() {
        startForegroundService(new Intent(this, CellScanService.class));
        statusText.setText("Surveillance active");
        statusText.setTextColor(Color.parseColor("#00FF88"));
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (hasPerms()) startScan();
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
