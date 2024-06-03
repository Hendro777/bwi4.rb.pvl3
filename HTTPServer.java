/*
 * HTTPServer.java
 *
 * Version 1.0
 * Autor: H. Lind
 * Zweck: HTTP-Server, der Verbindungsanfragen entgegennimmt
 *       Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine HTTP-Anfrage bearbeitet
 */

import java.net.*;
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

