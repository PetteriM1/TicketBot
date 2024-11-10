package petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static petterim1.ticketbot.Main.log;

public class Instance {

    final JDA JDA;
    final Properties CONFIG = new Properties();

    Instance(String name) throws InterruptedException, LoginException, IOException {
        log("Loading " + name + "...");
        loadConfig("instances/" + name);

        log("Logging in to Discord...");
        JDA = JDABuilder.createDefault(CONFIG.getProperty("bot_token")).build();
        log("Waiting JDA to load...");
        JDA.awaitReady();
        log("Setting bot status to " + CONFIG.getProperty("bot_activity_type") + " " + CONFIG.getProperty("bot_activity_text"));
        JDA.getPresence().setActivity(Activity.of(Activity.ActivityType.valueOf(CONFIG.getProperty("bot_activity_type")), CONFIG.getProperty("bot_activity_text")));
        log("Registering event listener...");
        JDA.addEventListener(new EventListener(this));

        prepareCommands();
        prepareChannel();
    }

    private void loadConfig(String name) throws IOException {
        FileInputStream propsInput = new FileInputStream(name);
        CONFIG.load(propsInput);
        propsInput.close();
    }

    void prepareCommands() {
        if (CONFIG.getProperty("enable_commands", "false").equalsIgnoreCase("true")) {
            log("Registering command listener...");
            JDA.addEventListener(new CommandListener(this));
            log("Registering slash commands...");
            JDA.upsertCommand("close", CONFIG.getProperty("ticket_close_button_text", "ticket_close_button_text")).queue();
            //JDA.upsertCommand("add", "Add more people to this channel").queue();
            //JDA.upsertCommand("remove", "Remove people from this channel").queue();
        }
    }

    void prepareChannel() {
        log("Preparing ticket panel channel...");

        TextChannel channel = JDA.getTextChannelById(CONFIG.getProperty("tickets_panel_channel"));
        if (channel == null) {
            log("tickets_panel_channel is null!");
        } else {
            boolean forceUpdate = CONFIG.getProperty("force_panel_update_on_startup", "true").equalsIgnoreCase("true");
            channel.getHistory().retrievePast(20).queue((messages) -> {
                for (Message message : messages) {
                    if (message.getAuthor().getIdLong() == JDA.getSelfUser().getIdLong() && !message.getEmbeds().isEmpty()) {
                        if (!forceUpdate) {
                            log("No panel update (force_panel_update_on_startup=false)");
                            return;
                        } else {
                            message.delete().queue();
                        }
                    }
                }
                createPanel(channel);
            });
        }
    }

    private void createPanel(TextChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.RED);
        embed.setAuthor(CONFIG.getProperty("tickets_panel_title", "tickets_panel_title"));
        embed.setDescription(CONFIG.getProperty("tickets_panel_text", "tickets_panel_text"));
        channel.sendMessageEmbeds(embed.build())
                .setActionRow(Button.of(ButtonStyle.PRIMARY, ElementID.BTN_OPEN_TICKET, CONFIG.getProperty("tickets_panel_button_text", "tickets_panel_button_text"), Emoji.fromUnicode("\uD83C\uDFAB"))).queue();
    }
}
