package petterim1.ticketbot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ListIterator;

import static petterim1.ticketbot.Main.log;

public class HistoryUtils {

    static void exportFullHistoryAndDelete(TextChannel channel, Member closedBy, TextChannel logChannel) {
        StringBuilder chatLog = new StringBuilder();

        chatLog.append(channel.getName());
        if (channel.getParent() != null) {
            chatLog.append(" in ").append(channel.getParent().getName());
        }
        chatLog.append(" in ").append(channel.getGuild().getName());
        chatLog.append(" was created ").append(channel.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC)).append(" (UTC)\n\n");

        fetchAll(channel, chatLog, null, () -> {
            chatLog.append(channel.getName())
                    .append(" was closed ")
                    .append(OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC))
                    .append(" (UTC) by ")
                    .append(closedBy.getUser().getName())
                    .append(" ")
                    .append(closedBy.getId());

            try {
                byte[] data = chatLog.toString().getBytes(StandardCharsets.UTF_8);
                logChannel.sendFile(new ByteArrayInputStream(data), channel.getName() + ".txt")
                        .queue(done -> channel.delete().queue());
            } catch (Exception e) {
                log("Failed to write log");
                e.printStackTrace();
            }
        });
    }

    static void fetchAll(TextChannel channel, StringBuilder chatLog, Long beforeId, Runnable onComplete) {
        if (beforeId == null) {
            channel.getHistory().retrievePast(100).queue(messages ->
                    handleMessages(channel, chatLog, messages, onComplete));
        } else {
            channel.getHistoryBefore(beforeId, 100).queue(messages ->
                    handleMessages(channel, chatLog, messages.getRetrievedHistory(), onComplete));
        }
    }

    static void handleMessages(TextChannel channel, StringBuilder chatLog, List<Message> messages, Runnable onComplete) {
        if (messages.isEmpty()) {
            onComplete.run();
            return;
        }

        ListIterator<Message> it = messages.listIterator(messages.size());
        while (it.hasPrevious()) {
            Message msg = it.previous();

            chatLog.append(msg.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC))
                    .append(" (UTC) ")
                    .append(msg.getAuthor().getName())
                    .append(" ")
                    .append(msg.getAuthor().getId())
                    .append("\n")
                    .append(msg.getContentStripped())
                    .append("\n\n");
        }

        long nextBefore = messages.get(messages.size() - 1).getIdLong();
        fetchAll(channel, chatLog, nextBefore, onComplete);
    }
}
