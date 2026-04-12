<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <TextView
            android:id="@+id/tvPermissionStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Permissions..."
            android:textColor="#FF3B30"
            android:textSize="18sp"/>

        <Button
            android:id="@+id/btnStart"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="START SCAN"/>

        <TextView
            android:id="@+id/tvStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Status"
            android:textColor="#FFFFFF"
            android:textSize="18sp"/>

        <TextView
            android:id="@+id/tvScore"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Score"
            android:textColor="#FFD60A"
            android:textSize="18sp"/>

        <TextView
            android:id="@+id/tvLastScan"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Last Scan"
            android:textColor="#BDBDBD"
            android:textSize="16sp"/>

        <TextView
            android:id="@+id/tvDebug"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="#00FF00"
            android:textSize="14sp"/>

    </LinearLayout>
</ScrollView>
