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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public final class MainActivity extends AppCompatActivity {
    //    Apps mempunyai 3 fragment yang di bind dengan viewpager
    int NUM_PAGES = 3;

    ViewPager viewPager;
    PagerAdapter pagerAdapter;

    WifiManager wifiManager;
    WifiConfiguration wifiConfiguration;

    SharedPreferences sharedPreferences;

    //    Target Wifi BSSID dan SSID
    private static String TARGET_WIFI_BSSID = "00:26:5a:42:de:4e";
    private static String TARGET_WIFI_SSID = "Nelson";

    //    Username kalo uda login
    String username = "";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

//        ---Ambil username dari sharedpreference agar user tetap dapat melihat schedule jika keluar dari apps------
        sharedPreferences = getSharedPreferences("PHAROS", MODE_PRIVATE);
        username = sharedPreferences.getString("Username", "");
//        ---------------------------------------------------------------------------------------------------------

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

//       ------- Instantiasi intent filter untuk broadcast recevier------
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        IntentFilter connectionIntentFilter = new IntentFilter();
        connectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
//        ---------------------------------------------------------------------


        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiConfiguration = new WifiConfiguration();

        registerReceiver(new wifiEnabled(wifiManager), intentFilter);
        registerReceiver(new wifiConnecting(wifiManager), connectionIntentFilter);

//       -------- Deklarasi view pager---------
        viewPager = (ViewPager) findViewById(R.id.fragmentFrame);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), CameraFragment, LoginFramgent, ScheduleFragment);
        viewPager.setAdapter(pagerAdapter);
//        Mengarahkan halaman awal saat membuka aplikasi ke fragment index 1
        viewPager.setCurrentItem(1);
//        -------------------------------------

    }


    //    Class sebuah broadcast receiver yang listen adanya perubahan pada state wifi
    class wifiEnabled extends BroadcastReceiver {

        WifiManager wifiManager;
        ProgressDialog progressDialog;

        wifiEnabled(WifiManager wifiManager) {
            this.wifiManager = wifiManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            switch (wifiState) {
//              ---------------jika wifi belom di nyalakan-------------
                case WifiManager.WIFI_STATE_DISABLED: {
//                      Buat dialog dimana akan meminta user untuk menyalakan wifi
                    new AlertDialog.Builder(context)
                            .setMessage("Wifi is needed for this app")
                            .setPositiveButton("Enable Wifi", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    wifiManager.setWifiEnabled(true);
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

    class wifiConnecting extends BroadcastReceiver {

        //        Membuat sebuah semaphore receiver hanya dapat sekali, karena terdapat bug dimana broadcast receiver menerima lebih dari sekali
        boolean firstConnect = true;

        WifiManager wifiManager;

        wifiConnecting(WifiManager wifiManager) {
            this.wifiManager = wifiManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                if (firstConnect) {
                    Log.v("Wifi listener", "Wifi is Connected to a network");
                    checkWifi(wifiManager);
                    firstConnect = false;
                } else {
                    firstConnect = true;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkWifi(wifiManager);
    }

    void checkWifi(final WifiManager wifiManager) {
        Log.d("WIFI CHECK", wifiManager.getConnectionInfo().toString());

        if (wifiManager.getConnectionInfo().getBSSID() == null || wifiManager.getConnectionInfo().getBSSID().toString().compareTo(TARGET_WIFI_BSSID) != 0) {
            new AlertDialog.Builder(this)
                    .setMessage("Please Connect to wifi network named '" + TARGET_WIFI_SSID + "'")
                    .setPositiveButton("Connect to wifi", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
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


    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }
}
