# SquareMap Banner Reboot

A community-maintained continuation of the original **SquareMap Banner** mod for Minecraft Fabric.

SquareMap Banner Reboot keeps the original functionality, brings updated Minecraft compatibility and adds admin tools, spam protection and self-maintaining markers.

<p align="center">
  <img alt="SquareMap Banner preview" src="images/preview.png" />
</p>

## Features

- **Banner Markers:** Sneak + right-click with a named banner while holding a filled map to add the banner to the SquareMap world map. You get a confirmation message.
- **Banner Removal:** Sneak + left-click with a named banner while holding a filled map, or break the banner, to remove its marker from the map.
- **Self-cleaning markers:** Markers of banners that disappeared without a player breaking them (explosions, fire, pistons, water, ...) are removed automatically by a lightweight background check. It spreads the work over many ticks and never loads chunks.
- **Area Notifications:** Displays a message when entering an area marked by a banner.
- **Registration cooldown:** Players can only register a banner every few seconds (configurable, admins are exempt).
- **Banner owners:** The player who registers a banner is remembered, so admins can list the banners of a player.
- **Admin commands:** List, teleport to and remove banners, manage the blacklist and reload the config without a restart.
- **Safer data files:** Files are written safely, damaged files are backed up instead of being overwritten, and a single broken entry no longer breaks the whole save file.

<p align="center">
  <img alt="Additional preview" src="images/preview_extra.png" />
</p>

## Compatibility

| Component | Requirement |
|---|---|
| Minecraft | 26.3 or newer |
| Mod Loader | Fabric |
| Java | 25 or newer |
| Fabric API | Required |
| SquareMap | Required |

## Installation

1. Install Fabric Loader for a supported Minecraft version.
2. Install Fabric API and SquareMap.
3. Download the latest SquareMap Banner Reboot JAR from [GitHub Releases](https://github.com/KutiFF/squaremap_banner_reboot/releases).
4. Place the JAR file in your server's `mods` directory.

**Note:** Remove any previous version of SquareMap Banner before installing SquareMap Banner Reboot. Both versions use the same Fabric mod ID.

**Updating from 1.0.0:** Existing `banner.json` and config files keep working. New config options are added automatically with default values on the first start. Banners registered before the update have no owner.

## Commands

All commands require the `squaremap.banner.admin` permission.

| Command | Description |
|---|---|
| `/squaremapbanner list [player]` | Lists registered banners, or only the banners registered by a player. |
| `/squaremapbanner tp <name>` | Teleports you to a banner. If several banners match, the closest one is used. |
| `/squaremapbanner remove <x y z>` | Removes the marker of the banner at a position in your current dimension. |
| `/squaremapbanner removeall confirm` | Removes all markers in your current dimension. |
| `/squaremapbanner reload` | Reloads the config file. On an error the running config is kept. |
| `/squaremapbanner blacklist list` | Shows the blacklisted words. |
| `/squaremapbanner blacklist add <word>` | Adds a word to the blacklist. |
| `/squaremapbanner blacklist remove <word>` | Removes a word from the blacklist. |
| `/squaremapbanner blacklist purge` | Removes all registered banners whose name contains a blacklisted word. |
| `/squaremapbanner status` | Shows the data file, last save result, tracked dimensions and the active settings. |
| `/squaremapbanner debug on\|off\|tail` | Switches the debug log on or off, or shows its last lines in chat. |

## Configuration

The mod provides a JSON configuration file (`config/squaremap_banner.json`) with the following options:

| Option | Default | Description |
|---|---|---|
| `banner_radius` | `80` | Size of the area declared by a banner. |
| `announce_when_near_banner` | `true` | Enables or disables notifications when entering a banner's area. |
| `register_cooldown_seconds` | `10` | Minimum time between two registrations by the same player. `0` disables it. Admins are exempt. |
| `cleanup_interval_seconds` | `30` | Pause between two checks for banners that no longer exist. `0` disables the check. |
| `cleanup_banners_per_tick` | `20` | Maximum number of banners checked per server tick during a check. |
| `blacklist` | `["nigga", "slut"]` | Words that must not appear in a registered banner name (whole words, case-insensitive, Unicode-aware). |
| `debug` | `false` | Writes a step-by-step trace to `squaremap/banner-debug.log`. Only enable it while looking for a problem. |

Use `/squaremapbanner reload` to apply manual edits without restarting. The blacklist commands save the config file themselves, so run `reload` first if you also edited the file by hand.

Banner data is stored in `squaremap/banner.json`. A file that cannot be read is copied to `banner.json.broken-<timestamp>` before anything is overwritten.

## Permissions

- `squaremap.banner.use`: Permission required for players to add and remove banner markers.
- `squaremap.banner.admin`: Permission required for the `/squaremapbanner` commands. Also bypasses the registration cooldown.
- Without a permission manager, `squaremap.banner.use` requires permission level 1 or higher and `squaremap.banner.admin` requires level 2 or higher.

## Support and Feedback

Found a bug or have an idea for an improvement?

Please open an issue in the [GitHub Issue Tracker](https://github.com/KutiFF/squaremap_banner_reboot/issues). If something does not work, turn on `debug` and attach `squaremap/banner-debug.log`.

## Credits

- **FaeWulf** – Original creator of SquareMap Banner.
- **Acuadragon100** – Minecraft 26.1 compatibility update.
- **KutiFF** – Minecraft 26.3 compatibility, ongoing maintenance and further development.

Original project: [FaeWulf/squaremap_banner](https://github.com/FaeWulf/squaremap_banner)

## License

This project is released under **CC0 1.0 Universal**.

See [LICENSE](LICENSE) for the full license text.