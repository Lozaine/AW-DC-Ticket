package com.discordticketbot.utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple HTTP server utility for serving HTML transcripts.
 * This provides a basic HTTP server that can serve HTML files from the transcripts directory.
 */
public class HttpServerUtil {
    private static final int DEFAULT_PORT = 8080;
    private static final String TRANSCRIPTS_DIR = "transcripts";
    private static final AtomicBoolean serverRunning = new AtomicBoolean(false);
    private static ServerSocket serverSocket;
    private static ExecutorService executorService;
    private static Thread serverThread;

    /**
     * Starts the HTTP server on the specified port.
     */
    public static void startServer(int port) {
        if (serverRunning.get()) {
            System.out.println("HTTP server is already running.");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            executorService = Executors.newCachedThreadPool();
            serverRunning.set(true);
            
            System.out.println("🚀 HTTP server started on port " + port);
            System.out.println("📄 Transcripts will be available at: http://localhost:" + port + "/transcript/{uniqueId}");
            
            serverThread = new Thread(() -> {
                while (serverRunning.get() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        executorService.submit(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (serverRunning.get()) {
                            System.err.println("Error accepting client connection: " + e.getMessage());
                        }
                    }
                }
            });
            serverThread.start();
            
        } catch (IOException e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            serverRunning.set(false);
        }
    }

    /**
     * Starts the HTTP server on the default port.
     */
    public static void startServer() {
        startServer(DEFAULT_PORT);
    }

    /**
     * Stops the HTTP server.
     */
    public static void stopServer() {
        if (!serverRunning.get()) {
            return;
        }

        serverRunning.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

        System.out.println("🛑 HTTP server stopped.");
    }

    /**
     * Checks if the server is running.
     */
    public static boolean isServerRunning() {
        return serverRunning.get();
    }

    /**
     * Handles client connections.
     */
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Read HTTP request
            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            if (!"GET".equals(method)) {
                sendErrorResponse(out, 405, "Method Not Allowed");
                return;
            }

            // Handle different paths
            if (path.startsWith("/transcript/")) {
                handleTranscriptRequest(out, path);
            } else if ("/health".equals(path)) {
                handleHealthCheck(out);
            } else if ("/".equals(path)) {
                handleRootRequest(out);
            } else {
                sendErrorResponse(out, 404, "Not Found");
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Handles transcript requests.
     */
    private static void handleTranscriptRequest(OutputStream out, String path) throws IOException {
        String uniqueId = path.substring("/transcript/".length());
        if (uniqueId.isEmpty()) {
            sendErrorResponse(out, 400, "Missing transcript ID");
            return;
        }

        File transcriptFile = findTranscriptFile(uniqueId);
        if (transcriptFile == null || !transcriptFile.exists()) {
            sendErrorResponse(out, 404, "Transcript not found");
            return;
        }

        // Send transcript file
        sendFileResponse(out, transcriptFile, "text/html");
    }

    /**
     * Handles health check requests.
     */
    private static void handleHealthCheck(OutputStream out) throws IOException {
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 28\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n" +
                "Transcript service is running!";
        out.write(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Handles root requests.
     */
    private static void handleRootRequest(OutputStream out) throws IOException {
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n" +
                "<!DOCTYPE html><html><head><title>Discord Ticket Bot Transcript Server</title></head>" +
                "<body><h1>Discord Ticket Bot Transcript Server</h1>" +
                "<p>Use <code>/transcript/{uniqueId}</code> to access transcripts.</p>" +
                "<p>Use <code>/health</code> for health check.</p></body></html>";
        out.write(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a file response.
     */
    private static void sendFileResponse(OutputStream out, File file, String contentType) throws IOException {
        byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
        
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Cache-Control: public, max-age=3600\r\n" +
                "\r\n";
        
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(fileContent);
    }

    /**
     * Sends an error response.
     */
    private static void sendErrorResponse(OutputStream out, int statusCode, String statusText) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n" +
                "Error " + statusCode + ": " + statusText;
        out.write(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Finds a transcript file by unique ID.
     */
    private static File findTranscriptFile(String uniqueId) {
        File transcriptsDir = new File(TRANSCRIPTS_DIR);
        if (!transcriptsDir.exists()) {
            return null;
        }

        File[] files = transcriptsDir.listFiles((dir, name) -> name.endsWith(".html"));
        if (files != null) {
            for (File file : files) {
                if (file.getName().contains(uniqueId)) {
                    return file;
                }
            }
        }
        return null;
    }
}
