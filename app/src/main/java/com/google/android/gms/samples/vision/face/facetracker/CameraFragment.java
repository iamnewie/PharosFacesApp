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

/**
 * Created by Home on 12/10/2016.
 */
public class CameraFragment extends Fragment {

    private static final String TAG = "FaceTracker";

    //    Port number dan ipaddress/domain name server
    private static final int PORT = 2000;
    private static String IPNUM = "192.168.0.3";
//    --------------------------------------------

    //    Jumlah default face count yang terdeteksi
    int faceNumber = 0;
    //    --------------------------------------------
    private CameraSource mCameraSource = null;

    private GraphicOverlay mOverlay;
    private FaceGraphic mFaceGraphic;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    //    Camera shutter button dan logout button
    public ImageButton cameraButton;
    public ImageButton logoutButton;
//    --------------------------------------

    //    Default value username dan userid
    String username = null;
    String userId = null;
//    ------------------------------------

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

//        Memeriksa state button berdasarkan dari sharedpreference
//        agar apps dapat mengingat state button saat keluar dari apps
        if (cameraBtnEnabled && !logoutBtnEnabled) {
            cameraButton.setEnabled(true);
            cameraButton.setClickable(true);
            logoutButton.setEnabled(false);
            logoutButton.setClickable(false);
            logoutButton.setImageResource(R.drawable.icon3_disabled);
        } else {
            cameraButton.setEnabled(false);
            cameraButton.setClickable(false);
            logoutButton.setEnabled(true);
            logoutButton.setClickable(true);
            logoutButton.setImageResource(R.drawable.icon3_enable);
        }
//        ------------------------------------------------------------

//        Mengecek permission camera jika belom
//        di beri permission akan meminta user untuk
//        memberi permission
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


    //    OnClick listener saat logoutbutton di click
    private void logoutButtonClicks() {

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Membuat sebuah progress dialog yang melakukan proses logut
                ProgressDialog progress = new ProgressDialog(getContext());
                progress.setMessage("Logging out");
                progress.show();
                try {
//                    Membuat thread yang mengirim logout request
                    Thread thread = new Thread(new sendLogoutThread());
                    thread.start();
                    thread.join();
                    progress.dismiss();

//                    Mengubah state camera shutter button menjadi enabled
//                    dan state logout button menjadi disabled
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
//                    ---------------------------------------------------------------------

//                    -----------Clear schedule pada schedulefragment jika logout------------
                    TextView nameText = (TextView) getActivity().findViewById(R.id.name_text);
                    TextView staffIdText = (TextView) getActivity().findViewById(R.id.staffid_text);
                    nameText.setText(null);
                    staffIdText.setText(null);

                    ListView dateView = (ListView) getActivity().findViewById(R.id.date_view);
                    dateView.setAdapter(null);

                    ImageView imageView = (ImageView) getActivity().findViewById(R.id.profile_image);
                    imageView.setImageBitmap(null);
//                    -----------------------------------------------------------------

//                    Alert dialog yang menyatakan bahwa proses logout telah sukses
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

    //    OnClick listener saat camera shutter button di click
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
//                Jika tidak ada wajah yang terdeteksi maka
//                proses check in absensi tidak dapat di lakukan
                if (faceNumber == 0) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Error")
                            .setMessage("No face detected")
                            .setCancelable(true)
                            .show();
//                    Jika lebih dari 1 wajah yang terdeteksi maka
//                    proses check in absensi tidak dapat di lakukan
                } else if (faceNumber > 1) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Error")
                            .setMessage("More than one face is detected")
                            .setCancelable(true)
                            .show();
//                    Proses check in absensi dapat dilakukan
//                    jika hanya 1 wajah yang terdeteksi
                } else if (faceNumber == 1) {
                    mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] bytes) {
                            try {
//                                Eksekusi sebuah async task yang melakukan proses login
                                new loginTask(bytes).execute();
                            } catch (Exception e) {
                                Log.d(TAG, "FACE NOT FOUND");
                            }
                        }
                    });
                }
            }
        });
    }

    //    Fungsi yang menampilkan dialog dialog yang diperlukan saat melakukan proses konfirmasi bahwa
    //    user hanya login sekali hari ini
    private void confirmTaskDialog(Boolean confirmTaskBool) {

        try {
//            Jika user sudah login hari ini maka proses login berakhir secara sukses
            if (confirmTaskBool) {
                socket.close();
//                Set variable username global menjadi username user yang
//                telah melakukan proses login
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
//            ----------------------------------------------------------------------

//            Jika gagal maka akan mengeluarkan dialog bahwa user sudah login hari ini
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
//            ----------------------------------------------------------------------
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    Fungsi yang menampilkan dialog dialog yang diperlukan saat melakukan proses login
    private void loginTaskDialog() {
        new AlertDialog.Builder(getContext())
//                Memberikan prompt pada user bahwa apakah benar identitas yang
//                terdeteksi adalah user yang melakukan login
                .setMessage("Username found. Are you " + username + " ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
//                        Lakukan proses konfirmasi bahwa user hanya login sekali
//                        hari ini
                        new confirmTask("yes").execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//                        Jika identitas yang terdeteksi bukanlah user
//                        maka socket akan di tutup dan mereset ulang
//                        proses pada server untuk dapat melakukan
//                        pengidentifikasian ulang
                        try {
                            dialogInterface.dismiss();
                            socket.close();

                            new AlertDialog.Builder(getContext())
                                    .setMessage("Please take your self picture again!")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    }).show();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).show();

    }
    //    -------------------------------------------------------------------------------

    //    Thread yang digunakan saat user hendak logout
    class sendLogoutThread implements Runnable {
        @Override
        public void run() {
            try {
//                Buka socket baru untuk mengirim logout
                InetAddress HOST = InetAddress.getByName(IPNUM);
                socket = new Socket(HOST, PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String data = "LOGOUT;" + preferences.getString("UserID", null);   //FORMAT: "LOGOUT;userId"
//                Ubah variable global username menjadi kosong untuk mengkosongkan data schedule pada
//                schedule fragment
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

    //    Async task yang digunakan jika user hendak mengirim face recognition request ke server
    class loginTask extends AsyncTask<Void, Void, Void> {

        byte[] bytes;
        ProgressDialog progressDialog;

        //        Mendapat input parameter "byte" yaitu gambar yang didapatkan
//        yang telah diubah menjadi bentuk byte array
        loginTask(byte[] bytes) {
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
        protected Void doInBackground(Void... voids) {
            try {
//                Membuka socket ke server
                socket = new Socket(IPNUM, PORT);

//                Mendecode byte array menjadi bitmap untuk dihitung
//                Lebar dan tinggi gambarnya
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Log.d("HEIGHT WIDTH", bitmap.getHeight() + " " + bitmap.getWidth());

//                ubah menjadi jpeg format
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] arrayByte = stream.toByteArray();

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
//                Mengirim tinggi, lebar dan size file gambar kepada server agar server dapat mengalokasikan
//                memory untuk dapat menerima gambar dan membuat kembali gambarnya di server dengan
//                tinggi dan lebar yang benar
                String data = "SIZE;" + bitmap.getWidth() + ";" + bitmap.getHeight() + ";" + arrayByte.length;
                dataOutputStream.flush();
                dataOutputStream.write(data.getBytes());
                dataOutputStream.flush();

//                 menunggu response ack dari server yang menandakan bahwa server
//                telah mengalokasikan memory untuk mendapatkan stream gambar
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
//                -------------------------------------------------------------------------
                dataOutputStream.flush();

//                Kirim stream gambar dalam bentuk array byte ke server
                dataOutputStream.write(arrayByte);
                dataOutputStream.flush();
//                ----------------------------------

//                Listen response login
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
//                --------------------------------------------------

                dataOutputStream.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            super.onPostExecute(voids);
            progressDialog.dismiss();
//            Buat sebuah dialog setelah proses login selesai
            loginTaskDialog();
        }
    }
//    --------------------------------------------------------------------------------------

    //    Proses pengiriman konfirmasi bahwa proses identifikasi benar
//    dan memastikan bahwa user hanya dapat check in absensi sekali sehari
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
//                Mengirim message "yes" ke server yang mengindikasikan bahwa
//                hasil pengidentifikasian benar
                dataOutputStream.write(message.getBytes());
                dataOutputStream.flush();

                StringBuilder stringBuffer = new StringBuilder();
                byte inputByte;
//                Listen response dari server
                while (true) {
                    while ((inputByte = dataInputStream.readByte()) != 0) {
                        stringBuffer.append((char) inputByte);
                    }
                    Log.d("StringBuffer", stringBuffer.toString());
//                    response SUCCESS mengindikasikan bahwa user telah melakukan proses check in
                    if (stringBuffer.toString().contains("SUCCESS")) {
                        Log.d("SUCCESS", "Received SUCCESS");
                        dataInputStream.close();
                        return true;
//                       response FAIL mengindikasikan bahwa user telah melakukan
//                        proses check in hari ini dan tidak dapat melakukannya lagi
                    } else if (stringBuffer.toString().contains("FAIL")) {
                        Log.d("FAIL", "Received FAIL");
                        dataInputStream.close();
                        return false;
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
            confirmTaskDialog(bool);
        }
    }
//    -----------------------------------------------------------------

    //    Meminta request permission camera
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

    //    Proses instansiasi untuk semua proses camera dan
//    Face detection
    private void createCameraSource() {

        Context context = getContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

//        Mengecek apakah dependency terinstall
        if (!detector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(getContext(), detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(15.0f)
                .build();

    }


    //    Jika user membuka kembali appsnya maka camera akan dinyalakan
    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    //    Jika user mempause apps/ menutupnya preview camera akan di stop
    @Override
    public void onPause() {
        super.onPause();
        mPreview.stop();
    }

    //    Jika apps di kill maka object camera preview akan di release
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

    //    Class yang membuat overlay graphic setiap wajah terdeteksi
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

        //        proses yang dilakukan jika sebuah wajah telah terdeteksi
        @Override
        public void onNewItem(int faceId, Face item) {
//           menambah count pendeteksian wajah
            faceNumber++;
            mFaceGraphic.setId(faceId);
        }

        //        Mengupdate posisi graphic overlay jika wajah berpindah posisi
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        //        Proses yang dilakukan jika wajah tertutup suatu barang/ tidak terlihat jelas
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        //        Proses yang dilakukan jika wajah benar benar tidak terdeteksi
        @Override
        public void onDone() {
//            Mengurangi count pendeteksian wajah
            faceNumber--;
            mOverlay.remove(mFaceGraphic);
        }
    }

}
