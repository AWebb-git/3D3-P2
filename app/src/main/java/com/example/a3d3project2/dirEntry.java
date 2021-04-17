package com.example.a3d3project2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

//Page for entering IP of initial node to connect to
public class dirEntry extends AppCompatActivity {
    public TextView display;
    public TextView ipdisp;
    public EditText input;
    public Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir_entry);
        ipdisp = findViewById(R.id.localipadd);

        display = findViewById(R.id.textView2);
        input = findViewById(R.id.direntry);
        submit = findViewById(R.id.button4);

        runOnUiThread(()-> ipdisp.setText(getLocalIpAddress()));
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Dir_IP = getText();
                if (Dir_IP.length() < 9) {
                    runOnUiThread(()-> display.setText("ip too small"));
                    return;
                }
                InetAddress IP = null;
                PrintWriter outputSend;
                BufferedReader inputSend;
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.putExtra("DirectoryIP", Dir_IP);
                startActivity(intent);
            }
        });
    }
    private String getText(){
        String Dir_IP = input.getText().toString().trim();
        return Dir_IP;
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error Retrieving LocalIP address",Toast.LENGTH_SHORT).show());
        }
        return null;
    }
}