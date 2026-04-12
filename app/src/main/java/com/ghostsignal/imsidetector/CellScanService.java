package com.ghostsignal.imsidetector;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CellScanService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GhostSignal", "Service started OK");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
