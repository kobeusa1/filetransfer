import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class TCPClient  {
    private Socket socket;
    static int ack= 0;
    static int close = 0;
    static int pack_size = 50;
    public static void main(String args[]) throws UnknownHostException, IOException {
        String filename = args[0];
        String log_name = args[4]; // these code is to get user input information
        int self_post = Integer.parseInt(args[1]);
        String ip_add = args[2];
        int host_post = Integer.parseInt(args[3]);
        
        
    	InetAddress ip = InetAddress.getByName(ip_add); //build tcp socket to transfer acks
        Socket socket = new Socket(ip, host_post);
        int a;
        byte[] message = new byte[1024*1024];
      
        byte [] incomingData = new byte[1024*1024];
    	DatagramSocket sock = null;
		try {
			sock = new DatagramSocket(self_post); // build udp socket to receive socket
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
        DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
        int sequence_number_expected = 0; // the value of the expeceted value of sequence number
        int sequence_number_received = 0; // the value of the actual value of received sequence number

        int checksum = 0;
        while(true){   
            try {
				sock.receive(incomingPacket); // receive data
			} catch (IOException e) {
				
				e.printStackTrace();
			}

            Arrays.fill(message, (byte) 0); // empty the buffer
            byte[] data = incomingPacket.getData();
            int length = incomingPacket.getLength();
         
            int winsize = (length-20)/pack_size; // calculate the winsize
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            in.read(message,0,length); // store the received data into message buffer
            a= ((message[1])>=0?message[1]:256+message[1])  + (message[0] >=0? message[0]<<8 : (256+ message[0])<<8); //these forumla is used to recover the number in header
     	    int b;
     	    b= ((message[3])>=0?message[3]:256+message[3])  + (message[2] >=0? message[2]<<8 : (256+ message[2])<<8);
     	    checksum = ((message[17])>=0?message[17]:256+message[17])  + (message[16] >=0? message[16]<<8 : (256+ message[16])<<8);
     	    int ack_received = ((message[11])>=0?message[11]:256+message[11])  + (message[10] >=0? message[10]<<8 : (256+ message[10])<<8);
    		int sum = 0;
    		int i;
    	    for (i = 0;i<length;i+=2){
    	    	if(i == 17 || i==16){
    	    		continue;
    	    	}
    			if ((i+1)>length){
    			
    				sum += (message[i]>=0?message[i]:256+ message[i]);
    				if (sum > 65535){
    					int g,h;
    					g = sum >>16;
    				    h = sum - (g<<16);
    				    sum = h+1;  				   
    				}
    				break;
    			}
    			sum +=((message[i+1])>=0? message[i+1]:256+message[i+1])  + (message[i] >=0? message[i]<<8 : (256+ message[i+1])<<8);
    			if (sum > 65535){
    				int g,h;
    				g = sum >>16;
    			    h = sum - (g<<16);
    			    sum = h+1;
    			   
    			}
    			
    		}
    	    sum += checksum; // recover the checksum and see if any corruption
    	    sequence_number_received = ((message[7])>=0?message[7]:256+message[7])  + (message[6] >=0? message[6]<<8 : (256+ message[6])<<8);
    	    System.out.println("The received sequence num is " + sequence_number_received);
		    DateFormat df = new SimpleDateFormat("ss:ms");
		    Date date = new Date();
		   
		    String value = df.format(date) + " " + String.valueOf(host_post)+" " + String.valueOf(self_post)+ " "+String.valueOf(sequence_number_received)+ " " + String.valueOf(ack_received) + " " + "FIN:  " + String.valueOf(message[19]); // create the string to store information and save in the file named by the user
		 
		    File f = new File(log_name);
		    if(!f.exists()){
		    	f.createNewFile();
		    }
	    	try{
    	    	FileWriter fos = new FileWriter(f,true);
    	    	fos.write(value);
    	    	fos.write("\n");
    	    
    	    	fos.flush();
    	     	fos.close();    	    
    	    	
	    	} catch (FileNotFoundException e) {
	    		e.printStackTrace();
	    	}catch (IOException e) {
	    		
	            e.printStackTrace();
	
	        }
	    	if(sum != 65535){ // data received is corrput,send same ack
	    		System.out.println("Data corrupt!!");
	    	}
	    	if(sequence_number_received!= sequence_number_expected){ // ignore duplicate data
    	    	continue;
    	    	  	    	    	
    	    }else if (sum == 65535){ // nothing wrong with the received data,send new ack and save it into file
        	    sequence_number_expected += winsize;
    	    	ack += winsize;
    	    	byte[] data_to_store = new byte[length];
    	    	Arrays.fill(data_to_store, (byte)0);
    
    	    	System.arraycopy(message, 20, data_to_store, 0, length-20);
    	    	//byte [] data_to_write = new byte[length];
    	    	//System.arraycopy(data_to_store, 0, data_to_write, 0, length);
    	    	//File des = new File("/Users/hongyili/Desktop/file_received.txt");
    	    	File des = new File(filename);
    	    	if(!des.exists()){
    	    		des.createNewFile();
    	    	}
    	    	try{
        	    	FileOutputStream fos = new FileOutputStream(des,true);
        	    	fos.write(data_to_store);
        	    	fos.flush();
        	     	fos.close();    	    
        	    	//System.out.println("Save sucess");
    	    		
    	    	} catch (FileNotFoundException e) {
    	    		e.printStackTrace();
    	    	}catch (IOException e) {
    	    		
    	            e.printStackTrace();
    	
    	        }

    	    }

	        DataOutputStream out; 
		    byte[] ack_number = new byte[2];
		    int x;
		    int y;				
		    x = ack >> 8; // the first byte of port number
		    y = ack - (a<<8);
		    ack_number[0] = (byte) x;
		    ack_number[1] = (byte) y;
		    try {
		    	out = new DataOutputStream(socket.getOutputStream());
		    	out.write(ack_number.length);
		    	out.write(ack_number);
		    	//System.out.println("ACK sent successfully "+ack);
		    	} catch (IOException e1) {
		    		e1.printStackTrace();
		    	}
		    if(message[19] == 1 && sum == 65535 ){

	    	   break;	    	 
	        }
		    }
        System.out.println("File recevied successfully");
        
        
   
        }
    }
        


