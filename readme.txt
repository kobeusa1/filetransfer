The program can implement the file transfer from server to receiver under bad network condition. To invoke the code, user need type make, then type java. After that, type java <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>. same thing for receiver(<filename> <listening_port> <sender_IP> <sender_port>
<log_filename>).

In my code, the sender is called server and receiver is client. Note, user have to firstly run server code before run the client code. 
Note that the maximum size of one each transfer of file is 1MB.
Note that the user have to put transferred file into the folder or type the full path
I set each package size 50 byte. Window size is defined by user.

In sender log file, the arguments from left to right are: time,sender port(to receive ack)
destination port, sequence number of current data, received ack number from receiver, and FIN flag

In receiver log file, the arguments from left to right are: time,source por,self port(to receive data), sequence number of received data, received ack number in header(for next expected sequence number), and FIN flag


Answer the questions in hw:

a). TCP is build as a thread in server code(sender), it receive ack from client(receiver). If the ack is same as before, re-transmit the data. The receiver will transfer update ack if nothing wrong with the data and will transfer old ack if file corrupt. Ignore duplicate data.

b). I tried my code under different network condition, the states were all successfully transferred and received.

c). I create a thread for recovery loss data. If the data is lost, the receiver will not receive the data, hence, will not transfer ack to sender. I set the timeout time 500ms, if that time past, then ,the sender will re-transmit the packet. If it is caused by delay, the receiver will receive duplicate data, but will ignore since the sequence number already received. 
