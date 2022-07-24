package me.petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

import javax.annotation.Nonnull;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;

import static me.petterim1.ticketbot.Main.CONFIG;
import static me.petterim1.ticketbot.Main.log;

public class EventListener extends ListenerAdapter {

    public void onButtonClick(@Nonnull ButtonClickEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild != null && member != null && event.getButton() != null) {
            String buttonId = event.getButton().getId();
            if (ElementID.BTN_OPEN_TICKET.equals(buttonId)) {
                for (TextChannel channel : guild.getTextChannels()) {
                    if (member.getId().equals(channel.getTopic())) {
                        event.getInteraction().reply(CONFIG.getProperty("tickets_panel_reply_channel_exists", "tickets_panel_reply_channel_exists") + " <#" + channel.getId() + ">").setEphemeral(true).queue();
                        return;
                    }
                }
                Category category = guild.getCategoryById(CONFIG.getProperty("new_ticket_channels_category"));
                if (category == null) {
                    log("new_ticket_channels_category is null!");
                } else {
                    EnumSet<Permission> permissions = EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE);
                    guild.createTextChannel(CONFIG.getProperty("ticket_prefix") + member.getEffectiveName())
                            .setParent(category)
                            .setTopic(member.getId())
                            .addMemberPermissionOverride(Main.JDA.getSelfUser().getIdLong(), permissions, null)
                            .addMemberPermissionOverride(member.getIdLong(), permissions, null)
                            //.addRolePermissionOverride(Long.parseLong(CONFIG.getProperty("support_role_id_" + categoryId)), permissions, null) //TODO: support role when moving to correct category
                            .addPermissionOverride(guild.getPublicRole(), null, permissions)
                            .queue((channel) -> {
                                event.getInteraction().reply(CONFIG.getProperty("tickets_panel_reply_channel_created", "tickets_panel_reply_channel_created") + " <#" + channel.getId() + ">").setEphemeral(true).queue();
                                EmbedBuilder embed = new EmbedBuilder();
                                embed.setColor(Color.ORANGE);
                                embed.setAuthor(CONFIG.getProperty("category_panel_title", "category_panel_title"));
                                embed.setDescription(CONFIG.getProperty("category_panel_text", "category_panel_text"));
                                try {
                                    int typesCount = Integer.parseInt(CONFIG.getProperty("category_panel_categories"));
                                    //if (typesCount < 2) {
                                        //TODO: if typesCount < 2, no category selection needed
                                        //return;
                                    //}
                                    ArrayList<SelectOption> ticketTypes = new ArrayList<>();
                                    for (int i = 1; i <= typesCount; i++) {
                                        String ticketType = CONFIG.getProperty("category_panel_category_name_" + i, "category_panel_category_name_" + i);
                                        ticketTypes.add(SelectOption.of(ticketType, ticketType)
                                                        .withDescription(CONFIG.getProperty("category_panel_description_" + i, "category_panel_description_" + i))
                                                        .withEmoji(Emoji.fromUnicode("❓")));
                                    }
                                    channel.sendMessageEmbeds(embed.build())
                                            .setActionRow(SelectionMenu.create(ElementID.PNL_CATEGORY).addOptions(ticketTypes).build()).queue();
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("category_panel_categories must be a positive integer!");
                                }
                            });
                }
            } else if (ElementID.BTN_CLOSE_TICKET.equals(buttonId)) {
                //TODO
            }
        } else {
            log("Malformed ButtonClickEvent data!");
        }
    }
}
