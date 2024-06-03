/*
 * HTTPServer.java
 *
 * Version 1.0
 * Autor: H. Lind
 * Zweck: HTTP-Server, der Verbindungsanfragen entgegennimmt
 *       Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine HTTP-Anfrage bearbeitet
 */

import org.w3c.dom.html.HTMLImageElement;

import java.io.*;
import java.net.*;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


public class HTTPServer {
    public static final String HTTP_VERSION = "HTTP/1.0";
    public static final String STANDARD_ROOT_PATH = "/Users/hendrik/Entwicklung Studium/BWI4-RB Entwicklung/Praktikum3_HttpServer/html";
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

/*
 * HTTPWorkerThread.java
 *
 * Version 1.0
 * Autor: H. Lind
 * Zweck: Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung erhaelt
 */
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

    /* Konstruktor */
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

    /* HTTP-Anfrage bearbeiten */
    private void handleHTTPRequest() {
        System.err.println("Handle HTTP Request:");
        try {
            /* Lese HTTP-Anfrage */
            HTTPRequest<String> request = readHTTPRequest();
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

            /* Sende HTTP-Antwort */
            writeToClient(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HTTPResponse generateHTTPResponse(HTTPRequest request) {
        /* Erzeuge HTTP-Antwort */
        HTTPResponse response;
        Map<String, String> headers = new HashMap<String, String>();

        /* Überprüfe, ob der User-Agent ein Browser ist */
        String userAgent = request.getHeader("User-Agent");
        if (!(userAgent.contains("curl") || userAgent.contains("Firefox") || userAgent.contains("HTTPie"))) {
            String body = "User-agent is not accepted!";

            response = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.NOT_ACCEPTABLE, headers, body);
            response.setHeader("Content-Type", "text/plain");

            return setStandardHeaders(response);
        }

        if (!request.getMethod().equals(HTTPRequest.HTTPMethod.GET)) {
            String body = "Method not supported!";

            response = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.BAD_REQUEST, headers, body);
            response.setHeader("Content-Type", "text/plain");

            return setStandardHeaders(response);
        }

        /* RESTful API Zeitserver */
        switch (request.getPath()) {
            case "/time":
                DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
                String body = LocalTime.now().format(dtf);

                response = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.OK, headers, body);
                response.setHeader("Content-Type", "text/plain");
                response.setHeader("Content-Length", String.valueOf(body.length()));

                return setStandardHeaders(response);
            case "/date":
                dtf = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                body = LocalDate.now().format(dtf);

                response = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.OK, headers, body);
                response.setHeader("Content-Type", "text/plain");
                response.setHeader("Content-Length", String.valueOf(body.length()));

                return setStandardHeaders(response);
            default:
                break;
        }

        /* Überprüfe, ob die angeforderte Datei existiert */
        System.err.println("Request Path: " + request.getPath());
        File file = new File(server.rootPath + request.getPath());

        if (!file.exists()) {
            // Datei nicht gefunden
            String body = "404 - File not found!)";

            response = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.NOT_FOUND, headers, body);
            response.setHeader("Content-Type", "text/plain");

            return setStandardHeaders(response);
        } else if (file.isDirectory()) {
            // Verzeichnisinhalt anzeigen
            File[] files = file.listFiles();

            StringBuilder body = new StringBuilder();
            body.append("<html><head><title>Directory Listing</title></head><body>");
            body.append("<h1>Directory Listing</h1>");
            body.append("<ul>");
            for (File f : files) {
                body.append("<li><a href=\"")
                        .append(request.getPath().endsWith("/") ? request.getPath() : request.getPath() + "/")
                        .append(f.getName())
                        .append("\">")
                        .append(f.getName())
                        .append("</a></li>");
            }
            body.append("</ul>");
            body.append("</body></html>");

            response = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.OK, headers, body.toString());
            response.setHeader("Content-Type", "text/html");

            return setStandardHeaders(response);
        } else {
            // Datei gefunden
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[(int) file.length()];
                fis.read(data);

                response = new HTTPResponse<byte[]>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.OK, headers, data);
                response.setHeader("Content-Type", determineContentType(file));
                response.setHeader("Content-Length", String.valueOf(file.length()));

                return setStandardHeaders(response);
            } catch (IOException e) {
                HTTPResponse<String> errorResponse = new HTTPResponse<String>(HTTPServer.HTTP_VERSION, HTTPResponse.HTTPStatusCode.INTERNAL_SERVER_ERROR, headers, "500 - Internal Server Error!");
                e.printStackTrace();
                return setStandardHeaders(errorResponse);
            }
        }
    }

    private String determineContentType(File file) {
        String fileType = file.getName().split("\\.")[1];

        switch (fileType) {
            case "html":
                return "text/html";
            case "jpg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "ico":
                return "image/x-icon";
            case "pdf":
                return "application/pdf";
            default:
                throw new UnsupportedOperationException("File type not supported!");
        }
    }

    private HTTPResponse setStandardHeaders(HTTPResponse response) {
        response.setHeader("Date", Instant.now().toString());
        response.setHeader("Server", "Simple Java HTTP Server");

        return response;
    }

    // Lese den Input Stream vom Client und erstelle ein HTTPRequest Objekt
    private HTTPRequest readHTTPRequest() throws IOException {
        String curLine = readFromClient();
        String status[] = curLine.split(" ");

        Map<String, String> headers = new HashMap<String, String>();

        curLine = readFromClient();
        while (!curLine.isEmpty() && curLine != CRLF) {
            String[] headerParts = curLine.split(": ");
            headers.put(headerParts[0], headerParts[1]);

            curLine = readFromClient();
        }

        HTTPRequest request = new HTTPRequest(
                HTTPRequest.HTTPMethod.parseMethodString(status[0]),
                status[1],
                status[2],
                headers,
                null);

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

    // Lese eine Zeile vom Client InputStream
    private String readFromClient() throws IOException {
        /* Lies die naechste Anfrage-Zeile (request) vom Client
         * ALLE Anfragen vom Client müssen über diese Methode empfangen werden ("Sub-Layer") */

        return inFromClient.readLine();
    }

    // Schreibe eine HTTPResponse zum Client OutputStream
    private void writeToClient(HTTPResponse response) throws IOException {
        writeToClient(response.getHttpVersion() + " " + response.getStatusCode() + " " + response.getStatusMessage());
        response.Headers().forEach((key, value) -> {
            try {
                writeToClient(key + ": " + value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writeToClient("");

        if (response.Body() != null) {
            writeToClient(
                    response.Body() instanceof String ? ((String) response.Body()).getBytes() : (byte[]) response.Body()
            );
        }
    }

    // Schreibe eine Zeile zum Client OutputStream
    private void writeToClient(String line) throws IOException {
        /* Sende eine Antwortzeile zum Client
         * ALLE Antworten an den Client müssen über diese Methode gesendet werden ("Sub-Layer") */
        outToClient.write((line + CRLF).getBytes());
    }

    // Schreibe ein Byte-Array zum Client OutputStream
    private void writeToClient(byte[] data) throws IOException {
        /* Sende Daten zum Client
         * ALLE Antworten an den Client müssen über diese Methode gesendet werden ("Sub-Layer") */
        outToClient.write(data);
    }
}
