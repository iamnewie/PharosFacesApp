package com.google.android.gms.samples.vision.face.facetracker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

/**
 * Created by Home on 12/10/2016.
 */
public class CameraFragment extends Fragment {

    private static final String TAG = "FaceTracker";


    private static final int PORT = 2000;

    private static String IPNUM = "192.168.0.3";

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

    public ImageButton cameraButton;
    public ImageButton logoutButton;

    String username = null;
    String userId = null;
    Socket socket;

    SharedPreferences preferences;
    SharedPreferences.Editor preferenceseditor;
    boolean cameraBtnEnabled;
    boolean logoutBtnEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        ----------SHARED PREFERENCES------------------------
        preferences = getContext().getSharedPreferences("PHAROS", Context.MODE_PRIVATE);
        preferenceseditor = preferences.edit();

        cameraBtnEnabled = preferences.getBoolean("Camera", true);
        logoutBtnEnabled = preferences.getBoolean("Logout", false);
//        -----------------------------------------------------
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.main, container, false);

        mPreview = (CameraSourcePreview) view.findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) view.findViewById(R.id.faceOverlay);

        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton);
        logoutButton = (ImageButton) view.findViewById(R.id.logoutButton);

        //kalo belom login
        if (cameraBtnEnabled && !logoutBtnEnabled) {
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
        int rc = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        logoutButtonClicks();
        cameraButtonClicks();
        return view;
    }

    private void logoutButtonClicks() {

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progress = new ProgressDialog(getContext());
                progress.setMessage("Logging out");
                progress.show();
                try {
                    Thread thread = new Thread(new sendLogoutThread());  //Buat thread, kirim logout request
                    thread.start();
                    thread.join();
                    progress.dismiss();
                    //Logout sukses

                    preferenceseditor.putBoolean("Logout", false);
                    preferenceseditor.putBoolean("Camera", true);
                    preferenceseditor.putString("Username", "");
                    preferenceseditor.putString("UserID", "");
                    cameraButton.setEnabled(true);
                    cameraButton.setClickable(true);
                    logoutButton.setEnabled(false);
                    logoutButton.setClickable(false);
                    logoutButton.setImageResource(R.drawable.icon3_disabled);
                    preferenceseditor.commit();

//                    -----------Clear schedule pada schedulefragment jika logout------------
                    TextView nameText = (TextView) getActivity().findViewById(R.id.name_text);
                    TextView staffIdText = (TextView) getActivity().findViewById(R.id.staffid_text);
                    nameText.setText(null);
                    staffIdText.setText(null);

                    ListView dateView = (ListView) getActivity().findViewById(R.id.date_view);
                    dateView.setAdapter(null);

                    ImageView imageView = (ImageView) getActivity().findViewById(R.id.profile_image);
                    imageView.setImageBitmap(null);
//                    -------------------------------------------------

                    new AlertDialog.Builder(getContext())
                            .setMessage("Logout Success")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();

                } catch (Exception e) {
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
                    new AlertDialog.Builder(getContext())
                            .setTitle("Error")
                            .setMessage("No face detected")
                            .setCancelable(true)
                            .show();
                } else if (faceNumber > 1) {
                    new AlertDialog.Builder(getContext())
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


    private void capturePic(byte[] bytes) {

        try {
            Boolean listenTaskBool = new listenTask(bytes).execute().get();

//            Menunggu hasil dari async task
            if (listenTaskBool) {
                new AlertDialog.Builder(getContext())
                        .setMessage("Username found. Are you " + username + " ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                try {
                                    dialogInterface.dismiss();
                                    Boolean confirmTaskBool = new confirmTask("yes").execute().get();
                                    if (confirmTaskBool) {
                                        socket.close();

                                        ((MainActivity) getActivity()).setUsername(username);
                                        preferenceseditor.putBoolean("Logout", true);
                                        preferenceseditor.putBoolean("Camera", false);
                                        preferenceseditor.putString("UserID", userId);
                                        preferenceseditor.putString("Username", username);
                                        cameraButton.setEnabled(false);
                                        cameraButton.setClickable(false);
                                        logoutButton.setEnabled(true);
                                        logoutButton.setClickable(true);
                                        logoutButton.setImageResource(R.drawable.icon3_enable);
                                        preferenceseditor.commit();

                                        new AlertDialog.Builder(getContext())
                                                .setMessage("Login success.\n" + "Welcome, " + username + "!\n")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        dialogInterface.dismiss();
                                                    }
                                                }).show();
                                    }
                                    if (!confirmTaskBool) {
                                        new AlertDialog.Builder(getContext())
                                                .setMessage("Anda Sudah Absen Hari ini")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        dialogInterface.dismiss();
                                                    }
                                                }).show();
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                try {
                                    dialogInterface.dismiss();
                                    Boolean confirmTaskBool = new confirmTask("no").execute().get();
                                    socket.close();

                                    new AlertDialog.Builder(getContext())
                                            .setMessage("Please take your self picture again!")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            }).show();

                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    --------Thread yang digunakan saat user hendak logout-----
    class sendLogoutThread implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress HOST = InetAddress.getByName(IPNUM);
                socket = new Socket(HOST, PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String data = "LOGOUT;" + preferences.getString("UserID", null);   //FORMAT: "LOGOUT;userId"
                ((MainActivity) getActivity()).setUsername("");
                dataOutputStream.write(data.getBytes());
                dataOutputStream.flush();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
//    ----------------------------------------------------------

    //    -------Async task yang digunakan jika user hendak mengirim face recognition request ke server------
    class listenTask extends AsyncTask<Void, Void, Boolean> {

        byte[] bytes;
        ProgressDialog progressDialog;

        listenTask(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Loading");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                socket = new Socket(IPNUM, PORT);

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Log.d("HEIGHT WIDTH", bitmap.getHeight() + " " + bitmap.getWidth());

                //Convert to jpeg format
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] arrayByte = stream.toByteArray();

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String data = "SIZE;" + bitmap.getWidth() + ";" + bitmap.getHeight() + ";" + arrayByte.length;
                dataOutputStream.flush();
                dataOutputStream.write(data.getBytes());
                dataOutputStream.flush();

                // menunggu response ack
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                StringBuilder readbuffer = new StringBuilder();

                byte inputByte;

                while ((inputByte = dataInputStream.readByte()) != 0) {
                    readbuffer.append((char) inputByte);
                }
                Log.d("ACK", readbuffer.toString());
                if (readbuffer.toString().contains("ACK")) {
                    Log.d("ACK", "Received ACK");
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

                while ((inputByteLogin = dataInputStream.readByte()) != 0) {
                    readBufferLogin.append((char) inputByteLogin);
                }
                responses = readBufferLogin.toString(); //format terimanya : "id;username"
                Log.d("responses", responses);

                response = responses.split(";");
                userId = response[0];
                username = response[1];

                dataOutputStream.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();
        }
    }
//    --------------------------------------------------------------------------------------

    class confirmTask extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog progressDialog;

        String message;

        confirmTask(String message) {
            this.message = message;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getContext());

            progressDialog.setMessage("Loading");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                Log.d("Message", message);
                dataOutputStream.flush();
                dataOutputStream.write(message.getBytes());
                dataOutputStream.flush();

                StringBuilder stringBuffer = new StringBuilder();
                byte inputByte;

                if (message.compareTo("yes") == 0) {
                    while (true) {
                        while ((inputByte = dataInputStream.readByte()) != 0) {
                            stringBuffer.append((char) inputByte);
                        }
                        Log.d("StringBuffer", stringBuffer.toString());
                        if (stringBuffer.toString().contains("SUCCESS")) {
                            Log.d("SUCCESS", "Received SUCCESS");
                            dataInputStream.close();
                            return true;
                        } else if (stringBuffer.toString().contains("FAIL")) {
                            Log.d("FAIL", "Received FAIL");
                            dataInputStream.close();
                            return false;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            progressDialog.dismiss();
        }
    }


    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(getActivity(), permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }


        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(getActivity(), permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void createCameraSource() {

        Context context = getContext();
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

        mCameraSource = new CameraSource.Builder(getContext(), detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(15.0f)
                .build();

    }


    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.stop();
        //progressDialog.dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

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
                getActivity().finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
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

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

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
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            faceNumber--;
            mOverlay.remove(mFaceGraphic);
        }
    }

}
