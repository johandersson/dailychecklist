import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {
    private DebugLog() {}

    private static final Path LOG_DIR = Paths.get("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("dnd.log");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static BufferedWriter writer;

    private static synchronized void initWriter() {
        if (writer != null) return;
        try {
            if (!Files.exists(LOG_DIR)) Files.createDirectories(LOG_DIR);
            writer = Files.newBufferedWriter(LOG_FILE, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            writer = null;
            System.err.println("[DebugLog] Failed to open log file: " + e.getMessage());
        }
    }

    public static void d(String fmt, Object... args) {
        String msg = args == null || args.length == 0 ? fmt : String.format(fmt, args);
        String ts = LocalDateTime.now().format(TS_FMT);
        String out = String.format("[DEBUG] %s %s - %s", ts, Thread.currentThread().getName(), msg);

        // Console for immediate visibility
        System.out.println(out);

        // File (best-effort)
        try {
            initWriter();
            if (writer != null) {
                writer.write(out);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("[DebugLog] Failed to write to log file: " + e.getMessage());
        }
    }
}
