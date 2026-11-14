package faewulf.squaremap.banner.utils;

import faewulf.squaremap.banner.Squaremapbanner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Step-by-step trace for finding problems on servers where the normal log is not available.
 * Off by default ("debug": false in the config). When on, every line goes to
 * squaremap/banner-debug.log (and to the normal log). It can never throw into the game.
 */
public final class DebugLog {
    private static final Path FILE = Path.of("squaremap", "banner-debug.log");
    private static final long MAX_BYTES = 1_000_000L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private DebugLog() {
    }

    public static boolean enabled() {
        return Squaremapbanner.modConfigs != null && Squaremapbanner.modConfigs.debug;
    }

    public static Path file() {
        return FILE;
    }

    public static void log(String message) {
        if (enabled())
            write(message);
    }

    /**
     * The message is only built when debug is on, so call sites cost nothing otherwise.
     */
    public static void log(Supplier<String> message) {
        if (enabled())
            write(message.get());
    }

    public static void error(String message, Throwable error) {
        if (!enabled())
            return;

        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        write(message + "\n" + trace);
    }

    private static void write(String message) {
        try {
            Squaremapbanner.LOGGER.info("[debug] {}", message);

            Path parent = FILE.toAbsolutePath().getParent();
            if (parent != null)
                Files.createDirectories(parent);

            //keep the file small: start over when it gets big
            if (Files.exists(FILE) && Files.size(FILE) > MAX_BYTES)
                Files.delete(FILE);

            String line = LocalDateTime.now().format(TIME) + " [" + Thread.currentThread().getName() + "] "
                    + message + System.lineSeparator();

            Files.writeString(FILE, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            //debugging must never break the game
        }
    }
}
