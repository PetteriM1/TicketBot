package me.petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

import javax.annotation.Nonnull;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
                    int typesCount;
                    try {
                        typesCount = Integer.parseInt(CONFIG.getProperty("category_panel_categories"));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("category_panel_categories must be a positive integer!");
                    }
                    EnumSet<Permission> userPermissions;
                    //if (typesCount < 2) {
                        //userPermissions = EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE); //TODO: if typesCount < 2, no category selection needed, set support role permission
                    //} else {
                        userPermissions = EnumSet.of(Permission.VIEW_CHANNEL);
                    //}
                    EnumSet<Permission> viewAndWrite = EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE);
                    guild.createTextChannel(CONFIG.getProperty("ticket_prefix") + member.getEffectiveName())
                            .setParent(category)
                            .setTopic(member.getId())
                            .addMemberPermissionOverride(Main.JDA.getSelfUser().getIdLong(), viewAndWrite, null)
                            .addMemberPermissionOverride(member.getIdLong(), userPermissions, /*typesCount < 2 ? null :*/ EnumSet.of(Permission.MESSAGE_WRITE))
                            .addRolePermissionOverride(Long.parseLong(CONFIG.getProperty("ticket_access_role_id", "ticket_access_role_id")), viewAndWrite, null)
                            .addPermissionOverride(guild.getPublicRole(), null, viewAndWrite)
                            .queue((channel) -> {
                                event.getInteraction().reply(CONFIG.getProperty("tickets_panel_reply_channel_created", "tickets_panel_reply_channel_created") + " <#" + channel.getId() + ">").setEphemeral(true).queue();
                                EmbedBuilder embed = new EmbedBuilder();
                                embed.setColor(Color.ORANGE);
                                embed.setAuthor(CONFIG.getProperty("category_panel_title", "category_panel_title"));
                                embed.setDescription(CONFIG.getProperty("category_panel_text", "category_panel_text"));
                                /*if (typesCount < 2) {
                                    return; //TODO: if typesCount < 2, no category selection needed, send ticket info
                                }*/
                                ArrayList<SelectOption> ticketTypes = new ArrayList<>();
                                for (int i = 1; i <= typesCount; i++) {
                                    ticketTypes.add(SelectOption.of(CONFIG.getProperty("category_panel_category_name_" + i, "category_panel_category_name_" + i), ElementID.ROW_VALUE + "=" + i)
                                                    .withDescription(CONFIG.getProperty("category_panel_description_" + i, "category_panel_description_" + i))
                                                    .withEmoji(Emoji.fromUnicode("❓")));
                                }
                                channel.sendMessageEmbeds(embed.build())
                                        .setActionRow(SelectionMenu.create(ElementID.PNL_CATEGORY).addOptions(ticketTypes).build()).queue(); //TODO: close ticket if no category selected
                            });
                }
            } else if (ElementID.BTN_CLOSE_TICKET.equals(buttonId)) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Color.PINK);
                embed.setAuthor(CONFIG.getProperty("ticket_close_confirmation_title", "ticket_close_confirmation_title"));
                embed.setDescription(CONFIG.getProperty("ticket_close_confirmation_text", "ticket_close_confirmation_text"));
                event.getInteraction().replyEmbeds(embed.build()).addActionRow(
                        Button.of(ButtonStyle.PRIMARY, ElementID.BTN_CLOSE_TICKET_CONFIRM, CONFIG.getProperty("ticket_close_confirmation_button_text", "ticket_close_confirmation_button_text"))
                ).setEphemeral(true).queue();
            } else if (ElementID.BTN_CLOSE_TICKET_CONFIRM.equals(buttonId)) {
                TextChannel channel = guild.getTextChannelById(event.getChannel().getId());
                if (channel == null) {
                    log("Failed to find TextChannel where close confirmation button was clicked!");
                } else {
                    //TODO: save the ticket
                    channel.delete().queue();
                }
            }
        } else {
            log("Malformed ButtonClickEvent data!");
        }
    }

    public void onSelectionMenu(@Nonnull SelectionMenuEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        List<SelectOption> options = event.getInteraction().getSelectedOptions();
        if (guild != null && member != null && options != null) {
            if (!options.isEmpty()) {
                String[] split = options.get(0).getValue().split("=");
                if (split.length == 2 && ElementID.ROW_VALUE.equals(split[0])) {
                    int selected = Integer.parseInt(split[1]);
                    MessageChannel channel = event.getChannel();
                    channel.getHistory().retrievePast(1).queue((messages) -> {
                        for (Message message : messages) {
                            if (message.getAuthor().getIdLong() == Main.JDA.getSelfUser().getIdLong() && !message.getEmbeds().isEmpty()) {
                                message.delete().queue((then) -> {
                                    String supportRoleId = CONFIG.getProperty("ping_support_role_id_" + selected);
                                    if (supportRoleId != null && !supportRoleId.isEmpty()) {
                                        /*Role supportRole = Main.JDA.getRoleById(supportRoleId);
                                        if (supportRole != null) { //TODO: fix permission issue
                                            ((TextChannel) channel).putPermissionOverride(supportRole).setAllow(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE)).queue();
                                        }*/
                                        channel.sendMessage("<@&" + supportRoleId + ">").queue();
                                    }
                                    EmbedBuilder embed = new EmbedBuilder();
                                    embed.setColor(Color.GREEN);
                                    embed.setAuthor(CONFIG.getProperty("report_info_title_" + selected, "report_info_title_" + selected));
                                    embed.setDescription(CONFIG.getProperty("report_info_text_" + selected, "report_info_text_" + selected));
                                    channel.sendMessageEmbeds(embed.build())
                                            .setActionRow(Button.of(ButtonStyle.DANGER, ElementID.BTN_CLOSE_TICKET, CONFIG.getProperty("ticket_close_button_text", "ticket_close_button_text"), Emoji.fromUnicode("❌"))).queue();
                                    if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                                        ((TextChannel) channel).putPermissionOverride(member).setAllow(Permission.MESSAGE_WRITE).queue();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        } else {
            log("Malformed SelectionMenuEvent data!");
        }
    }
}
