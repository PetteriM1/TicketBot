package petterim1.ticketbot;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {

    static final ArrayList<Instance> INSTANCES = new ArrayList<>();
    static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    static void log(String text) {
        System.out.println(text);
    }

    public static void main(String[] args) throws InterruptedException, LoginException, IOException {
        log("Discord ticket bot made by PetteriM1");
        log("---------------------");
        log("Looking for instance configs...");

        File dir = checkConfigDir();
        if (dir == null) {
            return;
        }

        //noinspection DataFlowIssue
        for (File file : dir.listFiles()) {
            String name = file.getName();

            if (name.endsWith(".txt")) {
                INSTANCES.add(new Instance(name));
            }
        }

        log("The bot is online!");
    }

    private static File checkConfigDir() throws IOException {
        File dir = new File("instances");

        if (!dir.exists()) {
            log("No instances found, creating empty config at instances/default.txt");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdir();
            exportDefaultConfig();
            return null;
        }

        return dir;
    }

    private static void exportDefaultConfig() throws IOException {
        InputStream stream = null;
        OutputStream resStreamOut = null;

        try {
            stream = Main.class.getClassLoader().getResourceAsStream("config.txt.empty");
            if (stream == null) {
                throw new RuntimeException("Cannot get 'config.txt.empty' from the jar file!");
            }
            resStreamOut = Files.newOutputStream(Paths.get(new File("instances/default.txt").toURI()));
            byte[] buffer = new byte[4096];
            int readBytes;
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (resStreamOut != null) {
                resStreamOut.close();
            }
        }
    }
}
