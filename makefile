tasks: TCPServer TCPClient
TCPServer: TCPServer.java
	javac -classpath . TCPServer.java
TCPClient: TCPClient.java
	javac -classpath . TCPClient.java
clean:
	rm -rf TCPServer TCPClient *.o
