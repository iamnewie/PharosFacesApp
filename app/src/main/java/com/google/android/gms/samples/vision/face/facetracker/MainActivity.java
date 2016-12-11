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
import android.graphics.Camera;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
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

    //    Target Wifi BSSID
    private static String TARGET_WIFI_BSSID = "00:26:5a:42:de:4e";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        Fragment CameraFragment = new CameraFragment();
        Fragment LoginFramgent = new LoginFragment();
        Fragment ScheduleFragment = new ScheduleFragment();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiConfiguration = new WifiConfiguration();
        getWifi(TARGET_WIFI_BSSID);

//       -------- Deklarasi view pager---------
        viewPager = (ViewPager) findViewById(R.id.fragmentFrame);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),CameraFragment,LoginFramgent,ScheduleFragment);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(1);
//        -------------------------------------


    }

    static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        }
        return SECURITY_NONE;
    }

   /* class wifiScannerReceiver extends BroadcastReceiver{

        final EditText passwordInput = new EditText(getBaseContext());
        String WIFIBSSID;
        WifiManager wifiManager;

        wifiScannerReceiver(String WIFIBSSID,WifiManager wifiManager){
            this.WIFIBSSID = WIFIBSSID;
            this.wifiManager = wifiManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                List<ScanResult> scanResults = wifiManager.getScanResults();

                for (int i = 0; i < scanResult.size(); i++) {
                    Log.v("SCAN_RESULT :", scanResult.get(i).BSSID);
                    if (scanResult.get(i).BSSID.equals(WIFIBSSID)) {
                        Log.v("SCAN_RESULT :", scanResult.get(i).SSID);
                        String WifiSSID =  scanResult.get(i).SSID;
                        wifiConfiguration.BSSID = WIFIBSSID;
                        wifiConfiguration.SSID = WifiSSID;

                        final int ENCRYPTION = getSecurity(scanResult.get(i));

                        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

                        new AlertDialog.Builder(getBaseContext())
                                .setMessage("Enter Wifi password for \" " + WifiSSID + "\"")
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

                                    }
                                })
                                .setCancelable(false)
                                .show();
                        break;

                    }
                }
            }
        }
    }*/

    public void getWifi(final String WIFIBSSID) {

        final EditText passwordInput = new EditText(this);
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(this.CONNECTIVITY_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        wifiConfiguration.BSSID = WIFIBSSID;
        final List<WifiConfiguration> wifiConfigurations;

//        ---------------jika wifi belom di nyalakan-------------
        if(!wifiManager.isWifiEnabled()){
            new AlertDialog.Builder(this)
                    .setMessage("Wifi is needed for this app")
                    .setPositiveButton("Enable Wifi", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            wifiManager.setWifiEnabled(true);
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
//        ------------------------------------------------------

        if (wifiManager.isWifiEnabled()) {

            Log.d("IS CONNECTED TO", wifiManager.getConnectionInfo().toString());

//                Jika koneksi wifi bukan dari access point yang diinginkan / jika tidak ada access point yang terkoneksi
            if (!wifiManager.getConnectionInfo().getBSSID().equals(WIFIBSSID)) {
                wifiManager.startScan();
                scanResult = wifiManager.getScanResults();

                Log.d("ScanResult", String.valueOf(scanResult.size()));

                if (scanResult != null) {
                    Log.v("SCAN_RESULT", "SCAN RESULT SUCCESSFUL");
                    for (int i = 0; i < scanResult.size(); i++) {

                        if (scanResult.get(i).BSSID.equals(WIFIBSSID)) {
                            Log.v("SCAN_RESULT :", scanResult.get(i).SSID);
                            String WifiSSID = scanResult.get(i).SSID;
                            wifiConfiguration.BSSID = WIFIBSSID;
                            wifiConfiguration.SSID = WifiSSID;

                            final int ENCRYPTION = getSecurity(scanResult.get(i));

//                            Ubah input text menjadi input password
                            passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

                            new AlertDialog.Builder(this)
                                    .setMessage("Enter Wifi password for \" " + WifiSSID + "\"")
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
                                            wifiManager.enableNetwork(netId,true);
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                            break;
                        }
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
                    }
                }
            }
            else if(wifiManager.getConnectionInfo().getBSSID().equals(WIFIBSSID)){
                Log.v("NETWORK","Network Already Available");
            }
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        Fragment CameraFragment;
        Fragment LoginFragment;
        Fragment ScheduleFragment;

        public ScreenSlidePagerAdapter(FragmentManager fm,Fragment CameraFragment, Fragment LoginFragment, Fragment ScheduleFragment) {
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
