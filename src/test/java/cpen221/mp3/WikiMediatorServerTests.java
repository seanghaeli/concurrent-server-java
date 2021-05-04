package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.NotFoundException;
import cpen221.mp3.fsftbuffer.TestType;
import cpen221.mp3.server.WikiMediatorClient;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import org.fastily.jwiki.core.Wiki;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class WikiMediatorServerTests {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private static final String SEARCH =
        "{ \"id\": \"SEARCH\", \"type\": \"search\", " +
            "\"query\": \"Barack Obama\", \"limit\": \"1\" }";
    private static final String GETPAGE =
        "{ \"id\": \"PAGE\", \"type\": \"getPage\", " +
            "\"pageTitle\": \"UBC\", \"timeout\": \"1\" }";
    private static final String ZEITGEIST =
        "{ \"id\": \"ZEITGEIST\", \"type\": \"zeitgeist\", " +
            "\"limit\": \"5\" }";
    private static final String TRENDING =
        "{ \"id\": \"TRENDING\", \"type\": \"trending\", " +
            "\"limit\": \"5\" }";
    private static final String PEAK30 =
        "{ \"id\": \"PEAK30\", \"type\": \"peakLoad30s\" }";
    private static final String STOP =
        "{ \"id\": \"STOP\", \"type\": \"stop\" }";

    @Test
    public void testAllSequential() throws InterruptedException {

        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(200);
        String[] clientArgs = {SEARCH, GETPAGE, ZEITGEIST, TRENDING, PEAK30, STOP};
        WikiMediatorClient.main(clientArgs);
        String reply = outContent.toString();
        serverThread.join();
        assertEquals(
            "{\"id\":\"SEARCH\",\"status\":\"success\"," +
                "\"response\":[\"Barack Obama\"]}\r\n" +
                "{\"id\":\"PAGE\",\"status\":\"success\",\"response\":" +
                "\"#REDIRECT [[University of British Columbia]]\"}\r\n" +
                "{\"id\":\"ZEITGEIST\",\"status\":\"success\",\"response\":" +
                "[\"Barack Obama\",\"UBC\"]}\r\n" +
                "{\"id\":\"TRENDING\",\"status\":\"success\",\"response\":" +
                "[\"Barack Obama\",\"UBC\"]}\r\n" +
                "{\"id\":\"PEAK30\",\"status\":\"success\",\"response\":\"5\"}\r\n" +
                "{\"id\":\"STOP\",\"type\":\"bye\"}\r\n", reply);
    }

    @Test
    public void testInvalidOperation() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(WikiMediatorServer::main);
        serverThread.start();
        Thread.sleep(200);
        String[] clientArgs = {"{ \"id\": \"invalid\", \"type\": \"invalid\" }", STOP};
        WikiMediatorClient.main(clientArgs);
        String reply = outContent.toString();
        serverThread.join();
        assertEquals(
            "{\"id\":\"invalid\",\"status\":\"failed\"," +
                "\"response\":\"Invalid operation\"}\r\n" +
                "{\"id\":\"STOP\",\"type\":\"bye\"}\r\n", reply
        );
    }

    @Test
    public void testLog() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(200);
        String[] args = {GETPAGE, STOP};
        WikiMediatorClient.main(args);
        serverThread.join();
        Thread serverThread2 = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread2.start();
        Thread.sleep(200);
        String[] args2 = {ZEITGEIST, STOP};
        WikiMediatorClient.main(args2);
        String reply = outContent.toString();
        serverThread2.join();
        assertEquals("{\"id\":\"PAGE\",\"status\":\"success\",\"response\":" +
            "\"#REDIRECT [[University of British Columbia]]\"}\r\n"
            + "{\"id\":\"STOP\",\"type\":\"bye\"}\r\n" +
            "{\"id\":\"ZEITGEIST\",\"status\":\"success\",\"response\":" +
            "[\"UBC\"]}\r\n" + "{\"id\":\"STOP\",\"type\":\"bye\"}\r\n", reply);
    }

    @Test
    public void delayedRequest() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(200);
        String[] args = {GETPAGE};
        WikiMediatorClient.main(args);
        Thread.sleep(100);
        WikiMediatorClient.main(args);
        Thread.sleep(100);
        args = new String[] {STOP};
        WikiMediatorClient.main(args);
        String reply = outContent.toString();
        serverThread.join();
        assertEquals("{\"id\":\"PAGE\",\"status\":\"success\",\"response\":" +
            "\"#REDIRECT [[University of British Columbia]]\"}\r\n" +
            "{\"id\":\"PAGE\",\"status\":\"success\",\"response\":" +
            "\"#REDIRECT [[University of British Columbia]]\"}\r\n" +
            "{\"id\":\"STOP\",\"type\":\"bye\"}\r\n", reply);
    }

    @Test
    public void testTimeout() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(200);
        String[] args = {"{ \"id\": \"PAGE\", \"type\": \"getPage\", " +
            "\"pageTitle\": \"UBC\", \"timeout\": \"0\" }", STOP};
        WikiMediatorClient.main(args);
        String reply = outContent.toString();
        serverThread.join();
        assertEquals("{\"id\":\"PAGE\",\"status\":\"failed\"," +
            "\"response\":\"Operation timed out\"}\r\n"
            + "{\"id\":\"STOP\",\"type\":\"bye\"}\r\n", reply);
    }

    @Test
    public void testMultipleClients() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(200);

        String[] args1 = {SEARCH};
        String[] args2 = {GETPAGE};
        String[] args3 = {ZEITGEIST};
        String[] args4 = {TRENDING};
        String[] args5 = {PEAK30};


        Thread thread1 = new Thread(() -> {
            WikiMediatorClient.main(args1);
        });
        Thread thread2 = new Thread(() -> {
            WikiMediatorClient.main(args2);
        });
        Thread thread3 = new Thread(() -> {
            WikiMediatorClient.main(args3);
        });
        Thread thread4 = new Thread(() -> {
            WikiMediatorClient.main(args4);
        });

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        Thread.sleep(200);
        WikiMediatorClient.main(args5);
        String reply = outContent.toString();
        WikiMediatorClient.main(new String[] {STOP});
        serverThread.join();
        assertTrue(
            reply.contains("{\"id\":\"PEAK30\",\"status\":\"success\",\"response\":\"5\"}\r\n"));

    }

    @Test
    public void multipleClients() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Thread serverThread = new Thread(() -> {
            try {
                new WikiMediatorServer(4949, 2);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        serverThread.start();
        Thread clientThread1 = new Thread(() -> {
            String[] args = {GETPAGE, GETPAGE};
            WikiMediatorClient.main(args);
        });
        Thread clientThread2 = new Thread(() -> {
            String[] args = {GETPAGE, SEARCH};
            WikiMediatorClient.main(args);
        });
        Thread clientThread3 = new Thread(() -> {
            String[] args = {ZEITGEIST, STOP};
            WikiMediatorClient.main(args);
        });
        clientThread1.start();
        clientThread2.start();
        clientThread3.start();
        clientThread1.join();
        clientThread2.join();
        clientThread3.join();
        String reply = outContent.toString();
        assertTrue(reply
            .contains("{\"id\":\"ZEITGEIST\",\"status\":\"success\",\"response\":" +
                "[\"UBC\""));
        serverThread.join();
    }
}
