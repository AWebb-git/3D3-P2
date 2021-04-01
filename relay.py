import socket
import random
import threading
from threading import Thread


printLock = threading.Lock()    #lock to control terminal printing for legibility
arrLock = threading.Lock()  #lock for access to destination array/list
destArr = []

#update list of avaible addresses and ports
def arrUpdate(arr):
    arrLock.acquire()
    global destArr
    destArr.clear()
    destArr = arr.split()
    arrLock.release()

def arrPrint():
    arrLock.acquire()
    printLock.acquire()
    i = 0
    for x in destArr:
        if i%2 == 0:
            print(int(i/2), ':', end=' ')
        print(x, end=' ')
        if i%2:
            print()
        i+=1
    
    arrLock.release()
    printLock.release() 
    

#connect to directory
def dirConnect():
    dirSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    dirSocket.connect((dirAddress, 12000))
    dirSocket.send(str(serverPort).encode())
    destList = dirSocket.recv(1024).decode()
    arrUpdate(destList)
    arrPrint()
    dirSocket.close()

#request update from directory
def dirUpdate():
    dirSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    dirSocket.connect((dirAddress, 12000))
    dirSocket.send('Update'.encode())
    destList = dirSocket.recv(1024).decode()
    arrUpdate(destList)
    dirSocket.close()

#exit from directory
def dirExit():
    dirSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    dirSocket.connect((dirAddress, 12000))
    dirSocket.send(str(serverPort).encode())
    dirSocket.close()

#wrap message with relay and destination ports/ips
def msgConfig(msg, recvPort, recvIP):
    dirUpdate() #get up to date directory
    relays = [] #list of relays to go to 
    
    arrLock.acquire()
    indexs = list(range(len(destArr)))
    ports = indexs[1::2]
    random.shuffle(ports)
    while len(relays) < 2:
        rNum = int(ports.pop())
        posPort = destArr[rNum]    #random port
        posIP = destArr[rNum - 1]
        if(posIP != socket.gethostbyname(socket.gethostname()) and posIP != '127.0.0.1'):    #if different IP's, allow
            relays.append(posIP) #IP
            relays.append(posPort) #Port
        elif (posPort != str(serverPort) and posPort != str(recvPort)):  #if same IP, but different Port, allow
            relays.append(posIP) #IP
            relays.append(posPort) #Port
    arrLock.release()    
    
    relays.append(recvIP)   #last ip & port should be message recipients
    relays.append(str(recvPort))
    relays.append(msg)

    finMsg = ''
    for x in range(len(relays) - 1):    #create string of relays & msg seperated by ;
        finMsg += relays[x] + ';'
    finMsg += relays[4]
    
    return finMsg 

#get message and if necessary next relay to send onto    
def msgSeperator(msg):
    cells = msg.split(';')
    if len(cells) > 1:  
        nMsg = ''
        for x in range(2,len(cells) - 1):   #recombine cells except next relay info
            nMsg += cells[x] +';'
        nMsg += cells[len(cells) - 1]
        rVal = []
        rVal.append(cells[0]) #next ip
        rVal.append(cells[1]) #next port
        rVal.append(nMsg)   #rest of msg
        return rVal
    else:
        return cells

#One thread asks for inputs to send message
def send_thread():
    while True:
        clientSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sendReq = input('Enter SND to send a message, UP to update availible nodes, QUIT to exit ')
        if sendReq == 'SND':
            dirUpdate()
            arrLock.acquire()
            if len(destArr) < 6:    #1 relay requires 3 nodes = 3 IPs + 3 ports
                print('Not enough nodes to send message, updating availble nodes')
                arrLock.release()
                dirUpdate()
                arrPrint()
            else:
                while True:
                    enterNum = int(input('ST: Enter client # to go to (0,1,..): '))
                    if (enterNum <= (len(destArr)/2 -1) and enterNum >= 0): #loop until valid input
                        break
                senderPort = int(destArr[(enterNum*2)+1])   
                senderIP = destArr[(enterNum*2)]
                arrLock.release()
                printLock.acquire()
                msg = input('ST: Enter Message:  ')
                printLock.release()
                cMsg = msgConfig(msg, senderPort, senderIP) #wrap msg
                sendVals = msgSeperator(cMsg)   #get next relay
                clientSocket.connect((sendVals[0], int(sendVals[1])))
                clientSocket.send(sendVals[2].encode())
        elif sendReq == 'UP':
            dirUpdate()
            arrPrint()
        elif sendReq == 'QUIT':
            dirExit()
            clientSocket.connect(('127.0.0.1', serverPort))
            clientSocket.send('QUITREQ'.encode())
            clientSocket.close()
            break
        clientSocket.close()

#Other thread constantly listening for messages to recieve
def relay_thread():
    serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    serverSocket.bind(('',serverPort))
    serverSocket.listen(1)
    while True:
        connectionSocket, addr = serverSocket.accept()
        recMsg = connectionSocket.recv(1024).decode() # recieve message
        #decrypt msg here

        recVals = msgSeperator(recMsg)
        
        if recVals[0] == 'QUITREQ':
            print('Quitting...')
            break
        elif recVals[0] == 'PING\n':  #recieved ping from directory send ACK
            connectionSocket.send('ACK'.encode())
        elif len(recVals) > 1:    #send onto next relay
            print('relaying')
            relaySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            relaySocket.connect((recVals[0], int(recVals[1])))
            relaySocket.send(recVals[2].encode())
            relaySocket.close()
        else:   #this is the final stop!
            printLock.acquire()
            print('\nRT: From Client: ', recMsg)
            print('Enter SND to send a message, UP to update availible nodes, QUIT to exit ')
            printLock.release()
        connectionSocket.close()
    serverSocket.close()



#'main'
dirAddress = input('Enter Directory IPv4 Address: ')
serverPort = int(input('Enter this port num  > 1023: '))    #ask for this items port
dirConnect()
th1 = Thread(target=relay_thread, args=())
th2 = Thread(target=send_thread, args=())
th1.start()
th2.start()
th1.join()
th2.join()