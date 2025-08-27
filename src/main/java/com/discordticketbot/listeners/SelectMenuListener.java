package com.discordticketbot.listeners;

import com.discordticketbot.utils.HelpSections;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SelectMenuListener extends ListenerAdapter {
	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		if (!"help_menu".equals(event.getComponentId())) {
			return;
		}

		String value = event.getValues().isEmpty() ? "overview" : event.getValues().get(0);
		var embed = HelpSections.build(value, event.getJDA().getSelfUser().getAvatarUrl());
		event.editMessageEmbeds(embed.build()).queue();
	}
}


