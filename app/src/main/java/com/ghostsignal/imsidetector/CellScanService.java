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
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

public class CellScanService extends Service {

    private static final String CHANNEL_ID = "ghostsignal_scan";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification("Scan en cours..."));
        log("Service demarre");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scanCells();
        return START_STICKY;
    }

    private void scanCells() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            log("Location refusee");
            updateUI("NO PERMISSION", 0, "ACCESS_FINE_LOCATION refusee");
            return;
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            log("Phone state refuse");
            updateUI("NO PERMISSION", 0, "READ_PHONE_STATE refusee");
            return;
        }

        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            if (tm == null) {
                log("TelephonyManager null");
                updateUI("ERROR", 0, "TelephonyManager null");
                return;
            }

            List<CellInfo> infos = tm.getAllCellInfo();

            if (infos == null || infos.isEmpty()) {
                log("Aucune cellule detectee");
                updateUI("NO DATA", 10, "Aucune cellule detectee");
                return;
            }

            int score = 0;
            String status = "SAFE";
            String detail = "";

            int lteCount = 0;
            int nrCount = 0;
            int gsmCount = 0;
            int wcdmaCount = 0;

            for (CellInfo info : infos) {

                if (info instanceof CellInfoLte) {
                    lteCount++;
                    CellIdentityLte id = ((CellInfoLte) info).getCellIdentity();
                    detail = "LTE | CI=" + id.getCi() + " TAC=" + id.getTac() + " PCI=" + id.getPci();
                    log(detail);
                }

                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info instanceof CellInfoNr) {
                    nrCount++;
                    CellIdentityNr id = (CellIdentityNr) ((CellInfoNr) info).getCellIdentity();
                    detail = "5G | NCI=" + id.getNci() + " TAC=" + id.getTac() + " PCI=" + id.getPci();
                    log(detail);
                }

                else if (info instanceof CellInfoWcdma) {
                    wcdmaCount++;
                    CellIdentityWcdma id = ((CellInfoWcdma) info).getCellIdentity();
                    detail = "3G | CID=" + id.getCid() + " LAC=" + id.getLac() + " PSC=" + id.getPsc();
                    log(detail);
                }

                else if (info instanceof CellInfoGsm) {
                    gsmCount++;
                    CellIdentityGsm id = ((CellInfoGsm) info).getCellIdentity();
                    detail = "2G | CID=" + id.getCid() + " LAC=" + id.getLac();
                    log(detail);
                }

                else {
                    log("Autre type detecte: " + info.getClass().getSimpleName());
                }
            }

            if (gsmCount > 0 && lteCount == 0 && nrCount == 0) {
                score = 80;
                status = "HIGH RISK";
            } else if (gsmCount > 0 && (lteCount > 0 || nrCount > 0 || wcdmaCount > 0)) {
                score = 50;
                status = "SUSPICIOUS";
            } else if (wcdmaCount > 0 && lteCount == 0 && nrCount == 0) {
                score = 30;
                status = "MEDIUM RISK";
            } else if (lteCount > 0 || nrCount > 0) {
                score = 10;
                status = "SAFE";
            } else {
                score = 20;
                status = "UNKNOWN";
            }

            String summary =
                    detail +
                    " | LTE:" + lteCount +
                    " | 5G:" + nrCount +
                    " | 3G:" + wcdmaCount +
                    " | 2G:" + gsmCount;

            updateUI(status, score, summary);

        } catch (Exception e) {
            log("Erreur scan: " + e.getMessage());
            updateUI("ERROR", 0, e.getMessage() == null ? "Erreur inconnue" : e.getMessage());
        }
    }

    private void updateUI(String status, int score, String detail) {
        if (MainActivity.instance != null) {
            MainActivity.instance.updateScanResult(status, score, detail);
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
