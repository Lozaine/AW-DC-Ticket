package com.discordticketbot.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;

@Controller
@RequestMapping
public class TranscriptWebController {

    @Value("${app.base-url:https://aw-dc-ticket-production.up.railway.app}")
    private String appBaseUrl;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("baseUrl", appBaseUrl);
        return "dashboard";
    }

    @GetMapping("/transcripts/{filename}")
    public ResponseEntity<FileSystemResource> getTranscript(@PathVariable("filename") String filename) {
        if (filename.contains("..") || filename.contains("\\") || filename.startsWith("/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        File file = new File("transcripts", filename);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        MediaType mediaType = filename.endsWith(".html")
                ? MediaType.TEXT_HTML
                : (filename.endsWith(".txt") ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_OCTET_STREAM);

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(mediaType)
                .body(resource);
    }

    public String getPublicBaseUrl() {
        return appBaseUrl;
    }

    public String buildPublicUrl(String transcriptFileName) {
        String base = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return base + "/transcripts/" + transcriptFileName;
    }
}