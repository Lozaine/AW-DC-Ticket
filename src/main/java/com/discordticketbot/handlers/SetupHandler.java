package com.discordticketbot.handlers;

import com.discordticketbot.bot.TicketBot;
import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.PermissionUtil;
import com.discordticketbot.utils.RoleParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupHandler {
    private final Map<String, GuildConfig> guildConfigs;

    public SetupHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasAdminPermission(event.getMember())) {
            event.reply("❌ You need Administrator permissions to use this command.")
                    .setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.")
                    .setEphemeral(true).queue();
            return;
        }

        // Check if bot has Administrator permission
        Member botMember = guild.getSelfMember();
        if (!botMember.hasPermission(Permission.ADMINISTRATOR)) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Missing Administrator Permission")
                    .setDescription("This bot requires **Administrator** permission to function properly.\n\n" +
                            "**Why Administrator?**\n" +
                            "• Creates and manages ticket channels dynamically\n" +
                            "• Sets up complex permission overrides\n" +
                            "• Manages channel deletion and transcripts\n\n" +
                            "**Please:**\n" +
                            "1. Go to Server Settings → Roles\n" +
                            "2. Find the bot's role\n" +
                            "3. Enable 'Administrator' permission\n" +
                            "4. Try the setup command again")
                    .setColor(Color.RED)
                    .setFooter("Administrator permission ensures reliable ticket system operation");

            event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
            return;
        }

        String categoryId = event.getOption("category").getAsChannel().getId();
        String panelChannelId = event.getOption("panel_channel").getAsChannel().getId();
        TextChannel transcriptChannel = (TextChannel) event.getOption("transcript_channel").getAsChannel();

        // Required error log channel
        if (event.getOption("error_log_channel") == null) {
            event.reply("❌ Error log channel is required. Please specify a channel where bot errors will be logged.").setEphemeral(true).queue();
            return;
        }

        var errorChannel = event.getOption("error_log_channel").getAsChannel();
        if (!(errorChannel instanceof TextChannel)) {
            event.reply("❌ Error log channel must be a text channel.").setEphemeral(true).queue();
            return;
        }
        TextChannel errorLogChannel = (TextChannel) errorChannel;

        // Parse multiple roles from the STRING option
        String supportRaw = event.getOption("support_roles").getAsString();
        List<Role> supportRoles = RoleParser.parseSupportRoles(supportRaw, guild);
        if (supportRoles.isEmpty()) {
            event.reply("❌ I couldn't find any valid roles. Paste role mentions like `@Mods @Helpers` or role IDs separated by spaces/commas.")
                    .setEphemeral(true).queue();
            return;
        }

        // Validate channel types
        if (!(event.getOption("category").getAsChannel() instanceof Category)) {
            event.reply("❌ Please select a **Category** for `category`.").setEphemeral(true).queue();
            return;
        }
        if (!(event.getOption("panel_channel").getAsChannel() instanceof TextChannel)) {
            event.reply("❌ Please select a **Text Channel** for `panel_channel`.").setEphemeral(true).queue();
            return;
        }
        if (!(transcriptChannel instanceof TextChannel)) {
            event.reply("❌ Transcript channel must be a text channel.").setEphemeral(true).queue();
            return;
        }

        Category category = (Category) event.getOption("category").getAsChannel();
        TextChannel panelChannel = (TextChannel) event.getOption("panel_channel").getAsChannel();


        // Safer parent check
        Category parent = panelChannel.getParentCategory();
        if (parent == null || !parent.getId().equals(categoryId)) {
            event.reply("❌ The **panel channel** must be **under the selected category**.")
                    .setEphemeral(true).queue();
            return;
        }

        // Create or get existing config
        GuildConfig config = guildConfigs.getOrDefault(guild.getId(), new GuildConfig());
        config.categoryId = categoryId;
        config.panelChannelId = panelChannelId;
        config.transcriptChannelId = transcriptChannel.getId();
        config.errorLogChannelId = errorLogChannel.getId();

        // Clear existing support roles
        config.supportRoleIds.clear();
        for (Role role : supportRoles) config.supportRoleIds.add(role.getId());

        // Initialize ticket counter based on existing tickets
        initializeTicketCounter(guild, config);

        // Save to both memory and database
        config.setGuildId(guild.getId());
        guildConfigs.put(guild.getId(), config);
        config.save(); // This saves to database

        // Confirmation embed
        StringBuilder supportRolesMention = new StringBuilder();
        for (Role role : supportRoles) supportRolesMention.append(role.getAsMention()).append(" ");
        String supportRolesList = supportRolesMention.toString().trim();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Ticket System Configured!")
                .setDescription("Your ticket system has been successfully configured with Administrator permissions!\n\n**Configuration saved to database** ✅")
                .addField("📁 Ticket Category", category.getAsMention(), true)
                .addField("📋 Panel Channel", panelChannel.getAsMention(), true)
                .addField("📜 Transcript Channel", transcriptChannel.getAsMention(), true)
                .addField("🚨 Error Log Channel", errorLogChannel.getAsMention(), true)
                .addField("👥 Support Roles", supportRolesList, false)
                .addField("🔢 Ticket Counter", "Initialized at: " + config.ticketCounter + " (next: " + (config.ticketCounter + 1) + ")", true)
                .addField("🎯 Next Step", "Use `/panel` to send the ticket panel to the configured channel!", false)
                .setColor(Color.GREEN)
                .setFooter("Configuration saved for " + guild.getName());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    /**
     * Initialize the ticket counter based on existing ticket channels
     */
    private void initializeTicketCounter(Guild guild, GuildConfig config) {
        Pattern pattern = Pattern.compile("^ticket-[a-z0-9]+-(?<number>\\d{3})$");
        int highestNumber = 0;

        // Find the highest ticket number from existing channels
        for (TextChannel channel : guild.getTextChannels()) {
            Matcher matcher = pattern.matcher(channel.getName());
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group("number"));
                highestNumber = Math.max(highestNumber, number);
            }
        }

        // Initialize counter to continue from the highest existing number
        config.initializeCounterFromExistingTickets(highestNumber);
        System.out.println("Initialized ticket counter for guild " + guild.getName() + " to: " + config.ticketCounter);
    }
}