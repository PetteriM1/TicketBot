package petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

import javax.annotation.Nonnull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static petterim1.ticketbot.Main.log;

public class EventListener extends ListenerAdapter {

    private static final EnumSet<Permission> PERMISSION_READ_WRITE = EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_ATTACH_FILES);

    private final Instance INSTANCE;

    EventListener(Instance INSTANCE) {
        this.INSTANCE = INSTANCE;
    }

    @Override
    public void onButtonClick(@Nonnull ButtonClickEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild != null && member != null && event.getButton() != null) {
            String buttonId = event.getButton().getId();
            if (ElementID.BTN_OPEN_TICKET.equals(buttonId)) {
                for (TextChannel channel : guild.getTextChannels()) {
                    if (member.getId().equals(channel.getTopic())) {
                        event.getInteraction().reply(INSTANCE.CONFIG.getProperty("tickets_panel_reply_channel_exists", "tickets_panel_reply_channel_exists") + " <#" + channel.getId() + ">").setEphemeral(true).queue();
                        return;
                    }
                }
                Category category = guild.getCategoryById(INSTANCE.CONFIG.getProperty("new_ticket_channels_category"));
                if (category == null) {
                    log("new_ticket_channels_category is null!");
                } else {
                    EnumSet<Permission> pendingTicketUserPermissions;
                    //if (typesCount < 2) {
                        //pendingTicketUserPermissions = PERMISSION_VIEW_AND_WRITE; //TODO: if typesCount < 2, no category selection needed, set support role permission
                    //} else {
                        pendingTicketUserPermissions = EnumSet.of(Permission.VIEW_CHANNEL);
                    //}
                    guild.createTextChannel(INSTANCE.CONFIG.getProperty("ticket_prefix") + member.getUser().getName())
                            .setParent(category)
                            .setTopic(member.getId())
                            .addMemberPermissionOverride(INSTANCE.JDA.getSelfUser().getIdLong(), PERMISSION_READ_WRITE, null)
                            .addMemberPermissionOverride(member.getIdLong(), pendingTicketUserPermissions, /*typesCount < 2 ? null :*/ EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(guild.getPublicRole(), null, PERMISSION_READ_WRITE)
                            .queue((channel) -> {
                                event.getInteraction().reply(INSTANCE.CONFIG.getProperty("tickets_panel_reply_channel_created", "tickets_panel_reply_channel_created") + " <#" + channel.getId() + ">").setEphemeral(true).queue();
                                EmbedBuilder embed = new EmbedBuilder();
                                embed.setColor(Color.ORANGE);
                                embed.setAuthor(INSTANCE.CONFIG.getProperty("category_panel_title", "category_panel_title"));
                                embed.setDescription(INSTANCE.CONFIG.getProperty("category_panel_text", "category_panel_text"));
                                /*if (typesCount < 2) {
                                    return; //TODO: if typesCount < 2, no category selection needed, send ticket info
                                }*/
                                ArrayList<SelectOption> ticketTypes = new ArrayList<>();
                                for (int i = 1; i <= INSTANCE.typesCount; i++) {
                                    ticketTypes.add(SelectOption.of(INSTANCE.CONFIG.getProperty("category_panel_category_name_" + i, "category_panel_category_name_" + i), ElementID.ROW_VALUE + "=" + i)
                                                    .withDescription(INSTANCE.CONFIG.getProperty("category_panel_description_" + i, "category_panel_description_" + i))
                                                    .withEmoji(Emoji.fromUnicode("❓")));
                                }
                                channel.sendMessageEmbeds(embed.build())
                                        .setActionRow(SelectionMenu.create(ElementID.PNL_CATEGORY).addOptions(ticketTypes).build()).queue();
                                long channelId = channel.getIdLong();
                                try {
                                    Main.SCHEDULER.schedule(() -> {
                                        TextChannel checkChannel = INSTANCE.JDA.getTextChannelById(channelId);
                                        if (checkChannel != null) {
                                            checkChannel.getHistory().retrievePast(20).queue((messages) -> {
                                                for (Message message : messages) {
                                                    if (message.getAuthor().getIdLong() == INSTANCE.JDA.getSelfUser().getIdLong() && !message.getActionRows().isEmpty() && !message.getActionRows().get(0).getComponents().isEmpty()) {
                                                        if (ElementID.PNL_CATEGORY.equals(message.getActionRows().get(0).getComponents().get(0).getId())) {
                                                            checkChannel.sendMessage(INSTANCE.CONFIG.getProperty("category_panel_timeout_message", "category_panel_timeout_message")).queue((then) -> checkChannel.delete().queue());
                                                            return;
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }, Integer.parseInt(INSTANCE.CONFIG.getProperty("category_panel_timeout_seconds")), TimeUnit.SECONDS);
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("category_panel_timeout_seconds must be a positive integer!");
                                }
                            });
                }
            } else if (ElementID.BTN_CLOSE_TICKET.equals(buttonId)) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Color.PINK);
                embed.setAuthor(INSTANCE.CONFIG.getProperty("ticket_close_confirmation_title", "ticket_close_confirmation_title"));
                embed.setDescription(INSTANCE.CONFIG.getProperty("ticket_close_confirmation_text", "ticket_close_confirmation_text"));
                event.getInteraction().replyEmbeds(embed.build()).addActionRow(
                        Button.of(ButtonStyle.PRIMARY, ElementID.BTN_CLOSE_TICKET_CONFIRM, INSTANCE.CONFIG.getProperty("ticket_close_confirmation_button_text", "ticket_close_confirmation_button_text"))
                ).setEphemeral(true).queue();
            } else if (ElementID.BTN_CLOSE_TICKET_CONFIRM.equals(buttonId)) {
                TextChannel channel = guild.getTextChannelById(event.getChannel().getId());
                if (channel == null || !INSTANCE.isTicketChannel(channel)) {
                    log("Failed to find ticket TextChannel where close confirmation button was clicked!");
                } else {
                    Category category = channel.getParent();
                    if (category != null) {
                        int inTicketCategory = 0;
                        for (int i = 1; i <= INSTANCE.typesCount; i++) {
                            String ticketCategory = INSTANCE.CONFIG.getProperty("category_id_for_" + i);
                            if (category.getId().equals(ticketCategory)) {
                                inTicketCategory = i;
                                break;
                            }
                        }
                        if (inTicketCategory > 0) {
                            int finalInTicketCategory = inTicketCategory;
                            event.getInteraction().reply(INSTANCE.CONFIG.getProperty("ticket_close_reply", "ticket_close_reply")).setEphemeral(true).queue((x) -> {
                                String logChannelId = INSTANCE.CONFIG.getProperty("tickets_log_channel");
                                if (logChannelId.isEmpty()) {
                                    channel.delete().queue();
                                } else {
                                    TextChannel logChannel = INSTANCE.JDA.getTextChannelById(logChannelId);
                                    if (logChannel == null) {
                                        log("tickets_log_channel is null!");
                                    } else {
                                        HistoryUtils.exportFullHistoryAndDelete(channel, member, logChannel);
                                    }
                                }
                                tryDeleteStaffPanel(finalInTicketCategory, member.getId());
                            });
                            return;
                        }
                    }
                    event.getInteraction().reply(INSTANCE.CONFIG.getProperty("ticket_close_error", "ticket_close_error")).setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
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
                    channel.getHistory().retrievePast(20).queue((messages) -> {
                        for (Message message : messages) {
                            if (message.getAuthor().getIdLong() == INSTANCE.JDA.getSelfUser().getIdLong() && !message.getEmbeds().isEmpty()) {
                                message.delete().queue((then) -> {
                                    Category parent = INSTANCE.JDA.getCategoryById(INSTANCE.CONFIG.getProperty("category_id_for_" + selected));
                                    if (parent == null) {
                                        log("category_id_for_" + selected + " is null!");
                                    } else if (!(channel instanceof GuildChannel)) {
                                        log("MessageChannel is not a GuildChannel!");
                                    } else {
                                        ((GuildChannel) channel).getManager().setParent(parent)
                                                .putMemberPermissionOverride(member.getIdLong(), PERMISSION_READ_WRITE, null)
                                                .putRolePermissionOverride(Long.parseLong(INSTANCE.CONFIG.getProperty("ticket_access_role_id_" + selected, "ticket_access_role_id_" + selected)), PERMISSION_READ_WRITE, null) //TODO: replace with per category roles
                                                .queue();
                                    }
                                    String supportRoleId = INSTANCE.CONFIG.getProperty("ping_support_role_id_" + selected);
                                    if (supportRoleId != null && !supportRoleId.isEmpty()) {
                                        /*Role supportRole = INSTANCE.JDA.getRoleById(supportRoleId);
                                        if (supportRole != null) { //TODO: use ChannelManager on move
                                            ((TextChannel) channel).putPermissionOverride(supportRole).setAllow(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE)).queue();
                                        }*/
                                        channel.sendMessage("<@&" + supportRoleId + ">").queue();
                                    }
                                    EmbedBuilder embed = new EmbedBuilder();
                                    embed.setColor(Color.GREEN);
                                    embed.setAuthor(INSTANCE.CONFIG.getProperty("report_info_title_" + selected, "report_info_title_" + selected));
                                    embed.setDescription(INSTANCE.CONFIG.getProperty("report_info_text_" + selected, "report_info_text_" + selected));
                                    channel.sendMessageEmbeds(embed.build())
                                            .setActionRow(Button.of(ButtonStyle.DANGER, ElementID.BTN_CLOSE_TICKET, INSTANCE.CONFIG.getProperty("ticket_close_button_text", "ticket_close_button_text"), Emoji.fromUnicode("❌"))).queue();

                                    long channelId = channel.getIdLong();
                                    try {
                                        Main.SCHEDULER.schedule(() -> {
                                            TextChannel checkChannel = INSTANCE.JDA.getTextChannelById(channelId);
                                            if (checkChannel != null) {
                                                checkChannel.getHistory().retrievePast(20).queue((messages0) -> {
                                                    for (Message message0 : messages0) {
                                                        if (message0.getAuthor().getIdLong() != INSTANCE.JDA.getSelfUser().getIdLong()) {
                                                            return;
                                                        }
                                                    }

                                                    checkChannel.sendMessage(INSTANCE.CONFIG.getProperty("ticket_close_inactive_timeout_message", "ticket_close_inactive_timeout_message")).queue((then0) -> checkChannel.delete().queue());
                                                });
                                            }
                                        }, Integer.parseInt(INSTANCE.CONFIG.getProperty("ticket_close_inactive_timeout_seconds")), TimeUnit.SECONDS);
                                    } catch (NumberFormatException e) {
                                        throw new RuntimeException("ticket_close_inactive_timeout_seconds must be a positive integer!");
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        TextChannel channel = event.getChannel();
        Category category = channel.getParent();
        String topic = channel.getTopic();

        if (category == null || topic == null) {
            return;
        }

        if (!INSTANCE.isTicketChannel(channel)) {
            return;
        }

        int inTicketCategory = 0;
        for (int i = 1; i <= INSTANCE.typesCount; i++) {
            String ticketCategory = INSTANCE.CONFIG.getProperty("category_id_for_" + i);
            if (category.getId().equals(ticketCategory)) {
                inTicketCategory = i;
                break;
            }
        }

        if (inTicketCategory < 1) {
            tryHandleStaffPanelMessage(category, topic, event.getMessage());
            return;
        }

        String targetCategoryId = INSTANCE.CONFIG.getProperty("staff_panel_id_for_" + inTicketCategory);
        if (targetCategoryId == null || targetCategoryId.isEmpty()) {
            return;
        }

        Category targetCategory = INSTANCE.JDA.getCategoryById(targetCategoryId);
        if (targetCategory == null) {
            log("Didn't find Category for staff_panel_id_for_" + inTicketCategory);
            return;
        }

        TextChannel targetChannel = null;
        for (TextChannel c : targetCategory.getTextChannels()) {
            if (c.getTopic() != null && c.getTopic().equals(event.getAuthor().getId())) {
                targetChannel = c;
                break;
            }
        }

        StringBuilder fullMsg;

        if (targetChannel == null) {
            fullMsg = new StringBuilder("New ticket in '")
                    .append(INSTANCE.CONFIG.getProperty("category_panel_category_name_" + inTicketCategory))
                    .append("'\n\n<@")
                    .append(event.getAuthor().getId())
                    .append("> on <#")
                    .append(channel.getId())
                    .append(">\n")
                    .append(event.getMessage().getContentStripped());

            if (!event.getMessage().getAttachments().isEmpty()) {
                for (Message.Attachment attachment : event.getMessage().getAttachments()) {
                    fullMsg.append("\n").append(attachment.getUrl());
                }
            }

            targetCategory.getGuild().createTextChannel(channel.getName())
                    .setParent(targetCategory)
                    .setTopic(event.getAuthor().getId())
                    //.addMemberPermissionOverride(INSTANCE.JDA.getSelfUser().getIdLong(), PERMISSION_READ_WRITE, null)
                    //.addPermissionOverride(targetCategory.getGuild().getPublicRole(), null, PERMISSION_READ_WRITE)
                    .queue((newChannel) ->
                            newChannel.sendMessage(fullMsg).queue());
            return;
        }

        fullMsg = new StringBuilder("<@")
                .append(event.getAuthor().getId())
                .append("> on <#")
                .append(channel.getId())
                .append(">\n")
                .append(event.getMessage().getContentStripped());

        if (!event.getMessage().getAttachments().isEmpty()) {
            for (Message.Attachment attachment : event.getMessage().getAttachments()) {
                fullMsg.append("\n").append(attachment.getUrl());
            }
        }

        targetChannel.sendMessage(fullMsg).queue();
    }

    private void tryHandleStaffPanelMessage(Category category, String topic, Message originalMessage) {
        int inTicketCategory = 0;
        for (int i = 1; i <= INSTANCE.typesCount; i++) {
            String ticketCategory = INSTANCE.CONFIG.getProperty("staff_panel_id_for_" + i);
            if (category.getId().equals(ticketCategory)) {
                inTicketCategory = i;
                break;
            }
        }

        if (inTicketCategory < 1) {
            return;
        }

        String targetCategoryId = INSTANCE.CONFIG.getProperty("category_id_for_" + inTicketCategory);
        if (targetCategoryId == null || targetCategoryId.isEmpty()) {
            return;
        }

        Category targetCategory = INSTANCE.JDA.getCategoryById(targetCategoryId);
        if (targetCategory == null) {
            log("Didn't find Category for category_id_for_" + inTicketCategory);
            return;
        }

        TextChannel targetChannel = null;
        for (TextChannel c : targetCategory.getTextChannels()) {
            if (c.getTopic() != null && c.getTopic().equals(topic)) {
                targetChannel = c;
                break;
            }
        }

        boolean allowCategoryMerge = true;

        if (targetChannel == null) {
            if (allowCategoryMerge) {
                channelFound:

                for (int i = 1; i <= INSTANCE.typesCount; i++) {
                    String ticketCategory = INSTANCE.CONFIG.getProperty("category_id_for_" + i);
                    if (ticketCategory == null || ticketCategory.isEmpty()) {
                        continue;
                    }

                    Category otherCategory = INSTANCE.JDA.getCategoryById(ticketCategory);
                    if (otherCategory == null) {
                        continue;
                    }

                    for (TextChannel c : otherCategory.getTextChannels()) {
                        if (c.getTopic() != null && c.getTopic().equals(topic)) {
                            targetChannel = c;
                            break channelFound;
                        }
                    }
                }
            }

            if (targetChannel == null) {
                originalMessage.addReaction("❌").queue();
                return;
            }
        }

        StringBuilder fullMsg = new StringBuilder("<@")
                .append(topic)
                .append("> ")
                .append(INSTANCE.CONFIG.getProperty("staff_reply_format"))
                .append(originalMessage.getContentStripped());

        if (!originalMessage.getAttachments().isEmpty()) {
            for (Message.Attachment attachment : originalMessage.getAttachments()) {
                fullMsg.append("\n").append(attachment.getUrl());
            }
        }

        targetChannel.sendMessage(fullMsg).queue((sent) -> {
            if (sent != null) {
                originalMessage.addReaction("✅").queue();
            }
        });
    }

    private void tryDeleteStaffPanel(int inTicketCategory, String closedById) {
        String targetCategoryId = INSTANCE.CONFIG.getProperty("staff_panel_id_for_" + inTicketCategory);
        if (targetCategoryId == null || targetCategoryId.isEmpty()) {
            return;
        }

        Category targetCategory = INSTANCE.JDA.getCategoryById(targetCategoryId);
        if (targetCategory == null) {
            log("Didn't find Category for staff_panel_id_for_" + inTicketCategory);
            return;
        }

        for (TextChannel c : targetCategory.getTextChannels()) {
            if (c.getTopic() != null && c.getTopic().equals(closedById)) {
                c.delete().queue();
                return;
            }
        }
    }
}
