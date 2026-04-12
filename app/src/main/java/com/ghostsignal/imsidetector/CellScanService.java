package com.ghostsignal.imsidetector;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.telephony.*;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class CellScanService extends Service {

    private static final String CHANNEL_ID = "ghostsignal_scan";
    private static final long SCAN_INTERVAL = 5000;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable scanner;

    private String lastCell = "";

    private ArrayList<String> history = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification("Surveillance active"));
        log("Service demarre");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (scanner == null) {
            scanner = new Runnable() {
                @Override
                public void run() {
                    scanCells();
                    handler.postDelayed(this, SCAN_INTERVAL);
                }
            };
            handler.post(scanner);
        }

        return START_STICKY;
    }

    private void scanCells() {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            updateUI("NO PERMISSION", 0, "Location refusee");
            return;
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            updateUI("NO PERMISSION", 0, "Phone state refuse");
            return;
        }

        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm == null) {
                updateUI("ERROR", 0, "TelephonyManager null");
                return;
            }

            List<CellInfo> infos = tm.getAllCellInfo();

            if (infos == null || infos.isEmpty()) {
                updateUI("NO DATA", 10, "Aucune cellule");
                return;
            }

            int lte = 0, nr = 0, gsm = 0, wcdma = 0;
            String detail = "";

            for (CellInfo info : infos) {

                if (info instanceof CellInfoLte) {
                    lte++;
                    CellIdentityLte id = ((CellInfoLte) info).getCellIdentity();
                    detail = "LTE CI=" + id.getCi();
                }

                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info instanceof CellInfoNr) {
                    nr++;
                    CellIdentityNr id = (CellIdentityNr) ((CellInfoNr) info).getCellIdentity();
                    detail = "5G NCI=" + id.getNci();
                }

                else if (info instanceof CellInfoWcdma) {
                    wcdma++;
                    CellIdentityWcdma id = ((CellInfoWcdma) info).getCellIdentity();
                    detail = "3G CID=" + id.getCid();
                }

                else if (info instanceof CellInfoGsm) {
                    gsm++;
                    CellIdentityGsm id = ((CellInfoGsm) info).getCellIdentity();
                    detail = "2G CID=" + id.getCid();
                }
            }

            int score;
            String status;

            if (gsm > 0 && lte == 0 && nr == 0) {
                score = 80;
                status = "HIGH RISK";
            } else if (gsm > 0) {
                score = 50;
                status = "SUSPICIOUS";
            } else if (wcdma > 0 && lte == 0) {
                score = 30;
                status = "MEDIUM";
            } else if (lte > 0 || nr > 0) {
                score = 10;
                status = "SAFE";
            } else {
                score = 20;
                status = "UNKNOWN";
            }

            // changement cellule
            if (!detail.equals(lastCell) && !lastCell.isEmpty()) {
                score += 30;
                status = "CELL SWITCH";
                log("⚠️ changement cellule");
            }

            lastCell = detail;

            String summary = detail +
                    " | LTE:" + lte +
                    " 5G:" + nr +
                    " 3G:" + wcdma +
                    " 2G:" + gsm;

            history.add(summary);
            if (history.size() > 20) history.remove(0);

            updateUI(status, score, summary);

            if (score >= 70) {
                triggerAlert("Network anomaly detected");
            }

            if (score >= 80) {
                exportLogs();
            }

        } catch (Exception e) {
            updateUI("ERROR", 0, e.getMessage());
        }
    }

    private void triggerAlert(String msg) {

        log("🚨 ALERT: " + msg);

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(500);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notif = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GhostSignal ALERT")
                .setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();

        nm.notify(2, notif);
    }

    private void exportLogs() {
        try {
            File file = new File(getExternalFilesDir(null), "ghost_log.txt");
            FileWriter w = new FileWriter(file);

            for (String s : history) {
                w.write(s + "\n");
            }

            w.close();
            log("📁 export: " + file.getAbsolutePath());

        } catch (Exception e) {
            log("export error: " + e.getMessage());
        }
    }

    private void updateUI(String status, int score, String detail) {
        if (MainActivity.instance != null) {
            MainActivity.instance.updateScanResult(status, score, detail);
        }
    }

    private void log(String m) {
        Log.d("GhostSignal", m);
        if (MainActivity.instance != null) {
            MainActivity.instance.logToScreen(m);
        }
    }

    private Notification buildNotification(String text) {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GhostSignal")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL_ID,
                    "Scan",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager m = getSystemService(NotificationManager.class);
            if (m != null) m.createNotificationChannel(c);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanner != null) handler.removeCallbacks(scanner);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
