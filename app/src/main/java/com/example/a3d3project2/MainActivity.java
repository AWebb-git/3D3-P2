package com.example.a3d3project2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
public String Dest_IP = null;
public int Dest_Port;
public Socket socket;
public TextView response;
public TextView info;
public ListView list;
public String message;
public String Source_IP = null;
public int Source_Port = 1201;  //random port number
public String dir_IP = "192.168.1.25";
public int dir_port = 12000;
public ArrayList<String> dir_list;
public ArrayList<String> nodes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView)findViewById(R.id.textView);
        info = (TextView)findViewById(R.id.textView3);
        list = (ListView)findViewById(R.id.listview);


        Source_IP = getLocalIpAddress();
        info.setText(Source_IP);
        dir_list = new ArrayList<>();
        nodes = new ArrayList<>();
        nodes.add("null");

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nodes);
        list.setAdapter(adapter);

        new Thread(new RecThread()).start();
        dirConnect();

        list.setOnItemClickListener((parent, view, position, id) -> {
            Dest_IP = dir_list.get(position*2);
            Dest_Port = Integer.parseInt(dir_list.get(position*2+1));
        });
    }

    public void dirConnect(){
        String portMsg = String.valueOf(Source_Port);
        new Thread(new SendThread(portMsg, dir_IP, dir_port, 1)).start();
    }

    public void dirUpdate(View view){
        String updateMsg = "Update";
        new Thread(new SendThread(updateMsg, dir_IP, dir_port, 1)).start();
    }

    public void dirExit(View view){
        String portMsg = String.valueOf(Source_Port);
        new Thread(new SendThread(portMsg, dir_IP, dir_port, 0)).start();
        finishAndRemoveTask();
    }

    //runs on button click
    public void sendMsgBtn(View view){
        EditText msg = (EditText)findViewById(R.id.EditText);
        message = msg.getText().toString().trim();
        new Thread(new SendThread(message, Dest_IP, Dest_Port, 0)).start();
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
                    if(recMsg.equals("PING")){
                        output.write("ACK");
                        output.flush();
                        socket.shutdownOutput();
                    }
                    else{runOnUiThread(()-> response.setText(recMsg));}
                    socket.close();
                }

            } catch (IOException e) {
                runOnUiThread(()-> response.setText("server socket io err"));
            }
        }
    }

    class SendThread implements Runnable{
        private String message;
        private String dest_ip;
        private int dest_port;
        private int access_arr; //if one update nodes arraylist
        SendThread(String message, String dest_ip, int dest_port, int access_arr){
            this.message = message;
            this.dest_ip = dest_ip;
            this.dest_port = dest_port;
            this.access_arr = access_arr;
        }
        @Override
        public void run() {
            if(dest_ip == null){runOnUiThread(()-> response.setText("Choose destination"));}
            else {
                InetAddress destIP = null;
                PrintWriter outputSend;
                BufferedReader inputSend;
                try {
                    destIP = InetAddress.getByName(dest_ip);
                } catch (UnknownHostException e) {
                    runOnUiThread(()-> response.setText("Error ip"));
                }
                try {
                    socket = new Socket(destIP, dest_port);
                    outputSend = new PrintWriter(socket.getOutputStream());
                    inputSend = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outputSend.write(message);
                    outputSend.flush();
                    socket.shutdownOutput();
                    if (access_arr == 1) {    //if requesting connection or update from directory, update node list
                        String temp = inputSend.readLine();
                        dir_list.clear();
                        dir_list = new ArrayList<>(Arrays.asList(temp.split(" ")));
                        updateNodeList();
                    }
                } catch (IOException e) {
                    runOnUiThread(()-> response.setText("Error IO"));
                }
            }
        }
    }

    //update list of nodes used for displaying to listview
    public void updateNodeList(){
        nodes.clear();
        int x = 0;
        for(int i = 0; i < dir_list.size(); i+=2){
            nodes.add(dir_list.get(i) + " " + dir_list.get(i+1));
        }
        runOnUiThread(()->{
            ArrayAdapter adapter = (ArrayAdapter)list.getAdapter();
            adapter.addAll();
            adapter.notifyDataSetChanged();
        });
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
            runOnUiThread(()-> response.setText("ex"));
        }
        return null;
    }

}