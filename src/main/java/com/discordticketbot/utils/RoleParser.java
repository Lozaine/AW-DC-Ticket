package com.discordticketbot.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoleParser {
    private static final Pattern ROLE_MENTION_PATTERN = Pattern.compile("<@&(\\d+)>");
    private static final Pattern ROLE_ID_PATTERN = Pattern.compile("\\b(\\d{17,19})\\b");

    /**
     * Parse support roles from various formats:
     * - Role mentions: @Moderator @Helper
     * - Role IDs: 123456789012345678 987654321098765432
     * - Mixed formats with commas/spaces
     */
    public static List<Role> parseSupportRoles(String input, Guild guild) {
        List<Role> roles = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            return roles;
        }

        // First, try to find role mentions
        Matcher mentionMatcher = ROLE_MENTION_PATTERN.matcher(input);
        while (mentionMatcher.find()) {
            String roleId = mentionMatcher.group(1);
            Role role = guild.getRoleById(roleId);
            if (role != null && !roles.contains(role)) {
                roles.add(role);
            }
        }

        // Then, try to find role IDs (snowflakes)
        Matcher idMatcher = ROLE_ID_PATTERN.matcher(input);
        while (idMatcher.find()) {
            String roleId = idMatcher.group(1);
            Role role = guild.getRoleById(roleId);
            if (role != null && !roles.contains(role)) {
                roles.add(role);
            }
        }

        // Finally, try to parse by role name (space/comma separated)
        String[] parts = input.split("[,\\s]+");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty() || ROLE_MENTION_PATTERN.matcher(part).matches() || ROLE_ID_PATTERN.matcher(part).matches()) {
                continue; // Skip already processed mentions/IDs
            }

            // Try to find role by name
            List<Role> foundRoles = guild.getRolesByName(part, true);
            for (Role role : foundRoles) {
                if (!roles.contains(role)) {
                    roles.add(role);
                    break; // Only add the first match
                }
            }
        }

        return roles;
    }

    /**
     * Validate that a role can be used as a support role
     */
    public static boolean isValidSupportRole(Role role) {
        if (role == null) {
            return false;
        }

        // Don't allow @everyone
        if (role.isPublicRole()) {
            return false;
        }

        // Don't allow managed roles (bot roles, booster role, etc.)
        if (role.isManaged()) {
            return false;
        }

        // Don't allow roles that are higher than the bot's highest role
        Guild guild = role.getGuild();
        Role botHighestRole = guild.getSelfMember().getRoles().isEmpty() ?
                guild.getPublicRole() : guild.getSelfMember().getRoles().get(0);

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return false;
        }

        return true;
    }

    /**
     * Format roles for display
     */
    public static String formatRoleList(List<Role> roles) {
        if (roles.isEmpty()) {
            return "No roles configured";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(roles.get(i).getAsMention());
        }
        return sb.toString();
    }

    /**
     * Get role parsing help text
     */
    public static String getParsingHelpText() {
        return "**Role Input Formats:**\n" +
                "• Role mentions: `@Moderator @Helper`\n" +
                "• Role IDs: `123456789012345678 987654321098765432`\n" +
                "• Role names: `Moderator, Helper`\n" +
                "• Mixed: `@Moderator 123456789012345678 Helper`\n\n" +
                "**Tips:**\n" +
                "• Separate multiple roles with spaces or commas\n" +
                "• Role mentions are most reliable\n" +
                "• Bot cannot assign managed roles (other bots)\n" +
                "• @everyone role cannot be used";
    }
}