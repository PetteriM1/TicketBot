package me.petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import javax.annotation.Nonnull;
import java.awt.*;

import static me.petterim1.ticketbot.Main.CONFIG;
import static me.petterim1.ticketbot.Main.log;

public class CommandListener extends ListenerAdapter {

    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        Member member = event.getMember();
        if (member != null) {
            if (event.getChannel().getTopic() != null && event.getChannel().getName().startsWith(CONFIG.getProperty("ticket_prefix"))) {
                try {
                    Long.parseLong(event.getChannel().getTopic());
                } catch (NumberFormatException notATicket) {
                    return;
                }
                if (event.getMessage().getContentStripped().equalsIgnoreCase(CONFIG.getProperty("command_prefix", "") + "close")) {
                    Category category = event.getChannel().getParent();
                    if (category != null) {
                        int typesCount;
                        try {
                            typesCount = Integer.parseInt(CONFIG.getProperty("category_panel_categories"));
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("category_panel_categories must be a positive integer!");
                        }
                        boolean inTicketCategory = false;
                        for (int i = 1; i <= typesCount; i++) {
                            String ticketCategory = CONFIG.getProperty("category_id_for_" + i);
                            if (category.getId().equals(ticketCategory)) {
                                inTicketCategory = true;
                                break;
                            }
                        }
                        if (inTicketCategory) {
                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setColor(Color.PINK);
                            embed.setAuthor(CONFIG.getProperty("ticket_close_confirmation_title", "ticket_close_confirmation_title"));
                            embed.setDescription(CONFIG.getProperty("ticket_close_confirmation_text", "ticket_close_confirmation_text"));
                            event.getMessage().replyEmbeds(embed.build()).setActionRow(
                                    Button.of(ButtonStyle.PRIMARY, ElementID.BTN_CLOSE_TICKET_CONFIRM, CONFIG.getProperty("ticket_close_confirmation_button_text", "ticket_close_confirmation_button_text"))
                            ).queue();
                        }
                    }
                }
            }
        } else {
            log("Malformed GuildMessageReceivedEvent data!");
        }
    }
}
