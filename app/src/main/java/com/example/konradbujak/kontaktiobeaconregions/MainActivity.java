package com.example.konradbujak.kontaktiobeaconregions;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.configuration.scan.ScanMode;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.device.BeaconRegion;
import com.kontakt.sdk.android.ble.device.EddystoneNamespace;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ProximityManager KontaktManager;
    String TAG = "MyActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onetimeconfiguration();
    }
    @Override
    protected void onStart() {
        checkPermissionAndStart();
        super.onStop();
    }
    @Override
    protected void onStop() {
        KontaktManager.stopScanning();
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        KontaktManager.disconnect();
        KontaktManager = null;
        super.onDestroy();
    }
    private void checkPermissionAndStart() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            //already granted
            Log.d(TAG,"Permission already granted");
            startScan();
        } else
        {
                //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            Log.d(TAG,"Permission request called");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (100 == requestCode) {
                Log.d(TAG,"Permission granted");
                startScan();
            }
        } else
        {
            Log.d(TAG,"Permission not granted");
            showToast("Kontakt.io SDK require this permission");
        }
    }
    private void onetimeconfiguration(){
        sdkInitialise();
        configureProximityManager();
        configureListeners();
        configureSpaces();
    }
    public void sdkInitialise()
    {
        KontaktSDK.initialize("Put your secret API Key here");
        if (KontaktSDK.isInitialized())
            Log.v(TAG, "SDK initialised");
    }
    private void configureProximityManager() {
        KontaktManager = new ProximityManager(this);
        KontaktManager.configuration()
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.create(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(20)))
                .activityCheckConfiguration(ActivityCheckConfiguration.DEFAULT);
    }
    private void configureListeners() {
        KontaktManager.setIBeaconListener(createIBeaconListener());
        KontaktManager.setEddystoneListener(createEddystoneListener());
        KontaktManager.setScanStatusListener(createScanStatusListener());
        Log.d(TAG,"Listeners Configured");
    }
    private void configureSpaces() {
        Collection<IBeaconRegion> beaconRegions = new ArrayList<>();
        Collection<IEddystoneNamespace> eddystoneNamespaces = new ArrayList<>();
        eddystoneNamespaces.add(EddystoneNamespace.create("namespace1", "f7826da64fa24e988024", false));
        eddystoneNamespaces.add(EddystoneNamespace.create("namespace2", "2b17b17d1dea47d1a690", false));
        IBeaconRegion region1 = new BeaconRegion.Builder()
                .setIdentifier("region1")
                .setProximity(UUID.fromString("17826da4-4fa3-4e98-8024-bc5b71e0893e"))
                .setMinor(1009)
                .build();
        IBeaconRegion region2 = new BeaconRegion.Builder()
                .setIdentifier("region2")
                .setProximity(UUID.fromString("17826da4-4fa3-4e98-8024-bc5b71e0893e"))
                .setMinor(1014)
                .build();
        IBeaconRegion region3 = new BeaconRegion.Builder()
                .setIdentifier("region3")
                .setProximity(UUID.fromString("17826da4-4fa3-4e98-8024-bc5b71e0893e"))
                .setMajor(2000)
                .build();
        beaconRegions.add(region1);
        beaconRegions.add(region2);
        beaconRegions.add(region3);
        KontaktManager.spaces().iBeaconRegions(beaconRegions);
        Log.d(TAG,"Regions configured");
    }
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
    private EddystoneListener createEddystoneListener()
    {
        return new SimpleEddystoneListener()
        {
            @Override public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace eddystoneNamespaces) {
                if ("namespace1".equals(eddystoneNamespaces.getName())){
                    Log.d(TAG, eddystoneNamespaces.getName() + " discovered! UniqueID: " + eddystone.getUniqueId());
                    showToast(eddystoneNamespaces.getName() + " entered");
                }
                if ("namespace2".equals(eddystoneNamespaces.getName())){
                    Log.d(TAG, eddystoneNamespaces.getName() + " discovered! UniqueID: " + eddystone.getUniqueId());
                    showToast(eddystoneNamespaces.getName() + " entered");
                }
            }
        };
    }
    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion beaconRegion) {
                if ("region1".equals(beaconRegion.getIdentifier()))
                {
                    Log.d(TAG,beaconRegion.getIdentifier() +  " region  discovered! UniqueID: " + ibeacon.getUniqueId());
                    showToast(beaconRegion.getIdentifier() + " entered");
                }
                if ("region2".equals(beaconRegion.getIdentifier())) {
                    Log.d(TAG,beaconRegion.getIdentifier() +  " region  discovered! UniqueID: " + ibeacon.getUniqueId());
                    showToast(beaconRegion.getIdentifier() + " entered");
                }
                if ("region3".equals(beaconRegion.getIdentifier())) {
                    Log.d(TAG,beaconRegion.getIdentifier() +  " region  discovered! UniqueID: " + ibeacon.getUniqueId());
                    showToast(beaconRegion.getIdentifier() + " entered");
                }
            }
        };
    }
    private ScanStatusListener createScanStatusListener() {
        return new SimpleScanStatusListener() {
            @Override
            public void onScanStart()
            {
                Log.d(TAG,"Scanning started");
                showToast("Scanning started");
            }
            @Override
            public void onScanStop()
            {
                Log.d(TAG,"Scanning stopped");
                showToast("Scanning stopped");
            }
        };
    }
    private void startScan() {
        KontaktManager.connect(new OnServiceReadyListener()
        {
            @Override
            public void onServiceReady() {
                KontaktManager.startScanning();
            }
        });
    }
}
