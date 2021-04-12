package com.example.a3d3project2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {
public String Dest_IP = null;
public int Dest_Port;
public TextView response;
public TextView info;
public ListView list;
public String message;
public String Source_IP = null;
public int Source_Port = 1201;
public String dir_IP  ;  //ENTER you own directory IP
public int dir_port = 1201;
public ArrayList<String> dir_list;  //array list containing [ip1,port1,ip2,port2,...]
public ArrayList<String> nodes;     //array list containing ["ip1 port1","ip2 port2",...] used for arrayadapter
public ServerSocket serverSocket;
public Map keyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView)findViewById(R.id.textView);
        info = (TextView)findViewById(R.id.textView3);
        list = (ListView)findViewById(R.id.listview);
        dir_IP =  getIntent().getStringExtra("DirectoryIP");
        Source_IP = getLocalIpAddress();
        info.setText(Source_IP);
        dir_list = new ArrayList<>();
        dir_list.add(Source_IP);
        dir_list.add(Integer.toString(Source_Port));
        nodes = new ArrayList<>();

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nodes);
        list.setAdapter(adapter);
        updateNodeList();

        new Thread(new RecThread()).start();    //thread for receiving messages
        dirConnect();   //connect to node specified in dirEntry page
        new Thread(new pingThread()).start();   //start checking availability of nodes in dir_list

        //crypto util call example
        /*
        try {
           Map<String, String> keyexample = new HashMap<>();
           keyexample = CryptoUtil.generateKeyPair();
           String s = keyexample.get("publickey");
           info.setText(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        */

        list.setOnItemClickListener((parent, view, position, id) -> {
            Dest_IP = dir_list.get(position*2);
            Dest_Port = Integer.parseInt(dir_list.get(position*2+1));
        });
    }

    //send initial connection request to specified node
    public void dirConnect(){
        String portMsg = "€PORT:" + String.valueOf(Source_Port);
        new Thread(new SendThread(portMsg, dir_IP, dir_port, 1)).start();
    }

    //ask for updates of available nodes from all current nodes in dir_list
    public void dirUpdate(View view){
        for(int i = 0; i < dir_list.size(); i+=2) {
            new Thread(new SendThread("Update", dir_list.get(i),
                    Integer.parseInt(dir_list.get(i+1)), 1)).start();
        }
    }

    //tell all nodes in list to remove this client from their list, then quit activity
    public void dirExit(View view) throws IOException {
        for(int i = 0; i < dir_list.size(); i+=2) {
            new Thread(new SendThread("€QUIT:" + "1201", dir_list.get(i),
                    Integer.parseInt(dir_list.get(i+1)), 0)).start();
        }
        serverSocket.close();   //close socket for receiving messages
        finishAndRemoveTask();
    }

    //runs on button click
    public void sendMsgBtn(View view){
        if(dir_list.size()<6){runOnUiThread(()-> response.setText("Not enough relays"));}
        else {
            EditText msg = (EditText) findViewById(R.id.EditText);
            message = msg.getText().toString().trim();
            String sendMsg = msgConfig();
            ArrayList<String> sendArr = msgSeperator(sendMsg);
            new Thread(new SendThread(sendArr.get(2), sendArr.get(0), Integer.parseInt(sendArr.get(1)), 0)).start();
        }
    }

    //thread to check availability of current nodes in dir_list
    class pingThread implements Runnable{
        public int i = 0;
        @Override
        public void run() {
            while (true){
                try {
                    TimeUnit.SECONDS.sleep(3); //could decrease to microseconds ***deal with int i overflow then
                    if(dir_list.size() > 0){    //iterate through list
                        String pingIp = dir_list.get((i*2)%dir_list.size());
                        String pingPort = dir_list.get(((i*2)+1)%dir_list.size());
                        new Thread(new SendThread("PING", pingIp, Integer.parseInt(pingPort), 2)).start();
                    }
                } catch (InterruptedException e) {
                    runOnUiThread(()->response.setText("Interrupted ping delay"));
                }
                i++;
            }
        }
    }

    class RecThread implements Runnable{
        @Override
        public void run() {
            BufferedReader input;
            PrintWriter output;
            try{
                serverSocket = new ServerSocket(Source_Port);
                while(true) {
                    Socket socket = serverSocket.accept();
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    output = new PrintWriter(socket.getOutputStream());
                    final String recMsg = input.readLine();
                    ArrayList<String> recVals = msgSeperator(recMsg);
                    if(recMsg.equals("PING")){  //respond to ping
                        output.write("ACK");
                        output.flush();
                        socket.shutdownOutput();
                    }
                    else if(recMsg.startsWith("€QUIT:")){ //remove node from this nodes list
                        String newIP = socket.getInetAddress().toString().split("/")[1];
                        removeNode(newIP, recMsg.split("€QUIT:")[1]);
                        updateNodeList();
                    }
                    else if(recMsg.startsWith("€PORT:")){   //add new node to this nodes list
                        String newPort = recMsg.split("€PORT:")[1];
                        String newIP = socket.getInetAddress().toString().split("/")[1];
                        if (newIP.equals("10.0.2.2")){
                            newIP = "192.168.192.19"; // <- your ip here
                        }
                        output.write(listmsg());
                        output.flush();
                        socket.shutdownOutput();

                        if(!dir_list.contains(newIP) || !dir_list.contains(newPort)){
                            dir_list.add(newIP);
                            dir_list.add(newPort);
                        }
                        updateNodeList();
                    }
                    else if(recMsg.equals("Update")){   //send node copy of this nodes list
                        output.write(listmsg());
                        output.flush();
                        socket.shutdownOutput();
                    }
                    else if(recVals.size() > 1){//relay
                        runOnUiThread(()-> response.setText("Relaying"));
                        Socket relaySocket = new Socket(recVals.get(0), Integer.parseInt(recVals.get(1)));
                        PrintWriter relayOutput = new PrintWriter(relaySocket.getOutputStream());
                        int trashAmount = recVals.get(0).length() + recVals.get(1).length() + 2;
                        String nMsg = addTrash(recVals.get(2), trashAmount);
                        relayOutput.write(nMsg);
                        relayOutput.flush();
                        relaySocket.close();
                    }
                    else{   //receive message
                        String finMsg = recMsg.split("£€%%")[0];
                        runOnUiThread(()-> response.setText(finMsg));
                    }
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
        private int access_arr; //if 1 update nodes arraylist, if 2 send ping
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
                    destIP = InetAddress.getByName(dest_ip);    //convert string to INET address
                } catch (UnknownHostException e) {
                    runOnUiThread(()-> response.setText("Error ip"));
                }
                try {
                    Socket socket = new Socket(destIP, dest_port);
                    if(access_arr == 2){socket.setSoTimeout(2*10000);}  //set timeout to receive ping ack
                    outputSend = new PrintWriter(socket.getOutputStream());
                    inputSend = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outputSend.write(message);
                    outputSend.flush();
                    socket.shutdownOutput();
                    if (access_arr == 1) {    //if requesting connection or update from directory, update node list
                        String temp = inputSend.readLine();
                        ArrayList<String>  newList = new ArrayList<>(Arrays.asList(temp.split(" ")));
                        addNewNodes(newList);
                        updateNodeList();
                    }
                    else if (access_arr == 2){
                        String ACK = inputSend.readLine(); //could implement further ACK handling?
                    }
                } catch (IOException e) {
                    if(access_arr == 2){    //remove node
                        removeNode(dest_ip, Integer.toString(dest_port));
                        updateNodeList();   //tell arrayadapter (on-screen list) to change
                    }
                    else{runOnUiThread(()-> response.setText("Error IO"));}
                }
            }
        }
    }

    //check received list for any new node
    public void addNewNodes(ArrayList<String> newNodes){
        for(int i = 0; i < newNodes.size(); i+=2){
            if(!dir_list.contains(newNodes.get(i)) || !dir_list.contains((newNodes.get(i+1)))){    //if a node in received list is new, add to our list
                dir_list.add(newNodes.get(i));
                dir_list.add(newNodes.get(i+1));
            }
        }
    }

    //remove a node from our list
    public void removeNode(String IP, String port){
        for(int i = 0; i < dir_list.size(); i+=2){
            if(dir_list.get(i).equals(IP) && dir_list.get(i+1).equals(port)){
                dir_list.remove(i+1);
                dir_list.remove(i);
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

    public String listmsg(){    //create string of dir_list
        StringBuilder lMsg = new StringBuilder("");
        for(int i = 0; i < dir_list.size() - 1; i++){
            lMsg.append(dir_list.get(i) + " ");
        }
        lMsg.append(dir_list.get(dir_list.size() - 1));
        return lMsg.toString();
    }

    public String msgConfig(){
        View view = null;    //dummy View to call dirUpdate
        dirUpdate(view);
        ArrayList<String> relays = new ArrayList<>();
        ArrayList<Integer>indexes = new ArrayList<>();
        for(int i = 1; i < dir_list.size(); i+=2){ indexes.add(i);}
        Collections.shuffle(indexes);
        int rNum;
        while(relays.size() < 2){   //while 1 relay hasn't been chosen
           rNum = indexes.remove(0);
           String posPort = dir_list.get(rNum);
           String posIP = dir_list.get(rNum - 1);

           if(!(posIP.equals(Source_IP)) && !(posIP.equals("127.0.0.1")) && !((posIP.equals(Dest_IP)))){
               relays.add(posIP);
               relays.add(posPort);
           }
           else if(!(posPort.equals(String.valueOf(Source_Port))) && !(posPort.equals(String.valueOf(Dest_Port)))){
               relays.add(posIP);
               relays.add(posPort);
           }
        }
        relays.add(Dest_IP);
        relays.add(String.valueOf(Dest_Port));
        relays.add(message);

        String finMsg = "";
        for(int i = 0; i < relays.size() - 1; i++){
            finMsg += relays.get(i) + ";";
        }

        finMsg += relays.get(relays.size() - 1);

        return finMsg;
    }

    //seperates a list message into a list
    public ArrayList<String> msgSeperator(String msg){
        String cells[] = msg.split(";");
        ArrayList<String> rVals= new ArrayList<>();
        if(cells.length > 1){
            String nMsg  = "";
            for(int i = 2; i < cells.length - 1; i++){nMsg+=cells[i] + ";";}    //recombine
            nMsg += cells[cells.length - 1];
            rVals.add(cells[0]);
            rVals.add(cells[1]);
            rVals.add(nMsg);
            return rVals;
        }
        else{
            rVals.add(msg);
            return rVals;
        }
    }

    //for keeping messages a fixed length between relays
    public String addTrash(String msg, int amount){
        StringBuilder nMsg = new StringBuilder(msg);
        nMsg.append("£€%%");
        for(int i = 0; i < amount - 4; i++){ nMsg.append('0');}
        return nMsg.toString();
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