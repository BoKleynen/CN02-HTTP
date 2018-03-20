package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
	public static final int PORT = 4444;
	public static void main(String[] args) throws IOException {
		new TCPServer().runServer();
	}
	
	public void runServer() throws IOException {
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("Server up");
		while (true) {
			Socket socket = serverSocket.accept();
			System.out.println(socket);
			System.out.println("accepted");
			new ServerThread(socket).start();
		}
	}

}
