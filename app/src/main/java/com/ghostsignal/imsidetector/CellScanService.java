package com.ghostsignal.imsidetector;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.*;
import android.util.Log;

import java.util.List;

public class CellScanService extends Service {

    private static final String CHANNEL_ID = "ghostsignal_scan";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification("Scan en cours..."));
        log("Service démarré");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scanCells();
        return START_STICKY;
    }

    private void scanCells() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            log("❌ Location refusée");
            return;
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            log("❌ Phone state refusé");
            return;
        }

        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            List<CellInfo> infos = tm.getAllCellInfo();

            if (infos == null || infos.isEmpty()) {
                log("⚠️ Aucune cellule détectée");
                return;
            }

            log("📡 Cellules trouvées: " + infos.size());

            for (CellInfo info : infos) {

                if (info instanceof CellInfoLte) {
                    CellIdentityLte id = ((CellInfoLte) info).getCellIdentity();
                    String msg = "LTE | CI=" + id.getCi() +
                            " TAC=" + id.getTac() +
                            " PCI=" + id.getPci();
                    log(msg);
                    updateUI("LTE détecté", msg);
                }

                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info instanceof CellInfoNr) {
                    CellIdentityNr id = (CellIdentityNr) ((CellInfoNr) info).getCellIdentity();
                    String msg = "5G NR | NCI=" + id.getNci() +
                            " TAC=" + id.getTac();
                    log(msg);
                    updateUI("5G détecté", msg);
                }

                else if (info instanceof CellInfoWcdma) {
                    CellIdentityWcdma id = ((CellInfoWcdma) info).getCellIdentity();
                    String msg = "3G | CID=" + id.getCid();
                    log(msg);
                    updateUI("3G détecté", msg);
                }

                else if (info instanceof CellInfoGsm) {
                    CellIdentityGsm id = ((CellInfoGsm) info).getCellIdentity();
                    String msg = "2G | CID=" + id.getCid();
                    log(msg);
                    updateUI("2G détecté", msg);
                }
            }

        } catch (Exception e) {
            log("💥 Erreur: " + e.getMessage());
        }
    }

    private void updateUI(String status, String detail) {
        if (MainActivity.instance != null) {
            MainActivity.instance.updateScanResult(status, detail);
        }
    }

    private void log(String msg) {
        Log.d("GhostSignal", msg);
        if (MainActivity.instance != null) {
            MainActivity.instance.logToScreen(msg);
        }
    }

    private Notification buildNotification(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("GhostSignal")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle("GhostSignal")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .build();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GhostSignal Scan",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
