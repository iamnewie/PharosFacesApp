package com.google.android.gms.samples.vision.face.facetracker;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
    String userId   = "";
    ImageButton cameraButton;
    ImageButton logoutButton;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        View cameraFragmentView = inflater.inflate(R.layout.main,null);

        cameraButton = (ImageButton) cameraFragmentView.findViewById(R.id.cameraButton);
        logoutButton = (ImageButton) cameraFragmentView.findViewById(R.id.logoutButton);

        sharedPreferences = getContext().getSharedPreferences("PHAROS",Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        return inflater.inflate(R.layout.loginlayout, container, false);
    }

    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            new AlertDialog.Builder(getContext())
                    .setMessage("Only use manual login in case you can't login via face recognition.")
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
        Button loginButton = (Button) getView().findViewById(R.id.login_button);
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
            String line = "";
            InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            // Response from server after login process will be stored in response variable.
            response = stringBuilder.toString();
            if(response.contains("success")){
                String array[] = response.split(";");
                userId = array[1];
                Toast.makeText(getContext(), "Login Success", Toast.LENGTH_SHORT).show();

            }else
            {
                // You can perform UI operations here
                Toast.makeText(getContext(), "Login " + response, Toast.LENGTH_SHORT).show();
            }
                inputStreamReader.close();
                reader.close();

            //kirim username ke activity
            if (!username.equals("")){

                ((MainActivity) getActivity()).setUsername(username);
                sharedPreferencesEditor.putBoolean("Logout", true);
                sharedPreferencesEditor.putBoolean("Camera", false);
                sharedPreferencesEditor.putString("UserID", userId);
                sharedPreferencesEditor.putString("Username",username);
                cameraButton.setEnabled(false);
                cameraButton.setClickable(false);
                logoutButton.setEnabled(true);
                logoutButton.setClickable(true);
                logoutButton.setImageResource(R.drawable.icon3_enable);
                sharedPreferencesEditor.commit();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
