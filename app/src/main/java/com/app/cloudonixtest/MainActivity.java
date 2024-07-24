package com.app.cloudonixtest;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.cloudonixtest.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView tvIpAddress;
    private ProgressBar progressBar;
    private TextView tvPleaseWait;
    private ImageView ivResult;
    private Button btnGetIp;

    // Used to load the 'cloudonixtest' library on application startup.
    static {
        System.loadLibrary("cloudonixtest");
    }

    public native String getIPAddress();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvIpAddress = findViewById(R.id.tv_ip_address);
        progressBar = findViewById(R.id.progress_bar);
        tvPleaseWait = findViewById(R.id.tv_please_wait);
        ivResult = findViewById(R.id.iv_result);
        btnGetIp = findViewById(R.id.btn_get_ip);

        btnGetIp.setOnClickListener(v -> {
            // Call the native method and handle the result
            String ipAddress = getIPAddress();
            tvIpAddress.setText("IP Address: " + ipAddress);
            sendIPAddressToServer(ipAddress);
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void sendIPAddressToServer(String ipAddress) {
        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
                tvPleaseWait.setVisibility(View.VISIBLE);
                ivResult.setVisibility(View.GONE);
            }

            @Override
            protected JSONObject doInBackground(String... params) {
                String ipAddress = params[0];
                try {
                    URL url = new URL("https://s7om3fdgbt7lcvqdnxitjmtiim0uczux.lambda-url.us-east-2.on.aws/");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setConnectTimeout(3000); // 3 seconds timeout
                    connection.setDoOutput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("address", ipAddress);

                    OutputStream os = connection.getOutputStream();
                    os.write(jsonParam.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("HTTP", "OK");
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        return new JSONObject(response.toString());
                    } else {
                        Log.d("HTTP", "ERROR");
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);
                progressBar.setVisibility(View.GONE);
                tvPleaseWait.setVisibility(View.GONE);
                ivResult.setVisibility(View.VISIBLE);
                if (result != null) {
                    try {
                        boolean isNat = result.getBoolean("nat");
                        if (isNat) {
                            ivResult.setImageResource(android.R.drawable.presence_online); // green OK icon
                        } else {
                            ivResult.setImageResource(android.R.drawable.presence_busy); // red not OK icon
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ivResult.setImageResource(android.R.drawable.presence_offline); // error icon
                    }
                } else {
                    Log.d("Result","Null result");
                    ivResult.setImageResource(android.R.drawable.presence_offline); // error icon
                }
            }
        }.execute(ipAddress);
    }
}