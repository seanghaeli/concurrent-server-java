package cpen221.mp3.server;


import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class WikiMediatorClient {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    /**
     * Rep Invariant:
     * socket, in, out are not null
     */
    /**
     * Abstraction Function:
     * Represents a client that connects to the WikiMediatorServer
     * with requests and receives the results of those requests.
     */
    /**
     * Thread safety argument:
     * socket, in, and out are final and immutable
     */

    /**
     * Create a new instance of WikiMediatorClient that connects to hostname
     * through the given port
     * @param hostname name of the connection
     * @param port port of the connection
     * @throws IOException if the connection is interrupted
     */
    public WikiMediatorClient(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    /**
     * Start the client with default hostname and port number.
     * @param args
     */
    public static void main(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                WikiMediatorClient client = new WikiMediatorClient(null, 4949);
                client.sendRequest(args[i]);
                String reply = client.getReply();
                System.out.println(reply);
                client.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Sends a Json formatted String request to the server
     * @param jsonString the requested operation
     */
    public void sendRequest(String jsonString) {
        out.print(jsonString);
        out.flush();
    }

    /**
     * Receives the result of the previously requested operation
     * or throws IOException if interrupted.
     * @return result of requested operation
     * @throws IOException if connection interrupted
     */
    public String getReply() throws IOException {
        return in.readLine();
    }

    /**
     * Close the current connection between the client and server.
     * @throws IOException if connection interrupted
     */
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}


