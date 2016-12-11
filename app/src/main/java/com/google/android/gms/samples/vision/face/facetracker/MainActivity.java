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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
    private static String WIFIBSSID = "00:26:5a:42:de:4e";
//    Target Wifi SSID
    private static String WIFISSID = "Nelson";


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiConfiguration = new WifiConfiguration();
        getWifi();

//       -------- Deklarasi view pager---------
        viewPager = (ViewPager) findViewById(R.id.fragmentFrame);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
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

    class getWifiTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }


    /*class wifiScannerReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                List<ScanResult> scanResults = wifiManager.getScanResults();


                for (int i = 0; i < scanResult.size(); i++) {
                    Log.v("SCAN_RESULT :", scanResult.get(i).BSSID);
                    if (scanResult.get(i).BSSID.equals(WIFIBSSID)) {
                        Log.v("SCAN_RESULT :", scanResult.get(i).SSID);
                        wifiConfiguration.BSSID = WIFIBSSID;
                        wifiConfiguration.SSID = scanResult.get(i).SSID;

                        final int ENCRYPTION = getSecurity(scanResult.get(i));

                        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

                        new AlertDialog.Builder(this)
                                .setMessage("Enter Wifi password for \" " + WIFISSID + "\"")
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

                                                /*registerReceiver(new BroadcastReceiver() {
                                                    @Override
                                                    public void onReceive(Context context, Intent intent) {
                                                        progressDialog.dismiss();
                                                    }
                                                }, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
                                                wifiManager.enableNetwork(wifiManager.addNetwork(wifiConfiguration), true);
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void getWifi() {

                Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
                AlertDialog.Builder builder;

                final EditText passwordInput = new EditText(this);
                ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(this.CONNECTIVITY_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                wifiConfiguration.BSSID = WIFIBSSID;
                List<WifiConfiguration> wifiConfigurations;

                if (wifiManager.isWifiEnabled()) {
                    if (wifiManager.isScanAlwaysAvailable()) {
                        // TODO: 11/12/2016  jika sudah terkoneksi wifi network ..., jika belum ..., jika wifinya bukan wifi yg diinginkan ...

                        wifiConfigurations = wifiManager.getConfiguredNetworks();

                        Log.d("IS CONNECTED TO", wifiManager.getConnectionInfo().toString());

//                Jika koneksi wifi bukan dari access point yang diinginkan / jika tidak ada access point yang terkoneksi
                        if (!wifiManager.getConnectionInfo().getBSSID().equals(WIFIBSSID)) {
                            wifiManager.startScan();
                            scanResult = wifiManager.getScanResults();
                            Log.d("ScanResult", String.valueOf(scanResult.size()));

                            if (scanResult != null) {
                                Log.v("SCAN_RESULT", "SCAN RESULT SUCCESSFUL");

                                for (int i = 0; i < scanResult.size(); i++) {
                                    Log.v("SCAN_RESULT :", scanResult.get(i).BSSID);
                                    if (scanResult.get(i).BSSID.equals(WIFIBSSID)) {
                                        Log.v("SCAN_RESULT :", scanResult.get(i).SSID);
                                        wifiConfiguration.BSSID = WIFIBSSID;
                                        wifiConfiguration.SSID = scanResult.get(i).SSID;

                                        final int ENCRYPTION = getSecurity(scanResult.get(i));

                                        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

                                        new AlertDialog.Builder(this)
                                                .setMessage("Enter Wifi password for \" " + WIFISSID + "\"")
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

                                                /*registerReceiver(new BroadcastReceiver() {
                                                    @Override
                                                    public void onReceive(Context context, Intent intent) {
                                                        progressDialog.dismiss();
                                                    }
                                                }, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
                                                wifiManager.enableNetwork(wifiManager.addNetwork(wifiConfiguration), true);*/
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
                                                        getWifi();
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
                        } else {
                            Log.d("location", "IM HERE");
                        }
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage("Wifi Scanning is needed for this app")
                                .setPositiveButton("Enable Wifi Scanning", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startActivityForResult(new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE), 100);
                                        getWifi();
                                    }
                                })
                                .setCancelable(false)
                                .show();
                    }
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("Wifi is needed for this app")
                            .setPositiveButton("Enable Wifi", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    wifiManager.setWifiEnabled(true);
                                    getWifi();
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
            }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("Position Count", String.valueOf(position));
            if(position == 1){
                return new CameraFragment();
            }
            if(position == 2) {
                return new LoginFragment();
            }
            else{
                return new ScheduleFragment();
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    //Username kalo uda login
    String username = "";

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
