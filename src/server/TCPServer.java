package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class sets up the server and the connection between client and server.
 */
public class TCPServer {
	public static final int PORT = 4444;

	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("Server up");
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				System.out.println(socket);
				new ServerThread(socket).start();

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
