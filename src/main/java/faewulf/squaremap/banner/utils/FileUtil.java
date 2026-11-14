package faewulf.squaremap.banner.utils;

import faewulf.squaremap.banner.Squaremapbanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileUtil {
    private FileUtil() {
    }

    /**
     * Writes the file through a temporary file and moves it into place, so a crash or a full disk
     * in the middle of a save can never leave a half-written (and therefore unreadable) file behind.
     * Missing parent directories are created.
     */
    public static void writeAtomic(Path target, String content) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null)
            Files.createDirectories(parent);

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");

        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);

            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            //some file systems / mounts / hosts refuse the temp file or the rename:
            //fall back to a plain overwrite, exactly what the mod did before
            Squaremapbanner.LOGGER.warn("Safe write of {} failed ({}), writing the file directly", target, e.toString());

            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // leftover .tmp is harmless
            }

            Files.writeString(target, content, StandardCharsets.UTF_8);
        }
    }

    /**
     * Keeps a copy of an unreadable file (name.broken-timestamp) so the next save does not destroy it.
     */
    public static void backupBroken(Path file) {
        try {
            if (!Files.exists(file))
                return;

            Path backup = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            Squaremapbanner.LOGGER.warn("Backed up the unreadable file {} as {}", file, backup);
        } catch (IOException e) {
            Squaremapbanner.LOGGER.error("Could not back up the unreadable file " + file, e);
        }
    }
}