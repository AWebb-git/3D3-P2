package com.example.a3d3project2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
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
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.Destroyable;

public class MainActivity extends AppCompatActivity {
public String Dest_IP = null;
public int Dest_Port;
public TextView response;
public TextView device_ip;
public TextView info;
public ListView list;
public String message;
public int maxlength = 140; //max length of messages that can be sent
public String Source_IP = null;
public int Source_Port = 1201;
public String dir_IP  ;  //ENTER you own directory IP
public int dir_port = 1201;
public ArrayList<String> dir_list;  //array list containing [ip1,port1,ip2,port2,...]
public ArrayList<String> nodes;     //array list containing ["ip1 port1","ip2 port2",...] used for arrayadapter
public ServerSocket serverSocket;
public Map<String, String> keyPair;
public String recKey;
public int numRelays = 1; //how many relays are needed
ReentrantLock lock = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView)findViewById(R.id.textView);
        device_ip = (TextView)findViewById(R.id.textView3);
        info = (TextView)findViewById(R.id.textView4);
        list = (ListView)findViewById(R.id.listview);
        dir_IP =  getIntent().getStringExtra("DirectoryIP");
        Source_IP = getLocalIpAddress();

        device_ip.setText(Source_IP);
        dir_list = new ArrayList<>();
        dir_list.add(Source_IP);
        dir_list.add(Integer.toString(Source_Port));
        nodes = new ArrayList<>();

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nodes);
        list.setAdapter(adapter);
        updateNodeList();

        //generate keypair here
        try {
            keyPair = CryptoUtil.generateKeyPair();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        //remove unnecessary newlines
        String fixPubKey = keyPair.get("publicKey").replace("\n", "");
        String fixPriKey = keyPair.get("privateKey").replace("\n", "");
        keyPair.clear();
        keyPair.put("publicKey", fixPubKey);
        keyPair.put("privateKey", fixPriKey);

        new Thread(new RecThread()).start();    //thread for receiving messages
        dirConnect();   //connect to node specified in dirEntry page
        new Thread(new pingThread()).start();   //start checking availability of nodes in dir_list
        new Thread(new testThread()).start();
        //listener for on-screen list
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
            lock.lock();
            new Thread(new SendThread("Update", dir_list.get(i),
                    Integer.parseInt(dir_list.get(i+1)), 1)).start();
            lock.unlock();
        }
    }

    //tell all nodes in list to remove this client from their list, then quit activity
    public void dirExit(View view) throws IOException {
        for(int i = 0; i < dir_list.size(); i+=2) {
            lock.lock();
            new Thread(new SendThread("€QUIT:" + "1201", dir_list.get(i),
                    Integer.parseInt(dir_list.get(i+1)), 0)).start();
            lock.unlock();
        }
        serverSocket.close();   //close socket for receiving messages
        finishAndRemoveTask();
    }

    //runs on button click
    public void sendMsgBtn(View view){
        if(dir_list.size()<(4+2*numRelays)){runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Not enough relays",Toast.LENGTH_SHORT).show());}
        else {
            EditText msg = (EditText) findViewById(R.id.EditText);
            message = msg.getText().toString().trim();
            if(message.length() < maxlength){   //ensure message length is not too large
                String sendMsg = null;
                try {
                    sendMsg = msgConfig();
                } catch (GeneralSecurityException | InterruptedException e) {
                    Toast.makeText(getApplicationContext(), "Message Encryption Error", Toast.LENGTH_SHORT).show();
                }
                ArrayList<String> sendArr = msgSeperator(sendMsg);
                new Thread(new SendThread(sendArr.get(2), sendArr.get(0), Integer.parseInt(sendArr.get(1)), 0)).start();
            }
            else {
                runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error, message too long!!!",Toast.LENGTH_SHORT).show());
            }
        }
    }

    class  testThread implements Runnable{
        @Override
        public void run() {
            while(true){
                lock.lock();
                if(dir_list.size() >= (4+2*numRelays)){
                    lock.unlock();
                    break;
                }
                lock.unlock();
            }

            message = "€€€test€€€";
            Dest_IP = Source_IP;
            Dest_Port = Source_Port;
            String testMessage = null;
            try {
                testMessage = msgConfig();
            } catch (GeneralSecurityException | InterruptedException e) {
                Toast.makeText(getApplicationContext(), "Test Error", Toast.LENGTH_SHORT).show();
            }
            ArrayList<String> vals= msgSeperator(testMessage);
            //when there are enough nodes to send test message, send it
            new Thread(new SendThread(vals.get(2), vals.get(0), Integer.parseInt(vals.get(1)), 0)).start();
        }
    }

    //thread to check availability of current nodes in dir_list
    class pingThread implements Runnable{
        public int i = 0;
        @Override
        public void run() {
            while (true){
                try {
                    TimeUnit.SECONDS.sleep(1);
                    if(dir_list.size() > 0){    //iterate through list
                        lock.lock();
                        String pingPort = dir_list.get(((i*2)+1)%dir_list.size());
                        String pingIp = dir_list.get((i*2)%dir_list.size());
                        lock.unlock();
                        new Thread(new SendThread("PING", pingIp, Integer.parseInt(pingPort), 2)).start();
                    }
                } catch (InterruptedException e) {
                    runOnUiThread(()->Toast.makeText(getApplicationContext(),"Interrupted ping delay",Toast.LENGTH_SHORT).show());
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
                    else if(recMsg.equals("KEY")){
                        //send string value of this devices public key
                        output.write((String)keyPair.get("publicKey"));
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

                        output.write(listmsg());
                        output.flush();
                        socket.shutdownOutput();

                        lock.lock();
                        if(!dir_list.contains(newIP) || !dir_list.contains(newPort)){
                            dir_list.add(newIP);
                            dir_list.add(newPort);
                        }
                        lock.unlock();
                        updateNodeList();
                    }
                    else if(recMsg.equals("Update")){   //send node copy of this nodes list
                        output.write(listmsg());
                        output.flush();
                        socket.shutdownOutput();
                    }
                    else if(recVals.size() > 1){//relay
                        //decrypt recVals.get(0) and recVals.get(1) with this devices private key to get next destination
                        String ip = CryptoUtil.decrypt(recVals.get(0), (String)keyPair.get("privateKey"));
                        String port = CryptoUtil.decrypt(recVals.get(1), (String)keyPair.get("privateKey"));

                        runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Relaying",Toast.LENGTH_SHORT).show());
                        Socket relaySocket = new Socket(ip, Integer.parseInt(port));
                        PrintWriter relayOutput = new PrintWriter(relaySocket.getOutputStream());
                        int trashAmount = recVals.get(0).length() + recVals.get(1).length() + 2;
                        String nMsg = addTrash(recVals.get(2), trashAmount);
                        relayOutput.write(nMsg);
                        relayOutput.flush();
                        relaySocket.close();
                    }
                    else{   //decrypt and receive message
                        String finMsg = recMsg.split("£€%%")[0];    //remove trash added for fixed length
                        String finalMsg = CryptoUtil.decrypt(finMsg, (String)keyPair.get("privateKey"));

                        if(finalMsg.equals("€€€test€€€")){  //for test case
                            if(finalMsg.equals(finMsg)){
                                runOnUiThread(()->  Toast.makeText(getApplicationContext(),"test failed", Toast.LENGTH_LONG).show());}
                            else{
                                runOnUiThread(()->  Toast.makeText(getApplicationContext(),"test passed", Toast.LENGTH_LONG).show());}
                        }

                        else{runOnUiThread(()-> response.setText("Response: " +finalMsg));}

                    }
                    socket.close();
                }

            } catch (IOException e) {
                runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error receiving message",Toast.LENGTH_SHORT).show());
            } catch (GeneralSecurityException e) {
                runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error decrypting message",Toast.LENGTH_SHORT).show());
            }
        }
    }

    class SendThread implements Runnable{
        private String message;
        private String dest_ip;
        private int dest_port;
        private int access_arr; //if 1 update nodes arraylist, if 2 send ping, 3 recieve pubKey
        SendThread(String message, String dest_ip, int dest_port, int access_arr){
            this.message = message;
            this.dest_ip = dest_ip;
            this.dest_port = dest_port;
            this.access_arr = access_arr;
        }
        @Override
        public void run() {
            if(dest_ip == null){runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Choose destination",Toast.LENGTH_SHORT).show());}
            else {
                InetAddress destIP = null;

                //included due to unexpected null pointer error
                try {
                    destIP = InetAddress.getByName("127.0.0.1");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                PrintWriter outputSend;
                BufferedReader inputSend;
                try {
                    destIP = InetAddress.getByName(dest_ip);    //convert string to INET address
                } catch (UnknownHostException e) {
                    runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error getting INET addr",Toast.LENGTH_SHORT).show());
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
                    else if(access_arr == 3) {
                        //store public key you have received in a global variable
                        recKey = inputSend.readLine();
                    }
                } catch (IOException e) {
                    if(access_arr == 2){    //remove node
                        removeNode(dest_ip, Integer.toString(dest_port));
                        updateNodeList();   //tell arrayadapter (on-screen list) to change
                    }
                    else {runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error Sending a Message",Toast.LENGTH_SHORT).show());}
                }
            }
        }
    }

    //check received list for any new node
    public void addNewNodes(ArrayList<String> newNodes){
        for(int i = 0; i < newNodes.size(); i+=2){
            lock.lock();
            if(!dir_list.contains(newNodes.get(i)) || !dir_list.contains((newNodes.get(i+1)))){    //if a node in received list is new, add to our list
                dir_list.add(newNodes.get(i));
                dir_list.add(newNodes.get(i+1));
            }
            lock.unlock();
        }
    }

    //remove a node from our list
    public void removeNode(String IP, String port){
        for(int i = 0; i < dir_list.size(); i+=2){
            lock.lock();
            if(dir_list.get(i).equals(IP) && dir_list.get(i+1).equals(port)){
                dir_list.remove(i+1);
                dir_list.remove(i);
            }
            lock.unlock();
        }
    }

    //update list of nodes used for displaying to listview
    public void updateNodeList(){
        nodes.clear();
        int x = 0;
        lock.lock();
        for(int i = 0; i < dir_list.size(); i+=2){
            nodes.add(dir_list.get(i) + " " + dir_list.get(i+1));
        }
        lock.unlock();
        runOnUiThread(()->{
            ArrayAdapter adapter = (ArrayAdapter)list.getAdapter();
            adapter.addAll();
            adapter.notifyDataSetChanged();
        });
    }

    //create single string of dir_list values, seperated by " "
    public String listmsg(){    //create string of dir_list
        StringBuilder lMsg = new StringBuilder("");
        lock.lock();
        for(int i = 0; i < dir_list.size() - 1; i++){
            lMsg.append(dir_list.get(i) + " ");
        }
        lMsg.append(dir_list.get(dir_list.size() - 1));
        lock.unlock();
        return lMsg.toString();
    }

    //chooses relays to send message
    public String msgConfig() throws InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        View view = null;    //'fake' View to call dirUpdate
        dirUpdate(view);
        ArrayList<String> relays = new ArrayList<>();
        ArrayList<Integer>indexes = new ArrayList<>();
        for(int i = 1; i < dir_list.size(); i+=2){ indexes.add(i);}
        Collections.shuffle(indexes);
        int rNum;
        while(relays.size() < 2*numRelays){   //while 3 relays haven't been chosen
           rNum = indexes.remove(0);
           String posPort = dir_list.get(rNum);
           String posIP = dir_list.get(rNum - 1);

           if(!(posIP.equals(Source_IP)) && !(posIP.equals("127.0.0.1")) && !((posIP.equals(Dest_IP)))){
               if(relays.size() > 1){   //if not the first relay, encrypt with previous relays public key
                    //encrypt with received key
                    posIP = CryptoUtil.encrypt(posIP, recKey);
                    posPort = CryptoUtil.encrypt(posPort, recKey);
                    posIP = posIP.replace("\n", "");
                    posPort = posPort.replace("\n", "");
               }
               relays.add(posIP);
               relays.add(posPort);
               Thread getKEY = new Thread(new SendThread("KEY", posIP, Integer.parseInt(posPort), 3));
               getKEY.start(); //send message asking for key
               getKEY.join();  //wait for response
           }
           else if(!(posPort.equals(String.valueOf(Source_Port))) && !(posPort.equals(String.valueOf(Dest_Port)))){
               if(relays.size() > 1){   //if not the first relay, encrypt with previous relays public key
                    //encrypt with received key
                    posIP = CryptoUtil.encrypt(posIP, recKey);
                    posPort = CryptoUtil.encrypt(posPort, recKey);
                    posIP = posIP.replace("\n", "");
                    posPort = posPort.replace("\n", "");
                }
               relays.add(posIP);
               relays.add(posPort);
               Thread getKEY = new Thread(new SendThread("KEY", posIP, Integer.parseInt(posPort), 3));
               getKEY.start(); // send message asking for key
               getKEY.join();  // wait for response
           }
        }

        String encIP = CryptoUtil.encrypt(Dest_IP, recKey);
        String encPort = CryptoUtil.encrypt(String.valueOf(Dest_Port), recKey);
        encIP = encIP.replace("\n", "");
        encPort = encPort.replace("\n","");

        //get destination key
        Thread getKEY = new Thread(new SendThread("KEY", Dest_IP, Dest_Port, 3));
        getKEY.start(); // send message asking for key
        getKEY.join();  // wait for response

        //add destination address info
        relays.add(encIP);
        relays.add(encPort);

        message = CryptoUtil.encrypt(message, recKey);
        message = message.replace("\n","");
        relays.add(message);

        //concate into single string seperated by ;
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
    //used to get devices IPv4 address
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
            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"Error getting Local IP address",Toast.LENGTH_SHORT).show());
        }
        return null;
    }

}