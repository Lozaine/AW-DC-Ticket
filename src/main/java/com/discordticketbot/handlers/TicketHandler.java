package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.database.TicketLogDAO;
import com.discordticketbot.utils.ErrorLogger;
import com.discordticketbot.utils.PermissionUtil;
import com.discordticketbot.utils.TranscriptUtil;
import com.discordticketbot.utils.UserDisplayUtil;
import com.discordticketbot.utils.TimestampUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.io.File;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.List;
import net.dv8tion.jda.api.entities.Message;

public class TicketHandler {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketLogDAO ticketLogDAO;
    private final ErrorLogger errorLogger;
    private static final Pattern TICKET_PATTERN = Pattern.compile("^ticket-(.+)-(\\d{3})$");

    public TicketHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketLogDAO = new TicketLogDAO();
        this.errorLogger = new ErrorLogger(guildConfigs);
    }

    public void createTicket(ButtonInteractionEvent event, String type, String emoji, Color color) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        User user = event.getUser();
        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null || !config.isConfigured()) {
            event.reply("❌ This server's ticket system isn't configured. Please contact an administrator.").setEphemeral(true).queue();
            return;
        }

        // Check for existing open tickets.
        if (hasOpenTicket(guild, user)) {
            event.reply("❌ You already have an open ticket.").setEphemeral(true).queue();
            return;
        }

        Category category = guild.getCategoryById(config.categoryId);
        if (category == null) {
            event.reply("❌ Support category not found. Please contact an administrator.").setEphemeral(true).queue();
            return;
        }

        String baseChannelName = "ticket-" + user.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
        int ticketNumber = config.getNextTicketNumber();
        String channelName = String.format("%s-%03d", baseChannelName, ticketNumber);

        category.createTextChannel(channelName)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(user.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES),
                        null)
                .queue(channel -> {
                    setupChannelPermissions(channel, config);
                    channel.getManager().setTopic(user.getId()).queue();

                    ticketLogDAO.logTicketCreated(guild.getId(), channel.getId(), channel.getName(), user.getId(), type, ticketNumber);

                    sendWelcomeMessage(channel, user, type, ticketNumber, emoji, color);
                    notifySupportTeam(channel, config, type);

                    event.reply("✅ Your ticket has been created: " + channel.getAsMention()).setEphemeral(true).queue();
                }, error -> {
                    errorLogger.logError(guild, "Create Ticket",
                            "Failed to create " + type + " ticket for user " + user.getName() + ": " + error.getMessage(),
                            new Exception(error)); // Fix: Wrap Throwable in Exception
                    event.reply("❌ Failed to create ticket. Please contact an administrator.").setEphemeral(true).queue();
                });
    }

    public void requestCloseReason(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        GuildConfig config = guildConfigs.get(Objects.requireNonNull(event.getGuild()).getId());

        if (!canCloseTicket(event, channel, config)) {
            event.reply("❌ Only staff members or the ticket owner can close this ticket.")
                    .setEphemeral(true).queue();
            return;
        }

        Modal modal = Modal.create("close_reason_modal", "Close Ticket")
                .addActionRow(TextInput.create("close_reason", "Reason for closing", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Please provide a reason for closing this ticket...")
                        .setRequired(false)
                        .setMaxLength(1000)
                        .build())
                .build();
        event.replyModal(modal).queue();
    }

    public void showCloseOptions(ButtonInteractionEvent event) {
        showCloseOptionsWithReason(event, "No reason provided");
    }

    public void handleCloseReasonModal(ModalInteractionEvent event) {
        String reason = event.getValue("close_reason").getAsString();
        if (reason.trim().isEmpty()) {
            reason = "No reason provided";
        }

        TextChannel channel = event.getChannel().asTextChannel();
        GuildConfig config = guildConfigs.get(Objects.requireNonNull(event.getGuild()).getId());

        if (!canCloseTicket(event, channel, config)) {
            event.reply("❌ Only staff members or the ticket owner can close this ticket.")
                    .setEphemeral(true).queue();
            return;
        }

        ticketLogDAO.logTicketClosedWithReason(channel.getId(), event.getUser().getId(), reason);

        if (PermissionUtil.isTicketOwner(channel, event.getUser())) {
            channel.getManager()
                    .putMemberPermissionOverride(event.getUser().getIdLong(),
                            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY),
                            EnumSet.of(Permission.MESSAGE_SEND))
                    .queue();
        }

        sendClosedTicketMessage(event, reason);
    }

    private void sendClosedTicketMessage(ModalInteractionEvent event, String reason) {
        EmbedBuilder closeEmbed = new EmbedBuilder()
                .setTitle("🔒 Ticket Closed")
                .setDescription("**" + UserDisplayUtil.getFormattedUserInfo(event.getUser()) + "** has closed this ticket.\n\n" +
                        "**Close Reason:** " + reason + "\n" +
                        "**Closed at:** " + TimestampUtil.getCurrentTimestampForEmbeds() + "\n\n" +
                        "Please choose an action:")
                .addField("🔓 Re-open", "Re-open this ticket for further assistance", true)
                .addField("📄 Transcript", "Generate and save transcript to logs", true)
                .addField("🗑️ Delete", "Permanently delete this ticket", true)
                .setColor(Color.YELLOW)
                .setFooter("This ticket is now closed - choose an action above");

        event.reply("✅ Ticket has been closed!")
                .setEmbeds(closeEmbed.build())
                .addActionRow(
                        Button.success("reopen_ticket", "🔓 Re-open"),
                        Button.primary("generate_transcript", "📄 Transcript"),
                        Button.danger("delete_ticket", "🗑️ Delete")
                ).queue();
    }

    public void showCloseOptionsWithReason(ButtonInteractionEvent event, String closeReason) {
        TextChannel channel = event.getChannel().asTextChannel();
        User user = event.getUser();
        GuildConfig config = guildConfigs.get(Objects.requireNonNull(event.getGuild()).getId());

        if (!canCloseTicket(event, channel, config)) {
            event.reply("❌ Only staff members or the ticket owner can close this ticket.").setEphemeral(true).queue();
            return;
        }

        ticketLogDAO.logTicketClosedWithReason(channel.getId(), user.getId(), closeReason);

        if (PermissionUtil.isTicketOwner(channel, user)) {
            channel.getManager()
                    .putMemberPermissionOverride(user.getIdLong(),
                            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY),
                            EnumSet.of(Permission.MESSAGE_SEND))
                    .queue();
        }

        EmbedBuilder closeEmbed = new EmbedBuilder()
                .setTitle("🔒 Ticket Closed")
                .setDescription("**" + UserDisplayUtil.getFormattedUserInfo(user) + "** has closed this ticket.\n\n" +
                        "**Close Reason:** " + closeReason + "\n" +
                        "**Closed at:** " + TimestampUtil.getCurrentTimestampForEmbeds() + "\n\n" +
                        "Please choose an action:")
                .addField("🔓 Re-open", "Re-open this ticket for further assistance", true)
                .addField("📄 Transcript", "Generate and save transcript to logs", true)
                .addField("🗑️ Delete", "Permanently delete this ticket", true)
                .setColor(Color.YELLOW)
                .setFooter("This ticket is now closed - choose an action above");

        event.editMessage("✅ Ticket has been closed!")
                .setEmbeds(closeEmbed.build())
                .setActionRow(
                        Button.success("reopen_ticket", "🔓 Re-open"),
                        Button.primary("generate_transcript", "📄 Transcript"),
                        Button.danger("delete_ticket", "🗑️ Delete")
                ).queue();
    }

    public void reopenTicket(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();
        if (guild == null) return;

        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null || !PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can re-open tickets.").setEphemeral(true).queue();
            return;
        }

        String userId = channel.getTopic();
        if (userId == null || userId.isEmpty()) {
            event.reply("❌ Could not identify the original ticket owner.").setEphemeral(true).queue();
            return;
        }

        try {
            ticketLogDAO.logTicketReopened(channel.getId(), event.getUser().getId());

            channel.getManager()
                    .putMemberPermissionOverride(Long.parseLong(userId),
                            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES),
                            null)
                    .queue();

            EmbedBuilder reopenEmbed = new EmbedBuilder()
                    .setTitle("🔓 Ticket Re-opened")
                    .setDescription("This ticket has been re-opened by " + UserDisplayUtil.getFormattedUserInfo(event.getUser()) +
                            "\n\n**Re-opened at:** " + TimestampUtil.getCurrentTimestampForEmbeds())
                    .setColor(Color.GREEN)
                    .setFooter("You can continue the conversation");

            event.editMessage("").setEmbeds(reopenEmbed.build())
                    .setActionRow(Button.danger("close_ticket", "🔒 Close Ticket")).queue();
        } catch (Exception e) {
            errorLogger.logError(guild, "Reopen Ticket",
                    "Failed to reopen ticket " + channel.getName() + ": " + e.getMessage(), e);
            event.reply("❌ Failed to reopen ticket.").setEphemeral(true).queue();
        }
    }

    public void generateAndSendTranscript(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        GuildConfig config = guildConfigs.get(guild.getId());

        if (config == null || !PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can generate transcripts.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        TextChannel channel = event.getChannel().asTextChannel();

        channel.getHistory().retrievePast(100).queue(messages -> {
            try {
                // Only generate TXT transcript initially
                String transcriptContent = TranscriptUtil.createTranscriptContent(channel, messages);
                File transcriptFile = TranscriptUtil.saveTranscriptToFile(channel, transcriptContent);

                TextChannel transcriptChannel = guild.getTextChannelById(config.transcriptChannelId);
                if (transcriptChannel != null) {
                    // Send TXT transcript with Direct Link button
                    sendTranscriptWithDirectLinkButton(transcriptChannel, event, channel, messages.size(), transcriptFile, messages);
                    event.getHook().sendMessage("✅ Transcript generated and saved to logs channel! Click 'Direct Link' to generate HTML version.").queue();
                } else {
                    event.getHook().sendMessage("❌ Transcript log channel not found. Please contact an administrator.").queue();
                }
            } catch (Exception e) {
                errorLogger.logError(guild, "Generate Transcript",
                        "Failed to generate transcript for " + channel.getName() + ": " + e.getMessage(), e);
                event.getHook().sendMessage("❌ Error generating transcript: " + e.getMessage()).queue();
            }
        });
    }

    public void generateHtmlTranscript(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        GuildConfig config = guildConfigs.get(guild.getId());

        if (config == null || !PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can generate HTML transcripts.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        TextChannel channel = event.getChannel().asTextChannel();

        channel.getHistory().retrievePast(100).queue(messages -> {
            try {
                // Generate HTML transcript
                String html = TranscriptUtil.createHtmlTranscript(channel, messages);
                File htmlFile = TranscriptUtil.saveHtmlTranscriptToFile(channel, html);

                // Create a simple web server to serve the HTML file
                String htmlUrl = TranscriptUtil.serveHtmlTranscript(htmlFile, channel.getName());
                
                // Send the HTML link to the user
                EmbedBuilder htmlEmbed = new EmbedBuilder()
                        .setTitle("🔗 HTML Transcript Generated")
                        .setDescription("Your HTML transcript is ready!")
                        .addField("Ticket Channel", channel.getName(), true)
                        .addField("Generated by", UserDisplayUtil.getFormattedUserInfo(event.getUser()), true)
                        .addField("Generated at", TimestampUtil.getCurrentTimestampForEmbeds(), true)
                        .setColor(Color.GREEN)
                        .setFooter("Click the button below to view the HTML transcript");

                event.getHook().sendMessageEmbeds(htmlEmbed.build())
                        .addActionRow(Button.link(htmlUrl, "🔗 View HTML Transcript"))
                        .queue();

            } catch (Exception e) {
                errorLogger.logError(guild, "Generate HTML Transcript",
                        "Failed to generate HTML transcript for " + channel.getName() + ": " + e.getMessage(), e);
                event.getHook().sendMessage("❌ Error generating HTML transcript: " + e.getMessage()).queue();
            }
        });
    }

    public void deleteTicket(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        GuildConfig config = guildConfigs.get(guild.getId());

        if (config == null || !PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can delete tickets.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        try {
            ticketLogDAO.logTicketDeleted(channel.getId(), event.getUser().getId());

            EmbedBuilder deleteEmbed = new EmbedBuilder()
                    .setTitle("🗑️ Ticket Deleting")
                    .setDescription("This ticket will be permanently deleted in **10 seconds**.\n\n" +
                            "**Deleted by:** " + UserDisplayUtil.getFormattedUserInfo(event.getUser()) + "\n" +
                            "**Deleted at:** " + TimestampUtil.getCurrentTimestampForEmbeds())
                    .setColor(Color.RED)
                    .setFooter("This action cannot be undone");

            event.editMessage("").setEmbeds(deleteEmbed.build()).setComponents().queue();

            channel.delete().queueAfter(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            errorLogger.logError(guild, "Delete Ticket",
                    "Failed to delete ticket " + channel.getName() + ": " + e.getMessage(), e);
            event.reply("❌ Failed to delete ticket.").setEphemeral(true).queue();
        }
    }

    private boolean hasOpenTicket(Guild guild, User user) {
        return guild.getTextChannels().stream()
                .anyMatch(c -> user.getId().equals(c.getTopic()));
    }

    // For Button interactions
    private boolean canCloseTicket(ButtonInteractionEvent event, TextChannel channel, GuildConfig config) {
        if (config == null) return false;
        return PermissionUtil.hasStaffPermission(event.getMember(), config)
                || PermissionUtil.isTicketOwner(channel, event.getUser());
    }

    // For Modal interactions
    private boolean canCloseTicket(ModalInteractionEvent event, TextChannel channel, GuildConfig config) {
        if (config == null) return false;
        return PermissionUtil.hasStaffPermission(event.getMember(), config)
                || PermissionUtil.isTicketOwner(channel, event.getUser());
    }


    private void setupChannelPermissions(TextChannel channel, GuildConfig config) {
        for (String roleId : config.supportRoleIds) {
            Role sr = channel.getGuild().getRoleById(roleId);
            if (sr != null) {
                channel.upsertPermissionOverride(sr)
                        .setAllowed(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS))
                        .queue();
            }
        }
    }

    private void sendWelcomeMessage(TextChannel channel, User user, String type, int ticketNumber, String emoji, Color color) {
        EmbedBuilder welcomeEmbed = new EmbedBuilder()
                .setTitle(emoji + " " + type + " Ticket Created")
                .setDescription("Hello " + user.getAsMention() + "!\n\n" +
                        "Thank you for creating a **" + type.toLowerCase() + "** ticket. Please describe your issue in detail and our staff will assist you shortly.\n\n" +
                        "**Ticket Type:** " + type + "\n" +
                        "**Ticket Number:** #" + ticketNumber + "\n" +
                        "**Created by:** " + UserDisplayUtil.getFormattedUserInfo(user) + "\n" +
                        "**Created at:** " + TimestampUtil.getCurrentTimestampForEmbeds())
                .setColor(color)
                .setThumbnail(user.getAvatarUrl())
                .setFooter("Use the close button to close this ticket when resolved");

        channel.sendMessageEmbeds(welcomeEmbed.build())
                .setActionRow(Button.danger("close_ticket", "🔒 Close Ticket"))
                .queue();
    }

    private void notifySupportTeam(TextChannel channel, GuildConfig config, String type) {
        StringBuilder supportMention = new StringBuilder();
        for (String roleId : config.supportRoleIds) {
            supportMention.append("<@&").append(roleId).append("> ");
        }
        if (supportMention.length() > 0) {
            channel.sendMessage(supportMention + "- New " + type.toLowerCase() + " ticket created!")
                    .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private void sendTranscriptEmbed(TextChannel transcriptChannel, ButtonInteractionEvent event, TextChannel sourceChannel, int messageCount, File transcriptFile, File htmlFile) {
        EmbedBuilder transcriptEmbed = new EmbedBuilder()
                .setTitle("📄 Ticket Transcript")
                .addField("Ticket Channel", sourceChannel.getName(), true)
                .addField("Generated by", UserDisplayUtil.getFormattedUserInfo(event.getUser()), true)
                .addField("Generated at", TimestampUtil.getCurrentTimestampForEmbeds(), true)
                .addField("Message Count", String.valueOf(messageCount), true)
                .setColor(Color.BLUE)
                .setFooter("Transcript saved for record keeping");

        var action = transcriptChannel.sendMessageEmbeds(transcriptEmbed.build())
                .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(transcriptFile));
        if (htmlFile != null) {
            action = action.addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(htmlFile));
        }
        action.queue();
    }

    private void sendTranscriptWithDirectLinkButton(TextChannel transcriptChannel, ButtonInteractionEvent event, TextChannel sourceChannel, int messageCount, File transcriptFile, List<Message> messages) {
        EmbedBuilder transcriptEmbed = new EmbedBuilder()
                .setTitle("📄 Ticket Transcript")
                .addField("Ticket Channel", sourceChannel.getName(), true)
                .addField("Generated by", UserDisplayUtil.getFormattedUserInfo(event.getUser()), true)
                .addField("Generated at", TimestampUtil.getCurrentTimestampForEmbeds(), true)
                .addField("Message Count", String.valueOf(messageCount), true)
                .setColor(Color.BLUE)
                .setFooter("Click 'Direct Link' to generate HTML version");

        transcriptChannel.sendMessageEmbeds(transcriptEmbed.build())
                .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(transcriptFile))
                .addActionRow(Button.primary("generate_html_transcript", "🔗 Direct Link"))
                .queue();
    }
}