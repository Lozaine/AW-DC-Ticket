package com.discordticketbot.utils;

import com.discordticketbot.config.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class PermissionUtil {

    public static boolean hasAdminPermission(Member member) {
        if (member == null) return false;
        return member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner();
    }

    public static boolean hasStaffPermission(Member member, GuildConfig config) {
        if (member == null || config == null) return false;
        for (String roleId : config.supportRoleIds) {
            if (member.getRoles().stream().anyMatch(role -> role.getId().equals(roleId))) return true;
        }
        return member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner();
    }

    public static boolean isTicketOwner(TextChannel channel, User user) {
        String userId = channel.getTopic();
        return userId != null && userId.equals(user.getId());
    }
}