package me.petterim1.ticketbot;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

import static me.petterim1.ticketbot.Main.CONFIG;
import static me.petterim1.ticketbot.Main.log;

public class EventListener extends ListenerAdapter {

    public void onButtonClick(@Nonnull ButtonClickEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild != null && member != null && event.getButton() != null) {
            String buttonId = event.getButton().getId();
            if (ButtonID.OPEN_TICKET.equals(buttonId)) {
                for (TextChannel channel : guild.getTextChannels()) {
                    if (member.getId().equals(channel.getTopic())) {
                        event.getInteraction().reply(CONFIG.getProperty("tickets_panel_reply_channel_exists") + " <#" + channel.getId() + ">").setEphemeral(true).queue();
                        return;
                    }
                }
                Category category = guild.getCategoryById(CONFIG.getProperty("new_ticket_channels_category"));
                if (category == null) {
                    log("new_ticket_channels_category is null!");
                } else {
                    guild.createTextChannel(CONFIG.getProperty("ticket_prefix") + member.getEffectiveName(), category)
                            .setTopic(member.getId())
                            .queue((channel) ->
                            event.getInteraction().reply(CONFIG.getProperty("tickets_panel_reply_channel_created") + " <#" + channel.getId() + ">").setEphemeral(true).queue());
                }
                return;
            }
            if (ButtonID.OPEN_TICKET.equals(buttonId)) {
                //TODO
            }
        } else {
            log("Malformed ButtonClickEvent data!");
        }
    }
}
