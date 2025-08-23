package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.PermissionUtil;
import com.discordticketbot.utils.TranscriptUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.File;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketHandler {
    private final Map<String, GuildConfig> guildConfigs;

    public TicketHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void createTicket(ButtonInteractionEvent event, String type, String emoji, Color color) {
        User user = event.getUser();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("❌ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null || !config.isConfigured()) {
            event.reply("❌ This server's ticket system is not configured. Please contact an administrator.")
                    .setEphemeral(true).queue();
            return;
        }

        // Generate channel name with numeric counter
        String baseChannelName = "ticket-" + user.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
        String channelName = generateNextTicketName(guild, baseChannelName);

        // Check if user already has an open ticket
        for (TextChannel existingChannel : guild.getTextChannels()) {
            String topic = existingChannel.getTopic();
            if (topic != null && topic.equals(user.getId())) {
                event.reply("❌ You already have an open ticket: " + existingChannel.getAsMention())
                        .setEphemeral(true).queue();
                return;
            }
        }

        Category category = guild.getCategoryById(config.categoryId);
        if (category == null) {
            event.reply("❌ Support category not found. Please contact an administrator.")
                    .setEphemeral(true).queue();
            return;
        }

        category.createTextChannel(channelName)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(user.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES), null)
                .queue(channel -> {
                    for (String roleId : config.supportRoleIds) {
                        Role sr = guild.getRoleById(roleId);
                        if (sr != null) {
                            channel.upsertPermissionOverride(sr)
                                    .setAllowed(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND,
                                            Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS))
                                    .queue();
                        }
                    }

                    // Store user ID in channel topic for identification
                    channel.getManager().setTopic(user.getId()).queue();

                    EmbedBuilder welcomeEmbed = new EmbedBuilder()
                            .setTitle(emoji + " " + type + " Ticket Created")
                            .setDescription("Hello " + user.getAsMention() + "!\n\n" +
                                    "Thank you for creating a **" + type.toLowerCase() + "** ticket. Please describe your issue in detail and our staff will assist you shortly.\n\n" +
                                    "**Ticket Type:** " + type + "\n" +
                                    "**Created by:** " + user.getAsMention() + "\n" +
                                    "**Created at:** <t:" + (System.currentTimeMillis() / 1000L) + ":F>")
                            .setColor(color)
                            .setThumbnail(user.getAvatarUrl())
                            .setFooter("Use the close button to close this ticket when resolved");

                    channel.sendMessageEmbeds(welcomeEmbed.build())
                            .setActionRow(Button.danger("close_ticket", "🔒 Close Ticket"))
                            .queue();

                    StringBuilder supportMention = new StringBuilder();
                    for (String roleId : config.supportRoleIds)
                        supportMention.append("<@&").append(roleId).append("> ");
                    if (supportMention.length() > 0) {
                        channel.sendMessage(supportMention + " - New " + type.toLowerCase() + " ticket created!")
                                .queue(msg -> msg.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS));
                    }

                    event.reply("✅ Your ticket has been created: " + channel.getAsMention())
                            .setEphemeral(true).queue();
                }, error -> {
                    event.reply("❌ Failed to create ticket. Please contact an administrator.")
                            .setEphemeral(true).queue();
                    error.printStackTrace();
                });
    }

    /**
     * Generates the next available ticket name with persistent global counter
     * Format: ticket-username-001, ticket-username-002, etc.
     * Numbers continue globally even after tickets are deleted
     */
    private String generateNextTicketName(Guild guild, String baseChannelName) {
        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null) {
            // Fallback to old method if config not found
            return baseChannelName + "-001";
        }

        // Get next ticket number from persistent counter
        int nextNumber = config.getNextTicketNumber();
        return String.format("%s-%03d", baseChannelName, nextNumber);
    }

    public void showCloseOptions(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        User user = event.getUser();

        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null) return;

        if (!PermissionUtil.hasStaffPermission(event.getMember(), config) && !PermissionUtil.isTicketOwner(channel, user)) {
            event.reply("❌ Only staff members or the ticket owner can close this ticket.")
                    .setEphemeral(true).queue();
            return;
        }

        EmbedBuilder closeEmbed = new EmbedBuilder()
                .setTitle("🔒 Close Ticket Confirmation")
                .setDescription("**" + user.getAsMention() + "** wants to close this ticket.\n\nPlease choose an action:")
                .addField("🔓 Re-open", "Re-open this ticket for further assistance", true)
                .addField("📄 Transcript", "Generate and save transcript to logs", true)
                .addField("🗑️ Delete", "Permanently delete this ticket", true)
                .setColor(Color.YELLOW)
                .setFooter("This ticket is now closed - choose an action above");

        if (PermissionUtil.isTicketOwner(channel, user)) {
            channel.getManager()
                    .putMemberPermissionOverride(user.getIdLong(),
                            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY),
                            EnumSet.of(Permission.MESSAGE_SEND))
                    .queue();
        }

        event.reply("✅ Ticket has been closed!")
                .addEmbeds(closeEmbed.build())
                .addActionRow(
                        Button.success("reopen_ticket", "🔓 Re-open"),
                        Button.primary("generate_transcript", "📄 Transcript"),
                        Button.danger("delete_ticket", "🗑️ Delete")
                ).queue();
    }

    public void reopenTicket(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null) {
            event.reply("❌ Configuration not found.").setEphemeral(true).queue();
            return;
        }

        if (!PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can re-open tickets.").setEphemeral(true).queue();
            return;
        }

        String userId = channel.getTopic();
        if (userId == null || userId.isEmpty()) {
            event.reply("❌ Could not identify the original ticket owner.").setEphemeral(true).queue();
            return;
        }

        channel.getManager()
                .putMemberPermissionOverride(Long.parseLong(userId),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES),
                        null)
                .queue();

        EmbedBuilder reopenEmbed = new EmbedBuilder()
                .setTitle("🔓 Ticket Re-opened")
                .setDescription("This ticket has been re-opened by " + event.getUser().getAsMention() +
                        "\n\n**Re-opened at:** <t:" + (System.currentTimeMillis() / 1000L) + ":F>")
                .setColor(Color.GREEN)
                .setFooter("You can continue the conversation");

        event.editMessage("")
                .setEmbeds(reopenEmbed.build())
                .setActionRow(Button.danger("close_ticket", "🔒 Close Ticket"))
                .queue();
    }

    public void generateAndSendTranscript(ButtonInteractionEvent event) {
        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null) return;

        if (!PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can generate transcripts.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        TextChannel channel = event.getChannel().asTextChannel();

        channel.getHistory().retrievePast(100).queue(messages -> {
            try {
                String transcriptContent = TranscriptUtil.createTranscriptContent(channel, messages);
                File transcriptFile = TranscriptUtil.saveTranscriptToFile(channel, transcriptContent);

                TextChannel transcriptChannel = event.getGuild().getTextChannelById(config.transcriptChannelId);
                if (transcriptChannel != null) {
                    EmbedBuilder transcriptEmbed = new EmbedBuilder()
                            .setTitle("📄 Ticket Transcript")
                            .addField("Ticket Channel", channel.getName(), true)
                            .addField("Generated by", event.getUser().getAsMention(), true)
                            .addField("Generated at", "<t:" + (System.currentTimeMillis() / 1000L) + ":F>", true)
                            .addField("Message Count", String.valueOf(messages.size()), true)
                            .setColor(Color.BLUE)
                            .setFooter("Transcript saved for record keeping");

                    transcriptChannel.sendMessageEmbeds(transcriptEmbed.build())
                            .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(transcriptFile))
                            .queue();
                }

                event.getHook().sendMessage("✅ Transcript generated and saved to logs channel!").queue();

            } catch (Exception e) {
                event.getHook().sendMessage("❌ Error generating transcript: " + e.getMessage()).queue();
                e.printStackTrace();
            }
        });
    }

    public void deleteTicket(ButtonInteractionEvent event) {
        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null) return;

        if (!PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can delete tickets.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        EmbedBuilder deleteEmbed = new EmbedBuilder()
                .setTitle("🗑️ Ticket Deleting")
                .setDescription("This ticket will be permanently deleted in **10 seconds**.\n\n" +
                        "**Deleted by:** " + event.getUser().getAsMention() + "\n" +
                        "**Deleted at:** <t:" + (System.currentTimeMillis() / 1000L) + ":F>")
                .setColor(Color.RED)
                .setFooter("This action cannot be undone");

        event.editMessage("").setEmbeds(deleteEmbed.build()).setComponents().queue();

        channel.delete().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}