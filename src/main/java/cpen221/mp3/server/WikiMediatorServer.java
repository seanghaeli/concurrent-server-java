package cpen221.mp3.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cpen221.mp3.wikimediator.WikiMediator;

import javax.json.Json;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WikiMediatorServer {
    /*Default port of the server is 4949*/
    private static final int DEFAULT_PORT = 4949;
    /*Default request limit is 10 concurrent requests*/
    private static final int DEFAULT_LIMIT = 10;

    private final int numRequests;
    private final ServerSocket serverSocket;
    private final WikiMediator wm;
    private boolean stop;
    /**
     * Rep Invariant:
     * numRequests, serverSocket, wm, count, stop are not null
     * stop is only true if the server is about to be terminated
     */
    /**
     * Abstraction Function:
     * Represents a server that processes requests
     * associated with WikiMediator for up to numRequests
     * connected clients concurrently.
     */
    /**
     * Thread safety argument:
     * numRequests, serverSocket, and wm are final and immutable
     * variables used in methods are thread-confined
     * stop will have data races between threads but that is
     * fine as the server should terminate as soon as
     * one thread/client requests it to stop
     */

    /**
     * Start a server at a given port number, with the ability to process
     * up to n requests concurrently.
     *
     * @param port the port number to bind the server to
     * @param n    the number of concurrent requests the server can handle
     * @throws IOException if connection experiences an error
     */
    public WikiMediatorServer(int port, int n) throws IOException {
        this.numRequests = n;
        serverSocket = new ServerSocket(port);
        wm = new WikiMediator();
        stop = false;
        serve();
    }

    /**
     * handle identifies the input from socket and returns
     * the result of input request through the socket.
     *
     * @param socket the socket where input and output is passed
     * @throws IOException if connection experiences an error
     */
    private void handle(Socket socket) throws IOException {
        BufferedReader in =
            new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        JsonReader jsonIn = new JsonReader(in);
        PrintWriter out =
            new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream()), true);
        JsonObject jsonObject = new Gson().fromJson(jsonIn, JsonObject.class);
        try {
            String id = jsonObject.get("id").getAsString();;
            String type = jsonObject.get("type").getAsString();
            String query = "NO_QUERY";
            String pageTitle = "NO_PAGE_TITLE";
            int limit = DEFAULT_LIMIT;
            int timeout = Integer.MAX_VALUE;

            if (jsonObject.has("query")) {
                query = jsonObject.get("query").getAsString();
            }
            if (jsonObject.has("pageTitle")) {
                pageTitle = jsonObject.get("pageTitle").getAsString();
            }
            if (jsonObject.has("limit")) {
                limit = jsonObject.get("limit").getAsInt();
            }
            if (jsonObject.has("timeout")) {
                timeout = jsonObject.get("timeout").getAsInt();
            }
            String result = request(id, type, query, pageTitle, limit, timeout);
            out.println(result);
        } finally {
            out.close();
            in.close();
        }
    }

    /**
     * serve recognizes up to numRequests clients and
     * creates a new thread of handle() to identify.
     *
     * @throws IOException if connection experiences an error
     */
    private void serve() throws IOException {
        while (true) {
            ExecutorService executor = Executors.newFixedThreadPool(numRequests);
            Socket socket = serverSocket.accept();
            Runnable handler = () -> {
                try {
                    try {
                        handle(socket);
                    } finally {
                        socket.close();
                        if (stop) {
                            serverSocket.close();
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            };
            executor.execute(handler);
        }

    }

    /**
     * Start a WikiMediatorServer running on the default port and
     * with default concurrent requests limit.
     */
    public static void main() {
        try {
            new WikiMediatorServer(DEFAULT_PORT, DEFAULT_LIMIT);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * This method processes the client's request and returns the result of the
     * requested operation.
     *
     * @param id      id of the client
     * @param type    type of operation requested
     * @param query   the search query if applicable
     * @param limit   the limited number of results if applicable
     * @param timeout the amount of time this method has to process the request
     *                before timing out in seconds
     * @return the Json formatted String corresponding to the request if
     * successful, or an error message if unsuccessful
     */
    private String request(String id, String type, String query, String pageTitle, int limit,
                           int timeout) {
        Gson gson = new Gson();

        if (type.equals("stop")) {
            wm.log();
            stop = true;
            return gson.fromJson("{ \"id\": \"" + id + "\", \"type\": \"bye\" }", JsonObject.class)
                .toString();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> handler =
            executor.submit(() -> gson.toJson(
                new Result<>(id, "failed", "Invalid operation")));

        if (type.equals("search")) {
            List<String> search = wm.search(query, limit);
            handler = executor.submit(
                () -> gson.toJson(new Result<>(id, "success", search)));
        }
        if (type.equals("getPage")) {
            String page = wm.getPage(pageTitle);
            handler = executor.submit(
                () -> gson.toJson(new Result<>(id, "success", page)));
        }
        if (type.equals("zeitgeist")) {
            List<String> zeitgeist = wm.zeitgeist(limit);
            handler = executor.submit(
                () -> gson.toJson(new Result<>(id, "success", zeitgeist)));
        }
        if (type.equals("trending")) {
            List<String> trending = wm.trending(limit);
            handler = executor.submit(
                () -> gson.toJson(new Result<>(id, "success", trending)));
        }
        if (type.equals("peakLoad30s")) {
            int peak = wm.peakLoad30s();
            handler = executor.submit(
                () -> gson.fromJson(
                    "{\"id\": \"" + id + "\", \"status\": \"success\", " +
                        "\"response\": \"" + peak + "\" }", JsonObject.class).toString());
        }

        try {
            return handler.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            handler.cancel(true);
            return gson.toJson(
                new Result<>(id, "failed", "Operation timed out"));
        } catch (InterruptedException ie) {
            handler.cancel(true);
            return gson.toJson(
                new Result<>(id, "failed", "Operation interrupted"));
        } catch (ExecutionException ee) {
            handler.cancel(true);
            return gson.toJson(
                new Result<>(id, "failed", "Execution failed"));
        }
    }

    private class Result<T> {
        private final String id;
        private final String status;
        private final T response;
        /**
         * Rep Invariant:
         * id, status, and response are not null
         */
        /**
         * Abstraction Function:
         * Stores the id, status, and response in
         * a convenient class for the purpose of
         * making the processing easier to format.
         */
        /**
         * Thread safety argument:
         * this class only assigns final, immutable fields once.
         */

        /**
         * Creates a new instance of result
         *
         * @param id       the id of the request
         * @param status   whether the request succeeded or failed
         * @param response the output of the request
         */
        public Result(String id, String status, T response) {
            this.id = id;
            this.status = status;
            this.response = response;
        }
    }
}
