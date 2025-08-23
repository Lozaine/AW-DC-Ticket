package com.discordticketbot.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoleParser {

    public static List<Role> parseSupportRoles(String input, Guild guild) {
        Set<String> ids = new LinkedHashSet<>();

        // Extract <@&ID> mentions
        Matcher m = Pattern.compile("<@&(?<id>\\d+)>").matcher(input);
        while (m.find()) {
            ids.add(m.group("id"));
        }

        // Also allow raw IDs separated by spaces/commas
        String stripped = input.replaceAll("<@&\\d+>", " ");
        for (String token : stripped.split("[,\\s]+")) {
            if (token.matches("\\d{5,}")) {
                ids.add(token);
            }
        }

        List<Role> roles = new ArrayList<>();
        for (String id : ids) {
            Role r = guild.getRoleById(id);
            if (r != null) roles.add(r);
        }
        return roles;
    }
}