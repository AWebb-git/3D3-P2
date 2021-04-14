# 3D3-P2
Simple app showing message recieving and sending over LAN
Requires entry of the destinations IP and Port

Instructions for testing app/data fabric:

REQUIREMENTS


SETUP

----------Have no Android Device---------------

1.If you have no Android device you must download Android Studio and open the project in it.
2.We will be runnning the app on Android Studio's built-in emulator, so we are required to redirect our device port to the emulator port.
3.Before you do anything, enter your pcs  IPv4 address that the python scripts will be hosted on, on line 163 in the MainActivity.java file.
4.Pressing the green run button in the top left to start the emulator, hover over the app on your task bar and you should see 
  something like : Android Emulator - Pixel 3a_API_30_x86:5554
  that last bit after the 'x86:' is what we want, it will almost always be 5554.
5.now that we have that we have to Telnet into it and do some port redirection.
  open a command prompt window and type 
  telnet localhost 5554
  where 5554 is that port number we just found. You should see a OK.
6.It should now be asking for your generated authorization token which is usually located in C:\Users\(yourusername)\.emulator_console_auth_token,
  it will tell you where it is in the cmd window though. Open up your file explorer and navigate to where it is, 
  open it in notepad and right click copy it ( for some reason CTRl V and CTRL C don't work properly)
7.Now into the cmd window type: auth 
  Right click paste your auth key in and press enter. It should then say something like Android Console: type help ...
8.Now that you're into the emulator we enter:
  redir add tcp:1201:1201
  And provided everything has been done correctly incoming connections on localhost to port 1201 will be redirected into our emulator on port 1201.
9.Now enter 127.0.0.1 as the ip address in the first page of the app on the emulator and move onto the TESTING section.

----------Have an Android Device---------------

1.You can either install the app using the APK or android studio

--APK--
1.Plug your phone into the computer, enable file transfer and drag and drop the x.apk file into a folder on the phone.
2.Open your phone's file manager, select the APK file and install.
--Android Studio--
1.Enable developer settings on your phone by repedeately pressing the build number field in your "About phone"/"About device" category in settings.
2.In developer settings enable usb debugging.
3.Download Android Studio

( https://developer.android.com/studio/run/emulator-networking#connecting )
