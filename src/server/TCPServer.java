package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPserver {
	public static final int PORT = 4444;
	public static void main(String[] args) throws IOException {
		new TCPserver().runServer();
	}
	
	public void runServer() throws IOException {
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("Server up");
		while (true) {
			Socket socket = serverSocket.accept();
			new ServerThread(socket).start();
			
		}
	}

}
