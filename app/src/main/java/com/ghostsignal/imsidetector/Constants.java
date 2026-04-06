package com.ghostsignal.imsidetector;

public class Constants {
    public static final String API_ENDPOINT = "https://api.base44.com/api/apps/69bbda5b75a19519f5fc1e19/functions/receiveScan";
    public static final long SCAN_INTERVAL_MS = 30_000L;
    public static final int DANGER_THRESHOLD = 60;
    public static final int SUSPICIOUS_THRESHOLD = 30;
    public static final String NOTIF_CHANNEL_ID = "ghost_signal_scan";
    public static final int NOTIF_ID = 1001;
}