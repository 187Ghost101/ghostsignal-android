package com.ghostsignal.imsidetector;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.*;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 96, 48, 48);
        layout.setBackgroundColor(Color.parseColor("#0A0A0A"));

        TextView title = new TextView(this);
        title.setText("GhostSignal");
        title.setTextSize(36);
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setTypeface(null, Typeface.BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Detecteur IMSI Catcher - Donnees reelles");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#888888"));
        subtitle.setPadding(0, 16, 0, 48);
        layout.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Service inactif");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.parseColor("#FF4444"));
        layout.addView(statusText);

        Button btn = new Button(this);
        btn.setText("DEMARRER LA SURVEILLANCE");
        btn.setBackgroundColor(Color.parseColor("#00E5FF"));
        btn.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 32, 0, 0);
        btn.setLayoutParams(p);
        btn.setOnClickListener(v -> {
            if (hasPerms()) startScan();
            else requestPermissions(PERMS, PERM_CODE);
        });
        layout.addView(btn);

        scroll.addView(layout);
        setContentView(scroll);

        if (hasPerms()) startScan();
        else requestPermissions(PERMS, PERM_CODE);
    }

    private boolean hasPerms() {
        for (String p : PERMS)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    private void startScan() {
        startForegroundService(new Intent(this, CellScanService.class));
        statusText.setText("Surveillance active - donnees envoyees au cloud");
        statusText.setTextColor(Color.parseColor("#00FF88"));
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (hasPerms()) startScan();
    }
}