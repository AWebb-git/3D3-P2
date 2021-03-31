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
public static final String Dest_IP = "Your IP";
public static final int Dest_Port = 1201;  //ENTER whatever your dest port is
public Socket socket;
public TextView response;
public TextView info;
public String message;
public String Source_IP = "null";
public int Source_Port = 1201;  //random port number
public String dir_IP = "YOUR Directory IP";
public int dir_port = 12000;
public String list = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView)findViewById(R.id.textView);
        info = (TextView)findViewById(R.id.textView3);
        Source_IP = getLocalIpAddress();
        info.setText(Source_IP);
        new Thread(new RecThread()).start();
        dirConnect();
    }

    public void dirConnect(){
        String portMsg = String.valueOf(Source_Port);
        new Thread(new SendThread(portMsg, dir_IP, dir_port)).start();
    }

    //runs on button click
    public void sendMsgBtn(View view){
        EditText msg = (EditText)findViewById(R.id.EditText);
        message = msg.getText().toString().trim();
        new Thread(new SendThread(message, Dest_IP, Dest_Port)).start();
    }

    class RecThread implements Runnable{
        @Override
        public void run() {
            BufferedReader input;
            PrintWriter output;
            try{
                ServerSocket serverSocket = new ServerSocket(Source_Port);
                while(true) {
                    Socket socket = serverSocket.accept();
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    output = new PrintWriter(socket.getOutputStream());
                    final String recMsg = input.readLine();
                    runOnUiThread(()->{response.setText(recMsg);});
                    if(recMsg.equals("PING")){
                        output.write("ACK");
                        output.flush();
                        socket.shutdownOutput();
                    }
                    socket.close();
                }

            } catch (IOException e) {
                response.setText("server socket io err");
            }
        }
    }

    class SendThread implements Runnable{
        private String message;
        private String dest_ip;
        private int dest_port;
        SendThread(String message, String dest_ip, int dest_port){
            this.message = message;
            this.dest_ip = dest_ip;
            this.dest_port = dest_port;
        }
        @Override
        public void run() {
            InetAddress destIP = null;
            PrintWriter outputSend = null;
            BufferedReader inputSend = null;
            try {
                destIP = InetAddress.getByName(dest_ip);
            } catch (UnknownHostException e) {
                response.setText("Error ip");
            }
            try {
                socket = new Socket(destIP, dest_port);
                outputSend = new PrintWriter(socket.getOutputStream());
                inputSend = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputSend.write(message);
                outputSend.flush();
                socket.shutdownOutput();
                list = inputSend.readLine();
            } catch (IOException e) {
                response.setText("Error IO");
            }
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