package com.discordticketbot.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DiscordService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestTemplate restTemplate;
    
    @Value("${BOT_CLIENT_ID}")
    private String botClientId;
    
    public DiscordService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
        this.restTemplate = new RestTemplate();
    }
    
    public List<Map<String, Object>> getUserGuilds(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName()
        );
        
        String accessToken = client.getAccessToken().getTokenValue();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        
        ResponseEntity<List> response = restTemplate.exchange(
            "https://discord.com/api/users/@me/guilds",
            HttpMethod.GET,
            entity,
            List.class
        );
        
        return response.getBody();
    }
    
    public Map<String, Object> getGuild(String guildId, OAuth2AuthenticationToken authentication) {
        // In a real implementation, you would use the bot token to fetch guild information
        // This is a simplified example
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bot " + System.getenv("BOT_TOKEN"));
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://discord.com/api/guilds/" + guildId,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        return response.getBody();
    }
}