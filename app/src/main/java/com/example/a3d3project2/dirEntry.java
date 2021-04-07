package com.example.a3d3project2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;

public class dirEntry extends AppCompatActivity {
    public TextView display;
    public EditText input;
    public Button submit;
    public Socket socket;
    public String Source_Port = "1201";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir_entry);
        display = findViewById(R.id.textView2);
        input = findViewById(R.id.direntry);
        submit = findViewById(R.id.button4);
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
                /*try {
                   IP = InetAddress.getByName(Dir_IP);
                } catch (UnknownHostException e) {
                    runOnUiThread(()-> display.setText("Error ip"));
                    return;
                }
                try {
                    socket = new Socket(IP, 12000);
                    outputSend = new PrintWriter(socket.getOutputStream());
                    inputSend = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outputSend.write(Source_Port);
                    outputSend.flush();
                    socket.shutdownOutput();
                    String temp = inputSend.readLine();
                    runOnUiThread(()-> display.setText(temp));
                }catch (Exception e){
                    runOnUiThread(()-> display.setText("directory connect test failed"));
                    return;

                }*/
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
}