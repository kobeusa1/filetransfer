import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;	
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
public class TCPServer extends Thread { // create TCP channel to receive the acks from receiver

    static int time_out;
    static int stop = 0;
    static int acknum = 0; 
    static int checker = 0;
    static int ack_arrive = 1;
    static int package_size = 50;
   
    private Socket socket;

    public TCPServer(Socket sock) {
        socket = sock;
    }	
    public void run() {
        int ack_buffer = 0;
  
        int length;
    
        try {
           DataInputStream data_input = new DataInputStream(socket.getInputStream());         
           while ((length = data_input.read()) > 0) {   
        	   byte[] message = new byte[length];
        	   data_input.readFully(message,0,message.length);
        	     	         	
        	   acknum= ((message[1])>=0?message[1]:256+message[1])  + (message[0] >=0? message[0]<<8 : (256+ message[0])<<8);
        	   //System.out.println("The received acknum" +" " + acknum);
             
                if (ack_buffer >= acknum){ //duplicate acks, mean need to re-transmit
                	checker++;
            
                	
                }else{                	
                	checker = 0;
                }
                ack_buffer = acknum;
                ack_arrive = 1;
         
                
           }
        } catch (IOException e) {
            e.printStackTrace();
        } 
        try {
        	socket.close();
        	
        	} catch (IOException e) {
                e.printStackTrace();
            }
        
    }

    public static void main(String[] args) throws IOException {
        String filename = args[0];
        String log_name = args[4];
        int port = Integer.parseInt(args[3]);
        int winsize;
        int client_port = Integer.parseInt(args[2]);
        String ip_addr = args[1];
        if (args.length ==5){ // check the number of user input, the defaut is 1
            winsize =1;
        }else{
            winsize =Integer.parseInt(args[5]);
        }
        ServerSocket server_socket= new ServerSocket(port);
        
        System.out.println("Socket ready");
        System.out.println(args.length);
        System.out.println(args[0]);
        Socket clientsocket = server_socket.accept();
        TCPServer server = new TCPServer(clientsocket); // create TCP thread
        server.start();
        
        try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        System.out.println("TCP connection ready, ready to transfer file");
        File f = new File(filename); // check the existence of transferred file
        if(!f.exists()){
        	System.out.println("There is no such file");
        	System.exit(0);
        }
        InputStream file = new FileInputStream(filename);
        
        DatagramSocket socket = new DatagramSocket(); // create the udp socket
        BufferedInputStream bis = new BufferedInputStream(file);
        byte[] data_gather = new byte[package_size*winsize];
        int sequence_num = 0;
        byte [] header = new byte[20]; //create a header 20 byte
        Arrays.fill(header, (byte) 0); // initialize the header
        int a;
        int b;
        a = port >> 8; // the first byte of port number
        b = port - (a<<8);
        header[0] = (byte) a;
        header[1] = (byte) b;
        a = client_port >> 8; // the first byte of client_port number
        b = client_port - (a<<8);//second byte of client port number	
        header[2] = (byte) a;
        header[3] = (byte) b;
        int c,d;
        int read_length=0;
        int counter = 0;
        int counter_re = 0;
        int total_byte = bis.available();
        int lastrun = 1;
        InetAddress ip = InetAddress.getByName(ip_addr);
        Time_out thread = new Time_out();
        thread.start();
        
        while (bis.available()>0||lastrun ==1){
        	
        	
        	try {
				Thread.sleep(1); 	
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
     
        	if (checker == 0 && ack_arrive ==1){ 
        		if(bis.available() <=0){
        			lastrun = 0;
        			continue;
        		}
        		counter++;
        		thread.interrupt();
        		Arrays.fill(data_gather,(byte)0);
        		System.out.println("transmitting");
        		System.out.println(bis.available());
        		if (bis.available() < package_size*winsize){ // check if it is the last package to transmit
        			read_length = bis.available();
        			header[19] = (byte)1;
        			
        		}else{
        			read_length = package_size*winsize;
        		}
        	
        		ack_arrive = 0;	
        		int ack_next = acknum+winsize;
        		bis.read(data_gather,0,read_length);  
        		c = sequence_num >>8;
                d = sequence_num - (c<<8);
                header[6] = (byte) c;
                header[7] = (byte) d;
                c = ack_next >>8;
                d = ack_next- (c<<8);
                header[10] = (byte) c;
                header[11]= (byte) d;                      		      		
        		byte[] data_sent = new byte [header.length + data_gather.length];  // data_sent is a combination of header and payload
     		    System.arraycopy (header,0,data_sent,0,header.length);
    		    System.arraycopy (data_gather,0,data_sent,header.length,data_gather.length);
    			int sum = 0;
    			int i;
    		    for (i = 0;i<data_sent.length;i+=2){
        	    	if(i == 17 || i==16){
        	    		continue;
        	    	}
    				if ((i+1)>data_sent.length){
    					sum += (data_sent[i]>=0?data_sent[i]:256+data_sent[i]);
        				if (sum > 65535){
        					int g,h;
        					g = sum >>16;
        				    h = sum - (g<<16);
        				    sum = h+1;
        				   
        				}
    					break;
    				}
    				sum +=((data_sent[i+1])>=0?data_sent[i+1]:256+data_sent[i+1])  + (data_sent[i] >=0? data_sent[i]<<8 : (256+ data_sent[i+1])<<8);
    				if (sum > 65535){
    					int g,h;
    					g = sum >>16;
    				    h = sum - (g<<16);
    				    sum = h+1;
    				   
    				}
    				
    			}
    		    int checksum = 65535 - sum;
    		    int x,y;
    		    x = checksum >>8;
    			y = checksum - (x<<8);
    			header[16]=(byte)x;
    			header[17]=(byte)y;
    			System.arraycopy (header,0,data_sent,0,header.length);    		  
    		    DatagramPacket sendPacket = new DatagramPacket(data_sent, data_sent.length, ip, client_port); 
 
    		    try{
    		    	socket.send(sendPacket); 
    		    	//System.out.println("the sequence number sent is "+ sequence_num);
    		    } catch (IOException e) {
                    e.printStackTrace();
                }
    		    DateFormat df = new SimpleDateFormat("ss:ms"); // get current time
    		    Date date = new Date();
    		    String value = df.format(date) + " " + String.valueOf(port)+" " + String.valueOf(client_port)+ " "+String.valueOf(sequence_num)+ " " + String.valueOf(acknum) +" "+ String.valueOf(header[19]) ;
    		    File ff = new File(log_name);
    		    if(!ff.exists()){
    		    	ff.createNewFile();
    		    }
    	    	try{
        	    	FileWriter fos = new FileWriter(ff,true);
        	    	fos.write(value);
        	    	fos.write("\n");
        	    
        	    	fos.flush();
        	     	fos.close();    	    
        	    	
    	    	} catch (FileNotFoundException e) {
    	    		e.printStackTrace();
    	    	}catch (IOException e) {
    	    		
    	            e.printStackTrace();
    	
    	        }

    		   	    
    		    sequence_num += winsize;
        		
        	}else if((checker >0 && ack_arrive ==1)||(time_out ==1)){ // file corrupt, need to re-transmit
        		time_out =0;
        		counter_re++;
        		ack_arrive = 0;                      		      		
        		byte[] data_resent = new byte [header.length + data_gather.length];
     		    System.arraycopy (header,0,data_resent,0,header.length);
    		    System.arraycopy (data_gather,0,data_resent,header.length,data_gather.length);	
    		    DatagramPacket sendPacket = new DatagramPacket(data_resent, data_resent.length, ip, client_port); 
    		    try{
    		    	socket.send(sendPacket);   		    
    		    } catch (IOException e) {
                    e.printStackTrace();
                }
    		    DateFormat df = new SimpleDateFormat("ss:ms");
    		    Date date = new Date();
    		    String value = df.format(date) + " " + String.valueOf(port)+" " + String.valueOf(client_port)+ " "+String.valueOf(sequence_num)+ " " + String.valueOf(acknum)+ " " +String.valueOf(header[19]) ;
    		    File ff = new File(log_name);
  
    		    if(!ff.exists()){
    		    	ff.createNewFile();
    		    }
    	    	try{
        	    	FileWriter fos = new FileWriter(ff,true);
        	    	fos.write(value);
        	    	fos.write("\n");
        	    
        	    	fos.flush();
        	     	fos.close();    	    
        	    	
    	    	} catch (FileNotFoundException e) {
    	    		e.printStackTrace();
    	    	}catch (IOException e) {
    	    		
    	            e.printStackTrace();
    	
    	        }
        		
        	}   	
        	      	
              	
        }
       if (header[19]==1){
    	   System.out.println("Delivery completed successfully");
    	   System.out.println("Total byte sent- "+total_byte);
    	   System.out.println("Segments sent- " + counter);
    	   System.out.println("Segments retransmitted- "+ counter_re);
       }else{
    	   System.out.println("Transmission error");
    	   System.out.println("Segments sent-" + counter);
    	   System.out.println("Segments retransmitted- "+ counter_re);
       }
       stop =1;
       thread.interrupt();
      
       
        try {
        	server_socket.close();
        	
        	} catch (IOException e) {
                e.printStackTrace();
            }
        socket.close();
        bis.close();
             
        
    }

}
// the class is used to implement time_out
class Time_out extends Thread{
	@Override	
	public void run(){
		while (true){
			if (TCPServer.stop==1){
				break;
			}
			while(!Thread.interrupted()){ //interrputed if ack received,
				try {
					Thread.sleep(500);
					TCPServer.time_out = 1;
				} catch (InterruptedException e) {
					break;
				}
			}
			
			
		}
		System.out.println("File sent successfully");
	
		
	}

}


