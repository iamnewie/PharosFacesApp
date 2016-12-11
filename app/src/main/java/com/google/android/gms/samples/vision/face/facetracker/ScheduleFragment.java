package com.google.android.gms.samples.vision.face.facetracker;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Home on 12/10/2016.
 */
public class ScheduleFragment extends Fragment {
    String username = "";
    String fullName = "-";
    String staffId = "-";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        return inflater.inflate(R.layout.schedulelayout,container,false);
    }

    @Override
    public void onStart() {
        super.onStart();

        username = ((MainActivity)getActivity()).getUsername();
    }

    @Override
    public void onResume() {
        super.onResume();

        getSchedule();
        final TextView nameText = (TextView) getView().findViewById(R.id.name_text);
        final TextView staffIdText = (TextView) getView().findViewById(R.id.staffid_text);
        nameText.setText(fullName);
        staffIdText.setText(staffId);

    }

    protected void getSchedule(){
        HttpURLConnection connection;
        OutputStreamWriter request;

        URL url;
        String response;
        String parameters = "username="+username;

        try {
            url = new URL("http://192.168.1.108/pharosfaces/getschedule.php"); //URL buat login nya

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
                stringBuilder.append(line);
            }
            // Response from server after login process will be stored in response variable.
            response = stringBuilder.toString();

            String[] splitResponse = response.split(";");
            //staffId = splitResponse[0];
            //fullName = splitResponse[1];
            // You can perform UI operations here
            Toast.makeText(getContext(), response, Toast.LENGTH_SHORT).show();
            inputStreamReader.close();
            reader.close();


        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
