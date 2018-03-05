import java.io.InputStreamReader;
import java.net.Socket;
import java.net.InetAddress;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.Console;
import static java.lang.Integer.parseInt;
import static java.net.InetAddress.getByName;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("incorrect number of arguments");
            System.exit(1);
        }

        InetAddress address = getByName(args[1]);

        int port = 80;
        try {
            port = parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("invalid port number");
            System.exit(1);
        }

        Socket socket = new Socket(address, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String input;
        Console console = System.console();
        System.out.println("enter exit to terminate.");
        do {
            input = console.readLine();
            if (input.equals("exit"))
                break;

            out.println();
        } while (true);
    }
}
