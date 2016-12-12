package com.google.android.gms.samples.vision.face.facetracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Home on 12/10/2016.
 */
public class ScheduleFragment extends Fragment {
    String username = "";
    String fullName = "-";
    String staffId = "-";
    String image = "-";
    ArrayList<String> dateList;
    ImageView imageView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        return inflater.inflate(R.layout.schedulelayout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        imageView = (ImageView) getView().findViewById(R.id.profile_image);
        username = ((MainActivity) getActivity()).getUsername();
    }

    @Override
    public void onResume() {
        super.onResume();
        dateList = new ArrayList<String>();
        if (!username.equals("")) {   //jalanin kalo username ada isinya
            //ambil info
            getSchedule();
            TextView nameText = (TextView) getView().findViewById(R.id.name_text);
            TextView staffIdText = (TextView) getView().findViewById(R.id.staffid_text);
            nameText.setText(fullName);
            staffIdText.setText(staffId);

            //masukin ke list view
            ListView dateView = (ListView) getView().findViewById(R.id.date_view);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dateList);
            dateView.setAdapter(adapter);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            username = ((MainActivity) getActivity()).getUsername();
            if (username.equals("")) {

                new AlertDialog.Builder(getContext())
                        .setMessage("Login terlebih dahulu untuk dapat melihat profile dan schedule history")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            } else {
                //ambil info
                getSchedule();
                TextView nameText = (TextView) getView().findViewById(R.id.name_text);
                TextView staffIdText = (TextView) getView().findViewById(R.id.staffid_text);
                nameText.setText(fullName);
                staffIdText.setText(staffId);

                //masukin ke list view
                ListView dateView = (ListView) getView().findViewById(R.id.date_view);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dateList);
                dateView.setAdapter(adapter);
            }
        }
    }

    protected void getSchedule() {
        HttpURLConnection connection;
        OutputStreamWriter request;

        URL url;
        String response;
        String parameters = "username=" + username;

        try {
            url = new URL("http://192.168.0.3/pharosfaces/getschedule.php"); //URL buat login nya

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

            String[] splitResponse = response.split(";"); //FORMAT: id;name;image;date;date;date;date;dst
            staffId = splitResponse[0];
            fullName = splitResponse[1];
            image = splitResponse[2];
            Log.d("Image", image);
            for (int i = 3; i < splitResponse.length - 1; i++) {
                dateList.add(splitResponse[i]);
            }
            new getImage(imageView, image).execute();

            // You can perform UI operations here

            inputStreamReader.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class getImage extends AsyncTask<Void, Void, Bitmap> {

        ImageView imageView;
        String imagename;

        getImage(ImageView imageView, String imagename) {
            this.imageView = imageView;
            this.imagename = imagename;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap profilepic = null;
            try {
                URL url = new URL("http://192.168.0.3/pharosfaces/images/" + imagename);
                InputStream inputStream = url.openStream();
                profilepic = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return profilepic;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            imageView.setImageBitmap(bitmap);
        }
    }
}
