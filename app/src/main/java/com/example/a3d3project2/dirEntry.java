package com.example.a3d3project2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.CryptoUtil.CryptoUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Timer;

public class dirEntry extends AppCompatActivity {
    public TextView display;
    public TextView ipdisp;
    public TextView example;
    public TextView encrypted;
    public TextView decrypted;
    public EditText input;
    public Button submit;
    public Map keyPair;
    public Socket socket;
    public String Source_Port = "1201";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir_entry);
        ipdisp = findViewById(R.id.localipadd);
        example = findViewById(R.id.example);
        encrypted = findViewById(R.id.encrypted);
        decrypted = findViewById(R.id.decrypted);
        display = findViewById(R.id.textView2);
        input = findViewById(R.id.direntry);
        submit = findViewById(R.id.button4);
        /*
        String temp1; // un comment for basic encryption test
        String temp2;

        try {
            keyPair = CryptoUtil.generateKeyPair();
        }
        catch(Exception e){
        }
        String publicKey = (String)keyPair.get("publicKey");
        String privateKey = (String)keyPair.get("privateKey");
        try {
            String test = example.getText().toString();
            temp1 = CryptoUtil.encrypt(test , publicKey);
            temp2 = CryptoUtil.decrypt(temp1, privateKey);
        }catch (Exception e){
            runOnUiThread(()-> display.setText(e.toString()));
            temp1 = "u";
            temp2 = "h";
        }


        encrypted.setText(temp1);
        decrypted.setText(temp2);

         */

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
        runOnUiThread(()-> display.setText(Dir_IP));
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
            display.setText("ex");
            runOnUiThread(()-> display.setText("ex"));
        }
        return null;
    }
}