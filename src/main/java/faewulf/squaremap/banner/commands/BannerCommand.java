package faewulf.squaremap.banner.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import faewulf.squaremap.banner.Squaremapbanner;
import faewulf.squaremap.banner.dataType.Position;
import faewulf.squaremap.banner.utils.ConfigHandler;
import faewulf.squaremap.banner.utils.DebugLog;
import faewulf.squaremap.banner.utils.bannerManager;
import faewulf.squaremap.banner.utils.bannerRegister;
import faewulf.squaremap.banner.utils.saveDataHandler;
import faewulf.squaremap.banner.utils.wordCheck;
import faewulf.squaremap.banner.utils.world;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static faewulf.squaremap.banner.Squaremapbanner.modConfigs;

/**
 * /squaremapbanner ... (permission: squaremap.banner.admin, vanilla level 2 without a permission mod)
 */
public class BannerCommand {

    private static final int MAX_LIST_LINES = 30;

    private static final SuggestionProvider<CommandSourceStack> BANNER_NAMES = (ctx, builder) -> {
        Set<String> names = new TreeSet<>();
        bannerManager.bannerManager.values().forEach(w -> w.getData().values().forEach(d -> names.add(d.name())));
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OWNER_NAMES = (ctx, builder) -> {
        Set<String> names = new TreeSet<>();
        bannerManager.bannerManager.values().forEach(w -> w.getData().values().forEach(d -> {
            if (d.ownerName() != null)
                names.add(d.ownerName());
        }));
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> BLACKLIST_WORDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new TreeSet<>(modConfigs.blacklist), builder);

    public static void load() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("squaremapbanner")
                .requires(Permissions.require(faewulf.squaremap.banner.dataType.Permissions.ADMIN, 2))

                // /squaremapbanner list [player]
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, null))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(OWNER_NAMES)
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "player")))))

                // /squaremapbanner tp <banner name>
                .then(Commands.literal("tp")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(BANNER_NAMES)
                                .executes(BannerCommand::teleport)))

                // /squaremapbanner remove <x y z>
                .then(Commands.literal("remove")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(BannerCommand::remove)))

                // /squaremapbanner removeall confirm   (current dimension)
                .then(Commands.literal("removeall")
                        .then(Commands.literal("confirm")
                                .executes(BannerCommand::removeAll)))

                // /squaremapbanner status
                .then(Commands.literal("status")
                        .executes(BannerCommand::status))

                // /squaremapbanner debug on|off|tail
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(ctx -> setDebug(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setDebug(ctx, false)))
                        .then(Commands.literal("tail").executes(BannerCommand::debugTail)))

                // /squaremapbanner reload
                .then(Commands.literal("reload")
                        .executes(BannerCommand::reload))

                // /squaremapbanner blacklist list|add|remove|purge
                .then(Commands.literal("blacklist")
                        .then(Commands.literal("list")
                                .executes(BannerCommand::blacklistList))
                        .then(Commands.literal("add")
                                .then(Commands.argument("word", StringArgumentType.greedyString())
                                        .executes(BannerCommand::blacklistAdd)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("word", StringArgumentType.greedyString())
                                        .suggests(BLACKLIST_WORDS)
                                        .executes(BannerCommand::blacklistRemove)))
                        .then(Commands.literal("purge")
                                .executes(BannerCommand::blacklistPurge))));
    }

    // ------------------------------------------------------------------ list

    private record Row(String worldKey, String name, BlockPos pos, String owner) {
    }

    private static int list(CommandContext<CommandSourceStack> ctx, String player) {
        List<Row> rows = new ArrayList<>();

        bannerManager.bannerManager.forEach((worldKey, w) -> w.getData().forEach((position, data) -> {
            if (player != null && !player.equalsIgnoreCase(data.ownerName()))
                return;

            rows.add(new Row(worldKey, data.name(), position.loc(), data.ownerName() == null ? "unknown" : data.ownerName()));
        }));

        if (rows.isEmpty()) {
            fail(ctx, player == null ? "No banners are registered." : "No banners found for " + player + ".");
            return 0;
        }

        rows.sort(Comparator.comparing(Row::worldKey).thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));

        StringBuilder text = new StringBuilder("Banners (" + rows.size() + ")");
        int shown = Math.min(rows.size(), MAX_LIST_LINES);

        for (int i = 0; i < shown; i++) {
            Row row = rows.get(i);
            text.append('\n').append(row.name())
                    .append(" - ").append(row.worldKey()).append(' ')
                    .append(row.pos().getX()).append(' ').append(row.pos().getY()).append(' ').append(row.pos().getZ())
                    .append(" (").append(row.owner()).append(')');
        }

        if (rows.size() > shown)
            text.append("\n... and ").append(rows.size() - shown).append(" more");

        success(ctx, text.toString(), false);
        return rows.size();
    }

    // ------------------------------------------------------------------ tp

    private record Hit(String worldKey, BlockPos pos, String name) {
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        String query = StringArgumentType.getString(ctx, "name").trim().toLowerCase(Locale.ROOT);

        List<Hit> exact = new ArrayList<>();
        List<Hit> partial = new ArrayList<>();

        bannerManager.bannerManager.forEach((worldKey, w) -> w.getData().forEach((position, data) -> {
            String name = data.name().toLowerCase(Locale.ROOT);

            if (name.equals(query))
                exact.add(new Hit(worldKey, position.loc(), data.name()));
            else if (name.contains(query))
                partial.add(new Hit(worldKey, position.loc(), data.name()));
        }));

        //an exact name wins over "contains" matches
        List<Hit> candidates = exact.isEmpty() ? partial : exact;

        if (candidates.isEmpty()) {
            fail(ctx, "No banner found matching \"" + query + "\".");
            return 0;
        }

        //several matches: take the closest one (banners in the player's own dimension first)
        String currentWorld = player.level().dimension().identifier().toString();
        Hit target = Collections.min(candidates, Comparator.<Hit>comparingDouble(hit ->
                hit.worldKey().equals(currentWorld)
                        ? player.distanceToSqr(hit.pos().getX() + 0.5, hit.pos().getY(), hit.pos().getZ() + 0.5)
                        : Double.MAX_VALUE));

        ServerLevel level = bannerManager.getLevel(source.getServer(), target.worldKey());
        if (level == null) {
            fail(ctx, "The dimension " + target.worldKey() + " is not loaded.");
            return 0;
        }

        player.teleportTo(level,
                target.pos().getX() + 0.5, target.pos().getY(), target.pos().getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot(), true);

        String others = candidates.size() > 1 ? " (" + (candidates.size() - 1) + " more match, took the closest)" : "";
        success(ctx, "Teleported to banner \"" + target.name() + "\"" + others, true);
        return 1;
    }

    // ------------------------------------------------------------------ remove / removeall

    private static world currentWorld(CommandContext<CommandSourceStack> ctx) {
        return bannerManager.bannerManager.get(ctx.getSource().getLevel().dimension().identifier().toString());
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        Position position = Position.of(pos);

        world w = currentWorld(ctx);
        if (w == null || !w.getData().containsKey(position)) {
            fail(ctx, "There is no registered banner at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " in this dimension.");
            return 0;
        }

        w.removeBanner(position);
        bannerManager.save();

        success(ctx, "Removed the banner marker at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " (the banner block itself is untouched).", true);
        return 1;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx) {
        world w = currentWorld(ctx);
        if (w == null || w.getData().isEmpty()) {
            fail(ctx, "There are no registered banners in this dimension.");
            return 0;
        }

        //copy: removeBanner modifies the underlying map
        List<Position> positions = new ArrayList<>(w.getBanners());
        positions.forEach(w::removeBanner);
        bannerManager.save();

        success(ctx, "Removed " + positions.size() + " banner markers from this dimension.", true);
        return positions.size();
    }

    // ------------------------------------------------------------------ status

    /**
     * Diagnostics that do not need access to the server log.
     */
    private static int status(CommandContext<CommandSourceStack> ctx) {
        StringBuilder text = new StringBuilder("squaremap-banner status (build " + Squaremapbanner.BUILD_STAMP + ")");

        Path file = saveDataHandler.dataFile().toAbsolutePath();
        text.append("\nData file: ").append(file);
        try {
            if (Files.exists(file))
                text.append(" (").append(Files.size(file)).append(" bytes, modified ").append(Files.getLastModifiedTime(file)).append(')');
            else
                text.append(" (does not exist yet)");
        } catch (IOException e) {
            text.append(" (cannot read file info: ").append(e.getMessage()).append(')');
        }

        String lastError = saveDataHandler.lastError();
        text.append("\nLast save: ").append(lastError == null ? "OK" : "FAILED - " + lastError);

        if (bannerManager.bannerManager.isEmpty()) {
            text.append("\nTracked dimensions: NONE (squaremap did not report any enabled world to this mod)");
        } else {
            text.append("\nTracked dimensions:");
            bannerManager.bannerManager.forEach((key, w) ->
                    text.append("\n  ").append(key).append(": ").append(w.getData().size()).append(" banners"));
        }

        text.append("\nConfig: cooldown ").append(modConfigs.registerCooldownSeconds).append("s, cleanup every ")
                .append(modConfigs.cleanupIntervalSeconds).append("s, radius ").append(modConfigs.radius)
                .append(", blacklist ").append(modConfigs.blacklist.size()).append(" words");

        text.append("\nDebug: ").append(modConfigs.debug ? "ON" : "off").append(", log file ")
                .append(DebugLog.file().toAbsolutePath());

        success(ctx, text.toString(), false);
        return 1;
    }

    // ------------------------------------------------------------------ debug

    private static int setDebug(CommandContext<CommandSourceStack> ctx, boolean on) {
        //runtime only; to keep it after a restart set "debug" in config/squaremap_banner.json
        modConfigs.debug = on;

        if (on)
            DebugLog.log("debug switched ON by " + ctx.getSource().getTextName());

        success(ctx, "Banner debug " + (on ? "ON, writing to " + DebugLog.file().toAbsolutePath() : "off")
                + " (until restart; set \"debug\" in the config to keep it)", true);
        return 1;
    }

    /**
     * Shows the last lines of the debug log in chat, for servers without access to the files.
     */
    private static int debugTail(CommandContext<CommandSourceStack> ctx) {
        Path file = DebugLog.file();

        try {
            if (!Files.exists(file)) {
                fail(ctx, "There is no debug log yet. Turn debug on and try registering a banner.");
                return 0;
            }

            List<String> lines = Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - 15);

            success(ctx, "Last " + (lines.size() - from) + " debug lines:\n" + String.join("\n", lines.subList(from, lines.size())), false);
            return lines.size() - from;
        } catch (IOException e) {
            fail(ctx, "Could not read the debug log: " + e.getMessage());
            return 0;
        }
    }

    // ------------------------------------------------------------------ reload

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            //strict read: on an error the running config stays untouched
            Squaremapbanner.modConfigs = ConfigHandler.readConfig();
        } catch (IOException e) {
            Squaremapbanner.LOGGER.error("Config reload failed", e);
            fail(ctx, "Could not reload the config, keeping the current one: " + e.getMessage());
            return 0;
        }

        success(ctx, "squaremap-banner config reloaded.", true);
        return 1;
    }

    // ------------------------------------------------------------------ blacklist

    private static int blacklistList(CommandContext<CommandSourceStack> ctx) {
        List<String> words = new ArrayList<>(modConfigs.blacklist);
        Collections.sort(words);

        if (words.isEmpty())
            success(ctx, "The blacklist is empty.", false);
        else
            success(ctx, "Blacklisted words (" + words.size() + "): " + String.join(", ", words), false);

        return words.size();
    }

    private static int blacklistAdd(CommandContext<CommandSourceStack> ctx) {
        String word = StringArgumentType.getString(ctx, "word").trim().toLowerCase(Locale.ROOT);

        if (word.isEmpty()) {
            fail(ctx, "The word cannot be empty.");
            return 0;
        }

        if (modConfigs.blacklist.stream().anyMatch(existing -> existing.equalsIgnoreCase(word))) {
            fail(ctx, "\"" + word + "\" is already on the blacklist.");
            return 0;
        }

        modConfigs.blacklist.add(word);
        ConfigHandler.saveConfig(modConfigs);

        int alreadyUsed = 0;
        for (world w : bannerManager.bannerManager.values()) {
            for (var data : w.getData().values()) {
                if (wordCheck.containsWholeWord(data.name(), word))
                    alreadyUsed++;
            }
        }

        String hint = alreadyUsed > 0
                ? " " + alreadyUsed + " registered banner(s) already use it, run /squaremapbanner blacklist purge to remove them from the map."
                : "";
        success(ctx, "Added \"" + word + "\" to the blacklist." + hint, true);
        return 1;
    }

    private static int blacklistRemove(CommandContext<CommandSourceStack> ctx) {
        String word = StringArgumentType.getString(ctx, "word").trim();

        if (!modConfigs.blacklist.removeIf(existing -> existing.equalsIgnoreCase(word))) {
            fail(ctx, "\"" + word + "\" is not on the blacklist.");
            return 0;
        }

        ConfigHandler.saveConfig(modConfigs);
        success(ctx, "Removed \"" + word + "\" from the blacklist.", true);
        return 1;
    }

    /**
     * Removes every registered banner whose name contains a currently blacklisted word.
     */
    private static int blacklistPurge(CommandContext<CommandSourceStack> ctx) {
        int removed = 0;

        for (world w : bannerManager.bannerManager.values()) {
            List<Position> toRemove = new ArrayList<>();

            w.getData().forEach((position, data) -> {
                if (bannerRegister.findBlacklistedWord(data.name()) != null)
                    toRemove.add(position);
            });

            toRemove.forEach(w::removeBanner);
            removed += toRemove.size();
        }

        if (removed == 0) {
            success(ctx, "No registered banner contains a blacklisted word.", false);
            return 0;
        }

        bannerManager.save();
        success(ctx, "Removed " + removed + " banner markers with blacklisted names.", true);
        return removed;
    }

    // ------------------------------------------------------------------ helpers

    private static void success(CommandContext<CommandSourceStack> ctx, String message, boolean broadcastToAdmins) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), broadcastToAdmins);
    }

    private static void fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
    }
}