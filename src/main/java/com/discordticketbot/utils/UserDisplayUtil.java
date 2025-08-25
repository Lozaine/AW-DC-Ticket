package com.discordticketbot.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class UserDisplayUtil {

    /**
     * Get the best display name for a user
     * Priority: Server Nickname > Global Display Name > Username
     */
    public static String getDisplayName(User user) {
        // For global name, use getGlobalName() if available, otherwise use getName()
        String globalName = user.getGlobalName();
        return globalName != null ? globalName : user.getName();
    }

    /**
     * Get the best display name for a member (includes server nickname)
     */
    public static String getDisplayName(Member member) {
        if (member == null) return "Unknown User";

        // Server nickname takes priority
        String nickname = member.getNickname();
        if (nickname != null) return nickname;

        // Then global display name
        String globalName = member.getUser().getGlobalName();
        if (globalName != null) return globalName;

        // Finally username
        return member.getUser().getName();
    }

    /**
     * Get formatted user identifier with mention and display name
     */
    public static String getFormattedUserInfo(User user) {
        String displayName = getDisplayName(user);
        return user.getAsMention() + " (" + displayName + ")";
    }

    /**
     * Get formatted member identifier with mention and display name
     */
    public static String getFormattedMemberInfo(Member member) {
        if (member == null) return "Unknown User";

        String displayName = getDisplayName(member);
        return member.getAsMention() + " (" + displayName + ")";
    }

    /**
     * Get user identifier for logging (display name + ID)
     */
    public static String getUserLogInfo(User user) {
        if (user == null) return "Unknown User";

        String displayName = getDisplayName(user);
        return displayName + " (" + user.getId() + ")";
    }
}