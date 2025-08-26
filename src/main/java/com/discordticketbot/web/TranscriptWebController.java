package com.discordticketbot.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Minimal HTTP server to serve transcript files from the local "transcripts" directory.
 *
 * - Port is taken from env PORT (Railway), default 8080.
 * - Public base URL can be overridden via env PUBLIC_BASE_URL (e.g. https://aw-dc-ticket-production.up.railway.app).
 * - Serves: GET /health and GET /transcripts/{filename}
 */
public class TranscriptWebController {
    private static volatile HttpServer server;
    private static volatile String publicBaseUrl;
    private static volatile int boundPort;

    public static synchronized void startIfNeeded() {
        if (server != null) return;

        try {
            int port = getPortFromEnv();
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // Health endpoint
            httpServer.createContext("/health", exchange -> {
                byte[] ok = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, ok.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(ok);
                }
            });

            // Static transcripts: /transcripts/{filename}
            httpServer.createContext("/transcripts", new StaticTranscriptHandler());

            httpServer.setExecutor(null);
            httpServer.start();

            server = httpServer;
            boundPort = port;
            publicBaseUrl = computePublicBaseUrl(port);
            System.out.println("✅ Transcript web server started on port " + port + "; base URL: " + publicBaseUrl);
        } catch (IOException e) {
            System.err.println("❌ Failed to start transcript web server: " + e.getMessage());
        }
    }

    public static String getPublicBaseUrl() {
        if (publicBaseUrl == null) {
            publicBaseUrl = computePublicBaseUrl(boundPort == 0 ? 8080 : boundPort);
        }
        return publicBaseUrl;
    }

    public static String buildPublicUrl(String transcriptFileName) {
        return getPublicBaseUrl() + "/transcripts/" + transcriptFileName;
    }

    private static int getPortFromEnv() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }

    private static String computePublicBaseUrl(int port) {
        String env = System.getenv("PUBLIC_BASE_URL");
        if (env != null && !env.isBlank()) {
            return env.replaceAll("/$", "");
        }
        return "http://localhost:" + port;
    }

    private static class StaticTranscriptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            String path = Optional.ofNullable(exchange.getRequestURI().getPath()).orElse("");
            // Expected: /transcripts/<name>
            String[] parts = path.split("/", 3);
            if (parts.length < 3 || parts[2].isBlank()) {
                sendText(exchange, 400, "Bad Request");
                return;
            }

            String fileName = parts[2];
            // Security: disallow path traversal
            if (fileName.contains("..") || fileName.contains("\\") || fileName.startsWith("/")) {
                sendText(exchange, 400, "Invalid filename");
                return;
            }

            File baseDir = new File("transcripts");
            File target = new File(baseDir, fileName);
            if (!target.exists() || !target.isFile()) {
                sendText(exchange, 404, "Not Found");
                return;
            }

            Headers headers = exchange.getResponseHeaders();
            String contentType = guessContentType(target.getName());
            headers.add("Content-Type", contentType);
            headers.add("Cache-Control", "public, max-age=31536000, immutable");

            exchange.sendResponseHeaders(200, target.length());
            try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(target)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    os.write(buf, 0, r);
                }
            }
        }

        private String guessContentType(String name) {
            String type = URLConnection.guessContentTypeFromName(name);
            if (type != null) return type;
            if (name.endsWith(".html")) return "text/html; charset=utf-8";
            if (name.endsWith(".txt")) return "text/plain; charset=utf-8";
            return "application/octet-stream";
        }

        private void sendText(HttpExchange exchange, int code, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}


