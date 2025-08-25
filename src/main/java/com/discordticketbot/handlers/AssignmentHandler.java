package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Map;

public class AssignmentHandler {
    private final Map<String, GuildConfig> guildConfigs;

    public AssignmentHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void handle(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null || !PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can assign tickets.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        if (channel.getTopic() == null || channel.getTopic().isEmpty()) {
            event.reply("❌ This doesn't appear to be a valid ticket channel.").setEphemeral(true).queue();
            return;
        }

        Member assignee = event.getOption("member").getAsMember();
        if (assignee == null) {
            event.reply("❌ Could not resolve the specified member.").setEphemeral(true).queue();
            return;
        }

        // Ensure assignee is staff (has support role or admin)
        if (!PermissionUtil.hasStaffPermission(assignee, config)) {
            event.reply("❌ The selected member is not a staff member.").setEphemeral(true).queue();
            return;
        }

        // Grant channel permissions to assignee explicitly
        channel.upsertPermissionOverride(assignee)
                .setAllowed(net.dv8tion.jda.api.Permission.VIEW_CHANNEL, net.dv8tion.jda.api.Permission.MESSAGE_SEND, net.dv8tion.jda.api.Permission.MESSAGE_HISTORY, net.dv8tion.jda.api.Permission.MESSAGE_ATTACH_FILES, net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)
                .queue();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Ticket Assigned")
                .setDescription("This ticket has been assigned to " + assignee.getAsMention() + ".\n\n" +
                        "They now have explicit access to this channel.")
                .setColor(Color.CYAN)
                .setFooter("Assigned by " + event.getUser().getName());

        channel.sendMessageEmbeds(embed.build()).queue();
        event.reply("✅ Assigned to " + assignee.getEffectiveName() + ".").setEphemeral(true).queue();
    }
}


