package com.discordticketbot.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.discordticketbot.bot.TicketBot;

@Controller
public class DashboardController {
    
    private final TicketBot ticketBot;
    
    @Autowired
    public DashboardController(TicketBot ticketBot) {
        this.ticketBot = ticketBot;
    }
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Discord Ticket Bot Dashboard");
        return "home";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            model.addAttribute("username", principal.getAttribute("username"));
            model.addAttribute("userId", principal.getAttribute("id"));
            model.addAttribute("avatar", principal.getAttribute("avatar"));
        }
        
        // In a real implementation, you would fetch the guilds where the bot is installed
        // and the user has admin permissions
        
        return "dashboard";
    }
    
    @GetMapping("/servers/{guildId}")
    public String serverConfig(@PathVariable String guildId, 
                              @AuthenticationPrincipal OAuth2User principal, 
                              Model model) {
        // Add server-specific data to the model
        model.addAttribute("guildId", guildId);
        
        // In a real implementation, you would fetch the guild configuration
        // from your database
        
        return "server-config";
    }
    
    @GetMapping("/servers/{guildId}/tickets")
    public String tickets(@PathVariable String guildId, 
                         @AuthenticationPrincipal OAuth2User principal, 
                         Model model) {
        // Add ticket data to the model
        model.addAttribute("guildId", guildId);
        
        // In a real implementation, you would fetch the tickets
        // from your database
        
        return "tickets";
    }
}