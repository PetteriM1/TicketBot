package petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import javax.annotation.Nonnull;
import java.awt.*;

import static petterim1.ticketbot.Main.log;

public class CommandListener extends ListenerAdapter {

    private final Instance INSTANCE;

    CommandListener(Instance INSTANCE) {
        this.INSTANCE = INSTANCE;
    }

    @Override
    public void onSlashCommand(@Nonnull SlashCommandEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        if (member != null && guild != null && event.getName().equalsIgnoreCase("close")) {
            TextChannel channel = guild.getTextChannelById(event.getChannel().getId());
            if (channel == null || !INSTANCE.isTicketChannel(channel)) {
                log("Failed to find ticket TextChannel where slash command was executed!");
            } else {
                Category category = channel.getParent();
                if (category != null) {
                    boolean inTicketCategory = false;
                    for (int i = 1; i <= INSTANCE.typesCount; i++) {
                        String ticketCategory = INSTANCE.CONFIG.getProperty("category_id_for_" + i);
                        if (category.getId().equals(ticketCategory)) {
                            inTicketCategory = true;
                            break;
                        }
                    }
                    if (inTicketCategory) {
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setColor(Color.PINK);
                        embed.setAuthor(INSTANCE.CONFIG.getProperty("ticket_close_confirmation_title", "ticket_close_confirmation_title"));
                        embed.setDescription(INSTANCE.CONFIG.getProperty("ticket_close_confirmation_text", "ticket_close_confirmation_text"));
                        event.replyEmbeds(embed.build()).addActionRow(
                                Button.of(ButtonStyle.PRIMARY, ElementID.BTN_CLOSE_TICKET_CONFIRM, INSTANCE.CONFIG.getProperty("ticket_close_confirmation_button_text", "ticket_close_confirmation_button_text"))
                        ).setEphemeral(true).queue();
                    }
                }
            }
        }
    }
}
