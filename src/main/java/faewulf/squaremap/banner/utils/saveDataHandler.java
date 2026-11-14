package faewulf.squaremap.banner.utils;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import faewulf.squaremap.banner.Squaremapbanner;
import faewulf.squaremap.banner.dataType.Data;
import faewulf.squaremap.banner.dataType.DataTypeAdapter;
import faewulf.squaremap.banner.dataType.Position;
import faewulf.squaremap.banner.dataType.PositionTypeAdapter;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class saveDataHandler {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Data.class, new DataTypeAdapter())
            .registerTypeAdapter(Position.class, new PositionTypeAdapter())
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create();

    private static final Path DATA_FILE = Path.of("squaremap", "banner.json");

    /**
     * Never returns null. If the file cannot be parsed it is backed up first, because the server
     * saves right after startup and would otherwise overwrite (and lose) the damaged file.
     */
    public static Map<String, world> loadData() {
        Type type = new TypeToken<Map<String, world>>() {
        }.getType();

        if (!Files.exists(DATA_FILE))
            return Maps.newHashMap();

        try (Reader reader = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) {
            Map<String, world> data = GSON.fromJson(reader, type);
            return data != null ? data : Maps.newHashMap();
        } catch (Exception e) {
            Squaremapbanner.LOGGER.error("Error while reading data: ", e);
            FileUtil.backupBroken(DATA_FILE);
            return Maps.newHashMap();
        }
    }

    /** null when the last save worked, otherwise the reason it failed (shown by /squaremapbanner status). */
    private static volatile String lastError = null;

    public static Path dataFile() {
        return DATA_FILE;
    }

    public static String lastError() {
        return lastError;
    }

    /**
     * @return true if the file was written
     */
    public static boolean saveConfig() {
        try {
            String json = GSON.toJson(bannerManager.bannerManager);
            FileUtil.writeAtomic(DATA_FILE, json);
            lastError = null;
            DebugLog.log("saved " + DATA_FILE.toAbsolutePath() + " (" + json.length() + " chars)");
            return true;
        } catch (Exception e) {
            //IOException, but also runtime errors (e.g. from Gson) must never escape into the game event
            lastError = e.toString();
            Squaremapbanner.LOGGER.error("Error while saving data: ", e);
            DebugLog.error("save FAILED", e);
            return false;
        }
    }
}