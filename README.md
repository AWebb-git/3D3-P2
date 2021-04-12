# 3D3-P2
Simple app showing message recieving and sending over LAN
Requires entry of the destinations IP and Port
Wasnt able to test if recieving messages works on set-up emulator, but it works on phones
Also can recieve and send messages to other socket programmes such as the simple client and server.py scripts in the first commit of 3D3-group-work repo



If you do not have access to an Android phone for testing this, that is very unfortunate, and I would strongly recommend you borrow one for this testing instead,
as the emulator has a lengthy process for set up .
If not:     (use emu branch)

0. Before you do anything, enter your pcs  ip that the python scripts will be hosted on, on line 174 in the MainActivity.java file.

1. Start the app in the emulator. Then hover over the app on your task bar and you should see 
something like : Android Emulator - Pixel 3a_API_30_x86:5554
that last bit after the x86: is what we want, it will almost always be 5554.

2. now that we have that we have to Telnet into it and do some port redirection.
open a command prompt window and type 
telnet localhost 5554
where 5554 is that port no we just found.
you should see a OK.

3. It should now be asking for your generated authorization token which is usually located in C:\Users\(yourusername)\.emulator_console_auth_token
it will tell you where it is in the cmd window though.
open up your file explorer and navigate to where it is, open it in notepad and right click copy it ( for some reason CTRl V and CTRL C don't work properly)

4. now into the cmd window type: auth 
right click paste your auth key in and press enter( you can always manually enter the key in if you want)
it should then say something like Android Console: type help ...

5. now that you're (hopefully) in to the emulator we type 
redir add tcp:1201:1201
and press enter
And provided everything has been done correctly incoming connections on localhost to port 1201 will be redirected into our emulator on port 1201.

6. you are now free to start testing the app, by entering the displayed local ip of the emulator into it as the directory, and running two relay scripts with their directory address set to localhost(127.0.0.1) and any two unique port numbers above 1023 (except 1201, as this is the directory port number).

7. The list of available nodes should be more or less up to date on all the displays now,
and you are free to send msgs to other nodes as you wish.


( https://developer.android.com/studio/run/emulator-networking#connecting )