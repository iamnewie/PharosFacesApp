package com.google.android.gms.samples.vision.face.facetracker;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    //private ProgressDialog progressDialog;
    String username;
    String password;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        return inflater.inflate(R.layout.loginlayout,container,false);
    }

    @Override
    public void onResume() {
        super.onResume();
        new AlertDialog.Builder(getContext())
                .setMessage("Only use manual login in case you can't login via face recognition.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @Override
    public void onStart() {
        super.onStart();

        //warning-nya login manual

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

    protected void tryLogin(String username, String password){
        HttpURLConnection connection;
        OutputStreamWriter request;

        URL url = null;
        String response = null;
        String parameters = "username="+username+"&password="+password;

        try {
            url = new URL("http://192.168.1.108/pharosfaces/memberlogin.php"); //URL buat login nya

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
            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line + "\n");
            }
            // Response from server after login process will be stored in response variable.
            response = stringBuilder.toString();
            // You can perform UI operations here
            Toast.makeText(getContext(),"Message from Server: \nLogin "+ response, Toast.LENGTH_SHORT).show();
            inputStreamReader.close();
            reader.close();

        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    /*
    class Login extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Please wait..");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(true);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {


            return null;
        }
    }*/
}
