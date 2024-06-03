/*
 * HTTPServer.java
 *
 * Version 1.0
 * Autor: H. Lind
 * Zweck: HTTP-Server, der Verbindungsanfragen entgegennimmt
 *       Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine HTTP-Anfrage bearbeitet
 */

import java.io.*;
import java.net.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;


public class HTTPServer {
    public static final String HTTP_VERSION = "HTTP/1.0";
    public static final String STANDARD_ROOT_PATH =  "/Users/hendrik/Entwicklung Studium/BWI4-RB Entwicklung/Praktikum3_HttpServer/html";
    /* HTTP-Server, der Verbindungsanfragen entgegennimmt */

    /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads  */
    public Semaphore workerThreadsSem;

    /* Portnummer */
    public final int serverPort;

    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean serviceRequested = true;

    public String rootPath;

    /* Standardkonstruktor (Server-Port 80) */
    public HTTPServer() {
        this(80);
    }

    /* Konrstruktor mit Parametern: Server-Port */
    public HTTPServer(int serverPort) {
        this(serverPort, 10, STANDARD_ROOT_PATH);
    }

    public HTTPServer(int serverPort, String roothPath) {
        this(serverPort, 10, roothPath);
    }


    /* Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler Worker-Threads*/
    public HTTPServer(int serverPort, int maxThreads, String rootPath) {
        this.serverPort = serverPort;
        this.workerThreadsSem = new Semaphore(maxThreads);
        this.rootPath = rootPath;
    }

    public void startServer() {
        ServerSocket welcomeSocket; // TCP-Server-Socketklasse
        Socket connectionSocket; // TCP-Standard-Socketklasse

        int nextThreadNumber = 0;

        try {
            /* Server-Socket erzeugen */
            System.err.println("Creating new TCP Server Socket Port " + serverPort);
            welcomeSocket = new ServerSocket(serverPort);

            while (serviceRequested) {
                workerThreadsSem.acquire();  // Blockieren, wenn max. Anzahl Worker-Threads erreicht

                System.err.println("HTTP Server is waiting for connection - listening TCP port " + serverPort);
                /*
                 * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
                 * Standard-Socket erzeugen und an connectionSocket zuweisen
                 */
                connectionSocket = welcomeSocket.accept();

                /* Neuen Arbeits-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt uebergeben */
                (new HTTPWorkerThread(nextThreadNumber++, connectionSocket, this)).start();
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public static void main(String[] args) {
        /* Erzeuge Server und starte ihn */
        HTTPServer myServer = new HTTPServer(80);
        myServer.startServer();
    }
}

// ----------------------------------------------------------------------------

class HTTPWorkerThread extends Thread {
    /*
     * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
     * erhaelt
     */
    /* Protokoll-Codierung des Zeilenendes: CRLF */
    private final String CRLF = "\r\n";

    private int name;
    private Socket socket;
    private HTTPServer server;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean workerServiceRequested = true; // Arbeitsthread beenden?

    public HTTPWorkerThread(int num, Socket sock, HTTPServer server) {
        /* Konstruktor */
        this.name = num;
        this.socket = sock;
        this.server = server;
    }

    public void run() {

        try {
            /* Socket-Basisstreams durch spezielle Streams filtern */
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());

            /* Verbindungsaufbau --> Anfrage empfangen */
            handleHTTPRequest();

            /* Socket-Streams schliessen --> Verbindungsabbau */
            socket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by client!");
        } finally {
            System.err.println("HTTP Worker Thread " + name + " stopped!");
            /* Platz fuer neuen Thread freigeben */
            server.workerThreadsSem.release();
        }
    }

    private void handleHTTPRequest() {
        System.err.println("Handle HTTP Request:");
        try {
            /* Lese HTTP-Anfrage */
            HTTPRequest request = readHTTPRequest();
            System.err.println("HTTP Request:");
            System.err.println(CRLF + "---------------------");
            System.err.println(request.toString());
            System.err.println(CRLF + "---------------------");

            /* Erzeuge HTTP-Antwort */
            HTTPResponse response = generateHTTPResponse(request);
            System.err.println("HTTP Response:");
            System.err.println(CRLF + "---------------------");
            System.err.println(response.toString());
            System.err.println(CRLF + "---------------------");

            writeToClient(response.getHttpVersion() + " " + response.getStatusCode() + " " + response.getStatusMessage());
            for(Map.Entry<String, String> entry : response.Headers().entrySet()) {
                writeToClient(entry.getKey() + ": " + entry.getValue());
            }
            writeToClient("");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HTTPResponse generateHTTPResponse(HTTPRequest request) {
        /* Erzeuge HTTP-Antwort */
        HTTPResponse response = new HTTPResponse();
        response.setHttpVersion(HTTPServer.HTTP_VERSION);

        System.err.println("Request Path: " + request.getPath());
        File file = new File(server.rootPath + request.getPath());

        if(!file.exists()) {
            response.setStatusCode(HTTPResponse.HTTPStatusCode.NOT_FOUND);
            response.setBody("File not found!");
        } else {
            response.setStatusCode(HTTPResponse.HTTPStatusCode.OK);
            response.setBody("File found!");
        }

        String userAgent = request.getHeader("User-Agent");
        if( (userAgent.contains("curl") || userAgent.contains("Firefox")) ) {
            response.setStatusCode(HTTPResponse.HTTPStatusCode.NOT_ACCEPTABLE);
            response.setBody("Not acceptable!");
        }

        response.setHeader("Date", Instant.now().toString());
        response.setHeader("Server", "Simple Java HTTP Server");

        return response;
    }


    private HTTPRequest readHTTPRequest() throws IOException {
        HTTPRequest request = new HTTPRequest();

        String curLine = readFromClient();
        request.setMethod(curLine.split(" ")[0]);
        request.setPath(curLine.split(" ")[1]);
        request.setHttpVersion(curLine.split(" ")[2]);

        curLine = readFromClient();
        while (!curLine.isEmpty() && curLine != CRLF) {
            String[] headerLineArr = curLine.split(": ");
            request.setHeader(headerLineArr[0], headerLineArr[1]);

            curLine = readFromClient();
        }

//        StringBuilder bodyStringBuilder = new StringBuilder();
//        curLine = readFromClient();
//        System.out.println(curLine);
//        while(curLine != null && !curLine.isEmpty()) {
//            bodyStringBuilder.append(curLine);
//            curLine = readFromClient();
//        }
//        System.err.println("ffwfwf");
//        request.setBody(bodyStringBuilder.toString());

        return request;
    }

    private String readFromClient() throws IOException {
        /* Lies die naechste Anfrage-Zeile (request) vom Client
         * ALLE Anfragen vom Client müssen über diese Methode empfangen werden ("Sub-Layer") */

        return inFromClient.readLine();
    }

    private void writeToClient(String line) throws IOException {
        /* Sende eine Antwortzeile zum Client
         * ALLE Antworten an den Client müssen über diese Methode gesendet werden ("Sub-Layer") */
        outToClient.write((line + CRLF).getBytes());
    }
}
