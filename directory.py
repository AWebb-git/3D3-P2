import socket
import threading
from threading import Thread
import time

serverPort = 12000
addrArr = []
arrLock = threading.Lock()

def printArr():
    print('\n')
    i = 0
    for x in addrArr:
        print(x, end=' ')
        if i%2:
            print()
        i+=1

def removeNode(port, IP):
    arrLock.acquire()
    occurences = [x for x, i in enumerate(addrArr) if i == port]    #find indexs where port occurs
    arrLock.release()
    x = 0
    while True:
        arrLock.acquire()
        if addrArr[occurences[x] - 1] == IP:   #if IP also matches delete IP and Port
            addrArr.pop(occurences[x])
            addrArr.pop(occurences[x]-1)
            printArr()
            arrLock.release()
            break
        x+=1
        arrLock.release()

#thread for listening to messages from clients on relay.py
def listen_thread():
    serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    serverSocket.bind(('',serverPort))
    serverSocket.listen(1)
    print('The server is ready to recieve')
    while True:
        connectionSocket, addr = serverSocket.accept()
        senderPort = connectionSocket.recv(1024).decode()
        
        if addrArr.count(senderPort) == 0:  
            if (senderPort != 'Update'):    #add to list
                arrLock.acquire()
                addrArr.append(addr[0])
                addrArr.append(senderPort)
                arrLock.release()
                print(addr[0] + ' ' + senderPort)

            msg = ''     #send message w/ list of nodes
            for x in addrArr:
                msg+=x + ' '
            connectionSocket.send(msg.encode())
        else:
            removeNode(senderPort, addr[0])
        connectionSocket.close()

#thread to send pings to check availibility of nodes in directory
def ping_thread():
    i = 0
    while True:
        time.sleep(1)   #delay to avoid network congestion at relays
        arrLock.acquire()
        if len(addrArr) > 0:
            pingSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            pingSocket.settimeout(10)   #setting timeout for sending/recieving messages
            pingPort = int(addrArr[((i*2)+1)%len(addrArr)])
            pingIP = addrArr[(i*2)%len(addrArr)]
            arrLock.release()
            try:    #try send ping
                pingSocket.connect((pingIP,pingPort))
                pingSocket.send('PING\n'.encode())
                ACK = pingSocket.recv(1024).decode()
            except (socket.timeout, ConnectionRefusedError):  #if timeout connecting/sending/recieving, remove from directory
                print('Lost Node: ' + pingIP + ' ', pingPort)
                removeNode(str(pingPort), pingIP)
            pingSocket.close()
        else:
            arrLock.release()    
        i+=1

#'main'
th1 = Thread(target=listen_thread, args=())
th2 = Thread(target=ping_thread, args=())
th1.start()
th2.start()
th1.join()
th2.join()