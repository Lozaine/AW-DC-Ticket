package com.discordticketbot.web;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

public class WebDashboard {

    private static Javalin app;

    public static void start(int port) {
        if (app != null) return;

        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost()));
        });

        app.get("/", WebDashboard::renderIndex);
        app.get("/transcripts", WebDashboard::renderIndex);
        app.get("/transcripts/view/{name}", WebDashboard::viewTranscript);
        app.get("/transcripts/download/{name}", WebDashboard::downloadTranscript);

        String host = "0.0.0.0"; // bind all interfaces for Railway
        app.start(host, port);
    }

    private static void renderIndex(Context ctx) {
        File dir = new File("transcripts");
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".html"));
        if (files == null) files = new File[0];

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        html.append("<title>Ticket Transcripts</title>");
        html.append("<style>body{font-family:Segoe UI,Arial,sans-serif;background:#0b0c10;color:#c5c6c7;margin:0;padding:20px} .wrap{max-width:960px;margin:0 auto} h1{color:#66fcf1} a{color:#66fcf1;text-decoration:none} .card{background:#1f2833;padding:16px;border-radius:8px;margin-bottom:10px;display:flex;justify-content:space-between;align-items:center} .btn{background:#45a29e;color:#0b0c10;padding:8px 12px;border-radius:6px;margin-left:8px} .row{display:flex;gap:8px;align-items:center}</style>");
        html.append("</head><body><div class='wrap'>");
        html.append("<h1>Ticket Transcripts</h1>");
        html.append("<p>Total: ").append(files.length).append("</p>");

        for (File f : files) {
            String name = f.getName();
            String enc = URLEncoder.encode(name, StandardCharsets.UTF_8);
            html.append("<div class='card'>");
            html.append("<div>").append(name).append("<br><small>").append(f.length()).append(" bytes</small></div>");
            html.append("<div class='row'>");
            html.append("<a class='btn' href='/transcripts/view/").append(enc).append("' target='_blank'>View</a>");
            html.append("<a class='btn' href='/transcripts/download/").append(enc).append("'>Download</a>");
            html.append("</div></div>");
        }

        html.append("</div></body></html>");
        ctx.contentType("text/html").result(html.toString());
    }

    private static void viewTranscript(Context ctx) {
        String name = ctx.pathParam("name");
        if (name == null) { ctx.status(400).result("Bad Request"); return; }
        File file = safeFile(name);
        if (file == null || !file.exists()) { ctx.status(404).result("Not Found"); return; }
        try {
            ctx.contentType("text/html").result(java.nio.file.Files.newInputStream(file.toPath()));
        } catch (java.io.IOException e) {
            ctx.status(500).result("Failed to read file");
        }
    }

    private static void downloadTranscript(Context ctx) {
        String name = ctx.pathParam("name");
        if (name == null) { ctx.status(400).result("Bad Request"); return; }
        File file = safeFile(name);
        if (file == null || !file.exists()) { ctx.status(404).result("Not Found"); return; }
        try {
            ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
              .contentType("application/octet-stream")
              .result(java.nio.file.Files.newInputStream(file.toPath()));
        } catch (java.io.IOException e) {
            ctx.status(500).result("Failed to read file");
        }
    }

    private static File safeFile(String name) {
        try {
            String clean = name.replace("..", "");
            File file = new File("transcripts", clean);
            if (!file.getCanonicalPath().startsWith(new File("transcripts").getCanonicalPath())) {
                return null;
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}


