package com.ghostsignal.imsidetector;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CellScanService extends Service {

    private Handler handler;
    private LocationManager locationManager;
    private Location lastLocation;
    private TelephonyManager tm;

    private void debug(String msg) {
        android.util.Log.d("GhostSignal", msg);
        if (MainActivity.instance != null) {
            MainActivity.instance.logToScreen(msg);
        }
    }

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            performScan();
            handler.postDelayed(this, Constants.SCAN_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    loc -> lastLocation = loc
            );
        }

        createNotificationChannel();
        startForeground(Constants.NOTIF_ID, buildNotification("Scanning..."));
        debug("Service cree et foreground lance");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(scanRunnable);
        handler.post(scanRunnable);
        debug("onStartCommand -> scanRunnable lance");
        return START_STICKY;
    }

    private void performScan() {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                debug("Debut scan");

                JSONObject data = new JSONObject();
                int cellId = -1;
                int signal = -100;
                int neighbors = 0;
                int rsrp = -140;
                int rsrq = -20;
                int sinr = -20;
                String network = "UNKNOWN";

                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED) {
                    List<CellInfo> cells = tm.getAllCellInfo();

                    if (cells != null) {
                        debug("Cells detectees: " + cells.size());
                        neighbors = Math.max(0, cells.size() - 1);

                        for (CellInfo ci : cells) {
                            if (!ci.isRegistered()) {
                                continue;
                            }

                            if (ci instanceof CellInfoLte) {
                                CellInfoLte l = (CellInfoLte) ci;
                                cellId = l.getCellIdentity().getCi();
                                signal = l.getCellSignalStrength().getDbm();
                                rsrp = l.getCellSignalStrength().getRsrp();
                                rsrq = l.getCellSignalStrength().getRsrq();
                                sinr = l.getCellSignalStrength().getRssnr();
                                network = "LTE";
                            } else if (ci instanceof CellInfoNr) {
                                CellInfoNr n = (CellInfoNr) ci;
                                cellId = (int) (((CellIdentityNr) n.getCellIdentity()).getNci() & 0xFFFFFFFFL);
                                signal = n.getCellSignalStrength().getDbm();
                                network = "5G";
                            } else if (ci instanceof CellInfoGsm) {
                                CellInfoGsm g = (CellInfoGsm) ci;
                                cellId = g.getCellIdentity().getCid();
                                signal = g.getCellSignalStrength().getDbm();
                                network = "GSM";
                            }

                            break;
                        }
                    }
                }

                data.put("network", network);
                data.put("cellId", cellId);
                data.put("signal", signal);
                data.put("neighbors", neighbors);
                data.put("rsrp", rsrp);
                data.put("rsrq", rsrq);
                data.put("sinr", sinr);
                data.put("latitude", lastLocation != null ? lastLocation.getLatitude() : 0.0);
                data.put("longitude", lastLocation != null ? lastLocation.getLongitude() : 0.0);
                data.put("deviceId", android.os.Build.MODEL);

                final int finalCellId = cellId;
                final String finalNetwork = network;

                debug("Envoi vers API...");
                URL url = new URL(Constants.API_ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();

                if (code == 200) {
                    debug("API OK (200)");

                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }

                    JSONObject resp = new JSONObject(sb.toString());
                    String status = resp.optString("status", "SAFE");
                    int score = resp.optInt("score", 0);
                    updateNotification(status, score);

                    JSONArray flagsArr = resp.optJSONArray("flags");
                    StringBuilder flagsStr = new StringBuilder();
                    if (flagsArr != null) {
                        for (int i = 0; i < flagsArr.length(); i++) {
                            if (i > 0) {
                                flagsStr.append(", ");
                            }
                            flagsStr.append(flagsArr.getString(i));
                        }
                    }

                    if (MainActivity.instance != null) {
                        MainActivity.instance.updateScanResult(
                                status,
                                score,
                                flagsStr.toString(),
                                finalNetwork,
                                finalCellId
                        );
                    }

                    if ("DANGER".equals(status)) {
                        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        if (v != null && v.hasVibrator()) {
                            v.vibrate(VibrationEffect.createWaveform(
                                    new long[]{0, 300, 100, 300, 100, 300},
                                    -1
                            ));
                        }
                    }
                } else {
                    debug("Erreur HTTP: " + code);
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.notify(Constants.NOTIF_ID,
                            buildNotification("Erreur HTTP: " + code + " - verif connexion"));

                    if (MainActivity.instance != null) {
                        MainActivity.instance.updateScanResult(
                                "SAFE",
                                0,
                                "Erreur HTTP " + code,
                                finalNetwork,
                                finalCellId
                        );
                    }
                }
            } catch (Exception e) {
                debug("EXCEPTION: " + e);
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void updateNotification(String status, int score) {
        String text = "DANGER".equals(status) ? "DANGER! Score: " + score + "/100"
                : "SUSPICIOUS".equals(status) ? "Suspect. Score: " + score + "/100"
                : "Securise. Score: " + score + "/100";

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(Constants.NOTIF_ID, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, Constants.NOTIF_CHANNEL_ID)
                .setContentTitle("GhostSignal")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
                Constants.NOTIF_CHANNEL_ID,
                "GhostSignal",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scanRunnable);
        super.onDestroy();
    }
}
