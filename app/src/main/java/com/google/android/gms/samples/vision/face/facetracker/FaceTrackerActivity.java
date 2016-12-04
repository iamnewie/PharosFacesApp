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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

    private static final int SECURITY_NONE = 0;
    private static final int SECURITY_PSK = 1;
    private static final int SECURITY_WEP = 3;

    private static final int PORT = 2000;

    private static String IPNUM = "192.168.0.3";
    private static String WIFIBSSID = "00:26:5a:42:de:4e";
    private static String WIFISSID = "Nelson";

    WifiManager wifiManager;
    WifiConfiguration wifiConfiguration;

    private List<ScanResult> scanResult;

    int faceNumber = 0;

    private CameraSource mCameraSource = null;
    private float FrontFaceFPS = 15.0f;
    private float BackFaceFPS = 25.0f;

    private GraphicOverlay mOverlay;
    private FaceGraphic mFaceGraphic;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private ImageButton cameraButton;
    private ImageButton logoutButton;

    String username = null;
    String userId = null;
    String loginCode = null;
    String logoutCode = null;
    Socket socket;

    SharedPreferences preferences;
    SharedPreferences.Editor preferenceseditor;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiConfiguration = new WifiConfiguration();
        setContentView(R.layout.main);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
        else{
            finish();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        }
        else{
            finish();
        }*/


        getWifi();
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        logoutButton = (ImageButton) findViewById(R.id.logoutButton);

        //SHARED PREFERENCES
        preferences = getSharedPreferences("PHAROS",Context.MODE_PRIVATE);
        preferenceseditor = preferences.edit();

        boolean cameraBtnEnabled = preferences.getBoolean("Camera",true);
        boolean logoutBtnEnabled = preferences.getBoolean("Logout",false);

        //kalo belom login
        if (cameraBtnEnabled && !logoutBtnEnabled){
            cameraButton.setEnabled(true);
            cameraButton.setClickable(true);
            logoutButton.setEnabled(false);
            logoutButton.setClickable(false);
            logoutButton.setImageResource(R.drawable.icon3_disabled);
        }
        //kalo blom logout
        else {
            cameraButton.setEnabled(false);
            cameraButton.setClickable(false);
            logoutButton.setEnabled(true);
            logoutButton.setClickable(true);
            logoutButton.setImageResource(R.drawable.icon3_enable);
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        logoutButtonClicks();
        cameraButtonClicks();
    }

    private void logoutButtonClicks(){

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View view) {
                ProgressDialog progress = new ProgressDialog(FaceTrackerActivity.this);
                progress.setMessage("Logging out");
                progress.show();
                try{
                    new Thread (new sendLogoutThread()).start();    //Buat thread, kirim logout request
                    progress.dismiss();
                     //Logout sukses

                    preferenceseditor.putBoolean("Logout",false);
                    preferenceseditor.putBoolean("Camera",true);
                    cameraButton.setEnabled(true);
                    cameraButton.setClickable(true);
                    logoutButton.setEnabled(false);
                    logoutButton.setClickable(false);
                    logoutButton.setImageResource(R.drawable.icon3_disabled);
                    preferenceseditor.commit();

                        new AlertDialog.Builder(FaceTrackerActivity.this)
                                .setMessage("Logout Success")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }).show();

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void cameraButtonClicks() {
        cameraButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    cameraButton.setImageResource(R.drawable.icon2);
                } else if (motionEvent.getAction() == android.view.MotionEvent.ACTION_UP) {
                    cameraButton.setImageResource(R.drawable.icon1);
                }
                return false;
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("FaceNum", String.valueOf(faceNumber).toString());
                if (faceNumber == 0) {
                    new AlertDialog.Builder(FaceTrackerActivity.this)
                            .setTitle("Error")
                            .setMessage("No face detected")
                            .setCancelable(true)
                            .show();
                } else if (faceNumber > 1) {
                    new AlertDialog.Builder(FaceTrackerActivity.this)
                            .setTitle("Error")
                            .setMessage("More than one face is detected")
                            .setCancelable(true)
                            .show();
                } else if (faceNumber == 1) {
                    mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] bytes) {
                            try {
                                capturePic(bytes);
                            } catch (Exception e) {
                                Log.d(TAG, "FACE NOT FOUND");
                            }
                        }
                    });
                }
            }
        });
    }

    static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        }
        return SECURITY_NONE;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void getWifi() {

        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        AlertDialog.Builder builder;

        final EditText passwordInput = new EditText(this);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        wifiConfiguration.BSSID = WIFIBSSID;
        List<WifiConfiguration> wifiConfigurations;

        if (wifiManager.isWifiEnabled()) {
            if (wifiManager.isScanAlwaysAvailable()) {
                // TODO: 11/12/2016  jika sudah terkoneksi wifi network ..., jika belum ..., jika wifinya bukan wifi yg diinginkan ...

                wifiConfigurations = wifiManager.getConfiguredNetworks();

                Log.d("IS CONNECTING", wifiManager.getConnectionInfo().toString());
//                Jika koneksi wifi bukan dar access point yang diinginkan / jika tidak ada access point yang terkoneksi
                if (!wifiManager.getConnectionInfo().getBSSID().equals(WIFIBSSID)) {

                    scanResult = wifiManager.getScanResults();

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
                    .setCancelable(false)
                    .show();
        }

    }

    private void capturePic(byte[] bytes) {

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Checking");
        progress.show();

        try {

            Date now = new Date();
            android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

            Log.d("HEIGHT WIDTH", bitmap.getHeight() + " " + bitmap.getWidth());
            byte[] arrayByte = stream.toByteArray();

            Thread thread;

            thread = new Thread(new listenThread(arrayByte,bitmap));
            thread.start();
            thread.join();
            progress.dismiss();
            Log.d("username",username);
                new AlertDialog.Builder(this)
                        .setMessage("Username found. Are you "+ username + " ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                try {
                                    Thread thread = new Thread(new confirmThread("yes"));
                                    thread.start();
                                    thread.join();
                                    socket.close();

                                    preferenceseditor.putBoolean("Logout",true);
                                    preferenceseditor.putBoolean("Camera",false);
                                    preferenceseditor.putString("UserID",userId);
                                    cameraButton.setEnabled(false);
                                    cameraButton.setClickable(false);
                                    logoutButton.setEnabled(true);
                                    logoutButton.setClickable(true);
                                    logoutButton.setImageResource(R.drawable.icon3_enable);
                                    preferenceseditor.commit();
                                    new AlertDialog.Builder(FaceTrackerActivity.this)
                                            .setMessage("Login success.\n" + "Welcome, " + username + "!\n")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            }).show();
                                    //Simpen Button state ke SharedPreferences


                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                new Thread(new confirmThread("no")).start();
                                new AlertDialog.Builder(FaceTrackerActivity.this)
                                        .setMessage("Please take your self picture again!")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                try{
                                                    Thread thread = new Thread(new confirmThread("no"));
                                                    thread.start();
                                                    thread.join();
                                                    socket.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                dialogInterface.dismiss();
                                            }
                                        }).show();
                                dialogInterface.dismiss();
                            }
                        }).show();
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class sendLogoutThread implements Runnable{
        @Override
        public void run() {
            try{
                InetAddress HOST = InetAddress.getByName(IPNUM);
                socket = new Socket(HOST, PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String data = "LOGOUT;"+ preferences.getString("UserID",null);   //FORMAT: "LOGOUT;userId"
                dataOutputStream.write(data.getBytes());
                dataOutputStream.flush();
                socket.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    class listenThread implements Runnable {

        byte[] arrayByte;
        Bitmap croppedArea;
        listenThread(byte[] arrayByte,Bitmap croppedArea){
            this.arrayByte = arrayByte;
            this.croppedArea = croppedArea;
        }

        @Override
        public void run() {
            try {
                InetAddress HOST = InetAddress.getByName(IPNUM);
                socket = new Socket(HOST, PORT);

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String data = "SIZE;" + croppedArea.getWidth() + ";" + croppedArea.getHeight()+ ";" + arrayByte.length;
                dataOutputStream.flush();
                dataOutputStream.write(data.getBytes());
                dataOutputStream.flush();

                // menunggu response ack
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                StringBuffer readbuffer = new StringBuffer();
                StringBuilder sb = new StringBuilder();

                byte inputByte;

                while (true) {
                    while((inputByte = dataInputStream.readByte())!=0){
                        readbuffer.append((char)inputByte);
                    }
                    Log.d("ACK",readbuffer.toString());
                    if (readbuffer.toString().contains("ACK")){
                        Log.d("ACK","Received ACK");
                        break;
                    }
                }

                dataOutputStream.flush();

                //Kirim gambar
                dataOutputStream.write(arrayByte);
                dataOutputStream.flush();

                //Listen response login

                StringBuilder readBufferLogin = new StringBuilder();
                String responses;
                String[] response;

                byte inputByteLogin;

                while(true){
                    while ((inputByteLogin = dataInputStream.readByte()) != 0){
                        readBufferLogin.append((char) inputByteLogin);
                    }
                    responses = readBufferLogin.toString(); //format terimanya : "id;username"
                    Log.d("responses",responses);

                    response = responses.split(";");
                    userId = response[0];
                    username = response[1];
                    break;
                }
                dataOutputStream.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Kirim konfirmasi ke server bahwa wajah valid
    class confirmThread implements Runnable{
        String  message = null;
        confirmThread(String message){
            this.message = message;
        }

        @Override
        public void run() {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                Log.d("Message",message);
                dataOutputStream.flush();
                dataOutputStream.write(message.getBytes());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(getApplicationContext(), detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(15.0f)
                .build();

    }


    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        //progressDialog.dismiss();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {

                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */

    private class GraphicFaceTracker extends Tracker<Face> {

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            faceNumber++;
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            faceNumber--;
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            faceNumber = 0;
            mOverlay.remove(mFaceGraphic);
        }
    }
}
