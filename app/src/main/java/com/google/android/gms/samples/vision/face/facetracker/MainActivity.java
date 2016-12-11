/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.EditText;

import java.util.List;
import java.util.jar.Manifest;

import static java.security.AccessController.getContext;


public final class MainActivity extends AppCompatActivity {
    //    Apps mempunyai 3 fragment yang di bind dengan viewpager
    int NUM_PAGES = 3;

    ViewPager viewPager;
    PagerAdapter pagerAdapter;

    //    -----------Wifi security code-------------
    private static final int SECURITY_NONE = 0;
    private static final int SECURITY_PSK = 1;
    private static final int SECURITY_WEP = 3;
//    -----------------------------------------

    WifiManager wifiManager;
    WifiConfiguration wifiConfiguration;
    private List<ScanResult> scanResult;

    //    Target Wifi BSSID dan SSID
    private static String TARGET_WIFI_BSSID = "00:26:5a:42:de:4e";
    private static String TARGET_WIFI_SSID = "Nelson";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

//        ---Deklarasi fragment fragment yang akan di gunakan---
        Fragment CameraFragment = new CameraFragment();
        Fragment LoginFramgent = new LoginFragment();
        Fragment ScheduleFragment = new ScheduleFragment();
//        -----------------------------------------------------

//        Meminta permisssion untuk GPS jika menggunakan android Marshmallow keatas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
//        -------------------------------------------------------------------------

        IntentFilter intentFilter = new IntentFilter();
        IntentFilter intentFilter1 = new IntentFilter();

        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        IntentFilter connectionIntentFilter = new IntentFilter();
        connectionIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);


        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        wifiConfiguration = new WifiConfiguration();

        registerReceiver(new wifiEnabled(wifiManager),intentFilter);
//        registerReceiver(new wifiConnecting(connectivityManager),connectionIntentFilter);

//       -------- Deklarasi view pager---------
        viewPager = (ViewPager) findViewById(R.id.fragmentFrame);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), CameraFragment, LoginFramgent, ScheduleFragment);
        viewPager.setAdapter(pagerAdapter);
//        Mengarahkan halaman awal saat membuka aplikasi ke fragment index 1
        viewPager.setCurrentItem(1);
//        -------------------------------------

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkWifi(wifiManager);
    }

    /*static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        }
        return SECURITY_NONE;
    }*/


    class wifiEnabled extends BroadcastReceiver {

        WifiManager wifiManager;
        ProgressDialog progressDialog;

        wifiEnabled(WifiManager wifiManager){
            this.wifiManager = wifiManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            switch (wifiState) {
//              ---------------jika wifi belom di nyalakan-------------
                case WifiManager.WIFI_STATE_DISABLED: {
//                      Buat dialog dimana akan meminta user untuk menyalakan wifi
                        new AlertDialog.Builder(context)
                                .setMessage("Wifi is needed for this app")
                                .setPositiveButton("Enable Wifi", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        wifiManager.setWifiEnabled(true);
                                        checkWifi(wifiManager);
                                    }
                                })
//                              Jika tidak, keluar dari aplikasi
                                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .setCancelable(false)
                                .show();
//              -------------------------------------------------------
                }
                break;
            }
        }
    }

    class wifiConnecting extends BroadcastReceiver{

        ConnectivityManager connectivityManager;
        wifiConnecting(ConnectivityManager connectivityManager){
            this.connectivityManager = connectivityManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                checkWifi(wifiManager);
            }
        }
    }


    void checkWifi(final WifiManager wifiManager){
        Log.d("WIFI CHECK",wifiManager.getConnectionInfo().toString());

        if(wifiManager.getWifiState()

        if(wifiManager.getConnectionInfo().getBSSID() == null){
            new AlertDialog.Builder(this)
                    .setMessage("Please Connect to wifi network named '"+TARGET_WIFI_SSID+"'")
                    .setPositiveButton("Connect to wifi", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
            return;
        }
        else if(wifiManager.getConnectionInfo().getBSSID().compareTo(TARGET_WIFI_BSSID) != 0 ){
            Log.d("hello","im here");
            new AlertDialog.Builder(this)
                    .setMessage("Please Connect to wifi network named '"+TARGET_WIFI_SSID+"'")
                    .setPositiveButton("Connect to wifi", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
            return;
        }
    }

    /*public void getWifi(final String WIFIBSSID) {
        final EditText passwordInput = new EditText(this);

        ProgressDialog progressDialog;

        wifiConfiguration.BSSID = WIFIBSSID;

        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


//        -------------Memeriksa apakah gps sudah aktif---------
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setMessage("GPS is needed to scan wifi networks")
                    .setCancelable(false)
                    .setPositiveButton("ENABLE GPS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(locationIntent);
                        }
                    })
                    .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        }
//        -------------------------------------------------------

        progressDialog = new ProgressDialog(this);


        if (wifiManager.isWifiEnabled()) {

            //progressDialog.show(this, "", "Scanning Wifi");

            Log.d("connectioninfo",wifiManager.getConnectionInfo().toString());
//            Jika user terkoneksi pada wifi lain atau jika user tidak terkoneksi dengan apapun
            if (wifiManager.getConnectionInfo().getBSSID() == null || wifiManager.getConnectionInfo().getBSSID().toString().compareTo(WIFIBSSID) != 0){

                wifiManager.startScan();
                scanResult = wifiManager.getScanResults();

                Log.d("ScanResult", String.valueOf(scanResult.size()));

                if (scanResult.size() != 0 || scanResult != null) {
                    Log.v("SCAN_RESULT", "SCAN RESULT SUCCESSFUL");
                    for (int i = 0; i < scanResult.size(); i++) {

                        if (scanResult.get(i).BSSID.equals(WIFIBSSID)) {

                            progressDialog.dismiss();

                            Log.v("SCAN_RESULT :", scanResult.get(i).SSID);
                            String WifiSSID = scanResult.get(i).SSID;
                            wifiConfiguration.BSSID = WIFIBSSID;
                            wifiConfiguration.SSID = WifiSSID;

                            final int ENCRYPTION = getSecurity(scanResult.get(i));

//                            Ubah input text menjadi input password
                            passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

                            new AlertDialog.Builder(this)
                                    .setMessage("Enter Wifi password for \"" + WifiSSID + "\"")
                                    .setView(passwordInput)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int j) {
                                            String WIFI_PASSWORD = passwordInput.getText().toString();
                                            switch (ENCRYPTION) {
                                                case SECURITY_WEP:
                                                    wifiConfiguration.wepKeys[0] = "\"" + WIFI_PASSWORD + "\"";
                                                    wifiConfiguration.wepTxKeyIndex = 0;
                                                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                                                    break;
                                                case SECURITY_PSK:
                                                    wifiConfiguration.preSharedKey = "\"" + WIFI_PASSWORD + "\"";
                                                    break;
                                                case SECURITY_NONE:
                                                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                                    break;
                                            }
//                                            Melakukan koneksi ke wifi
                                            int netId = wifiManager.addNetwork(wifiConfiguration);
                                            wifiManager.enableNetwork(netId, true);
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                            progressDialog.dismiss();
                            break;
                        }

                        progressDialog.dismiss();
//                       ------ Jika sudah memeriksa seluruh hasil scanning dan tidak menemukan network yang diinginkan----
                        if (i == scanResult.size()) {
                            new AlertDialog.Builder(this)
                                    .setMessage("We cannot find the office wifi network for the app, the wifi office wifi network is needed for this app to work")
                                    .setPositiveButton("TRY AGAIN", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int j) {
                                            getWifi(WIFIBSSID);
                                        }
                                    })
                                    .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                        }
//                        -------------------------------------------------------------------------------------------------
                    }
                }
                else {
                    new AlertDialog.Builder(this)
                            .setMessage("Cant detect any wifi network near you, get to better location to get wifi network signal")
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getWifi(WIFIBSSID);
                                }
                            })
                            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                }
                if (wifiManager.getConnectionInfo().getBSSID().equals(WIFIBSSID)) {
                    Log.v("NETWORK", "Network Already Available");
                }
            }
        }
    }*/

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        Fragment CameraFragment;
        Fragment LoginFragment;
        Fragment ScheduleFragment;

        public ScreenSlidePagerAdapter(FragmentManager fm, Fragment CameraFragment, Fragment LoginFragment, Fragment ScheduleFragment) {
            super(fm);
            this.CameraFragment = CameraFragment;
            this.LoginFragment = LoginFragment;
            this.ScheduleFragment = ScheduleFragment;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("Position Count", String.valueOf(position));
            if (position == 1) {
                return CameraFragment;
            }
            if (position == 2) {
                return LoginFragment;
            } else {
                return ScheduleFragment;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

}
