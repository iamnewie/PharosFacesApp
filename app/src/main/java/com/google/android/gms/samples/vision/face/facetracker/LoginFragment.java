package com.google.android.gms.samples.vision.face.facetracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Home on 12/10/2016.
 */
public class LoginFragment extends Fragment {

    String username = "";
    String password = "";
    String userId = "";
    ImageButton cameraButton;
    ImageButton logoutButton;
    Button loginButton;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        sharedPreferences = getContext().getSharedPreferences("PHAROS", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        return inflater.inflate(R.layout.loginlayout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        cameraButton = (ImageButton) getActivity().findViewById(R.id.cameraButton);
        logoutButton = (ImageButton) getActivity().findViewById(R.id.logoutButton);
    }

//    Muncul saat user pindah ke LoginFragment
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            new AlertDialog.Builder(getContext())
                    .setMessage("Use manual login in case you can't login via face recognition.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final EditText usernameEdit = (EditText) getView().findViewById(R.id.username_edit);
        final EditText passwordEdit = (EditText) getView().findViewById(R.id.password_edit);
        loginButton = (Button) getView().findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                username = usernameEdit.getText().toString();
                password = passwordEdit.getText().toString();

                //coba login
                tryLogin(username, password);
            }
        });
    }

    protected void tryLogin(String username, String password) {
        HttpURLConnection connection;
        OutputStreamWriter request;

        URL url = null;
        String response = null;
        String parameters = "username=" + username + "&password=" + password;

        try {
            url = new URL("http://192.168.0.3/pharosfaces/memberlogin.php"); //URL buat login nya

            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("POST");

            request = new OutputStreamWriter(connection.getOutputStream());
            request.write(parameters);
            request.flush();
            request.close();

            // Menerima respon dari server
            String line;
            InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            // Memproses response dari server
            response = stringBuilder.toString();
            if (response.contains("success")) {
                String array[] = response.split(";");
                userId = array[1];

                //Tampilkan login success
                new AlertDialog.Builder(getContext())
                        .setMessage("Login success")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();

                if (!username.equals("")) {
                    //kirim username ke MainActivity
                    ((MainActivity) getActivity()).setUsername(username);

                    loginButton.setEnabled(false);
                    //Atur sharedprefereces
                    sharedPreferencesEditor.putBoolean("Logout", true);
                    sharedPreferencesEditor.putBoolean("Camera", false);
                    sharedPreferencesEditor.putString("UserID", userId);
                    sharedPreferencesEditor.putString("Username", username);
                    cameraButton.setEnabled(false);
                    cameraButton.setClickable(false);
                    logoutButton.setEnabled(true);
                    logoutButton.setClickable(true);
                    logoutButton.setImageResource(R.drawable.icon3_enable);
                    sharedPreferencesEditor.commit();
                }
            }
            else if(response.contains("fail")){
                //Tampilkan login fail
                new AlertDialog.Builder(getContext())
                        .setMessage("Login failed")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
            }
            else {
                //Kalo sudah login hari ini
                new AlertDialog.Builder(getContext())
                        .setMessage("You have logged in today")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
            }
            inputStreamReader.close();
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
