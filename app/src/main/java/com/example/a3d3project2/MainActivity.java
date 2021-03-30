package com.example.a3d3project2;

import androidx.appcompat.app.AppCompatActivity;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
public static final String Dest_IP = "YOUR_DEST_IP";
public static final int Dest_Port = 1201;  //ENTER whatever your dest port is
public Socket socket;
public TextView response;
public TextView info;
public String message;
public String Source_IP = "null";
public int Source_Port = 1201;  //random port number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView)findViewById(R.id.textView);
        info = (TextView)findViewById(R.id.textView3);
        Source_IP = getLocalIpAddress();
        info.setText(Source_IP);
        new Thread(new RecThread()).start();
    }

    //runs on button click
    public void sendMsgBtn(View view){
        EditText msg = (EditText)findViewById(R.id.EditText);
        message = msg.getText().toString().trim();
        new Thread(new SendThread()).start();
    }

    class RecThread implements Runnable{
        @Override
        public void run() {
            BufferedReader input;
            try{
                ServerSocket serverSocket = new ServerSocket(Source_Port);
                while(true) {
                    Socket socket = serverSocket.accept();
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final String recMsg = input.readLine();
                    runOnUiThread(()->{response.setText(recMsg);});
                    socket.close();
                }

            } catch (IOException e) {
                response.setText("server socket io err");
            }
        }
    }

    class SendThread implements Runnable{
        @Override
        public void run() {
            InetAddress destIP = null;
            PrintWriter outputSend = null;
            try {
                destIP = InetAddress.getByName(Dest_IP);
            } catch (UnknownHostException e) {
                response.setText("Error ip");
            }
            try {
                socket = new Socket(destIP,Dest_Port);
                outputSend = new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                response.setText("Error IO");
            }
            outputSend.write(message);
            outputSend.println();
            outputSend.flush();
        }
    }


    //gotten from https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
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
            response.setText("ex");
        }
        return null;
    }
}