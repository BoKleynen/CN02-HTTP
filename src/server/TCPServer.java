package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class sets up the server and the connection between client and server.
 */
public class TCPServer {
	public static final int PORT = 4444;
	public static void main(String[] args) throws IOException {
		new TCPServer().runServer();
	}
	
	public void runServer() throws IOException {
	    // Start server
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("Server up");
		while (true) {
		    // Infinity loop: looking for new clients and making a new thread per client.
			Socket socket = serverSocket.accept();
			System.out.println(socket);
			new ServerThread(socket).start();
		}
	}

}
