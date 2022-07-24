package me.petterim1.ticketbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main extends ListenerAdapter {

    static JDA JDA;
    static Properties CONFIG = new Properties();

    public static void main(String[] args) throws InterruptedException, LoginException, IOException {
        log("Discord ticket bot made by PetteriM1");
        log("---------------------");
        log("Loading config...");
        FileInputStream propsInput = new FileInputStream("config.txt");
        CONFIG.load(propsInput);
        propsInput.close();
        log("Logging in to Discord...");
        JDA = JDABuilder.createDefault(CONFIG.getProperty("bot_token")).build();
        log("Waiting JDA to load...");
        JDA.awaitReady();
        log("Setting bot status to " + CONFIG.getProperty("bot_activity_type") + " " + CONFIG.getProperty("bot_activity_text"));
        JDA.getPresence().setActivity(Activity.of(Activity.ActivityType.valueOf(CONFIG.getProperty("bot_activity_type")), CONFIG.getProperty("bot_activity_text")));
        log("Registering event listener...");
        JDA.addEventListener(new EventListener());
        log("Preparing ticket channel...");
        prepareChannel();
        log("The bot is online!");
    }

    private static void prepareChannel() {
        TextChannel channel = JDA.getTextChannelById(CONFIG.getProperty("tickets_panel_channel"));
        if (channel == null) {
            log("tickets_panel_channel is null!");
        } else {
            channel.getHistory().retrievePast(1).queue((messages) -> {
                for (Message message : messages) {
                    if (message.getAuthor().getIdLong() == JDA.getSelfUser().getIdLong() && !message.getEmbeds().isEmpty()) {
                        message.delete().queue();
                    }
                }
                createPanel(channel);
            });
        }
    }

    private static void createPanel(TextChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.RED);
        embed.setAuthor(CONFIG.getProperty("tickets_panel_title"));
        embed.setDescription(CONFIG.getProperty("tickets_panel_text"));
        channel.sendMessage(embed.build()).queue();
    }

    static void log(String text) {
        System.out.println(text);
    }
}
