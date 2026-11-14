# SquareMap Banner Reboot

A community-maintained continuation of the original **SquareMap Banner** mod for Minecraft Fabric.

SquareMap Banner Reboot maintains the original functionality while bringing updated Minecraft compatibility and providing a foundation for future improvements.

<p align="center">
  <img alt="SquareMap Banner preview" src="images/preview.png" />
</p>

## Features

- **Banner Markers:** Sneak + right-click with a named banner while holding a filled map to add the banner to the SquareMap world map.
- **Banner Removal:** Sneak + left-click with a named banner while holding a filled map, or break the banner, to remove its marker from the map.
- **Area Notifications:** Displays a message when entering an area marked by a banner.

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

## Configuration

The mod provides a JSON configuration file with the following options:

- `radius`: Specifies the size of the area declared by the banner.
- `announce_when_near_banner`: Enables or disables notifications when entering a banner's area.
- `blacklist`: Contains a list of words that should not be registered on the world map.

## Permissions

- `squaremap.banner.use`: Permission required for players to use the mod's features.
- Without a permission manager, only players with permission level 1 or higher can use the mod.

## Support and Feedback

Found a bug or have an idea for an improvement?

Please open an issue in the [GitHub Issue Tracker](https://github.com/KutiFF/squaremap_banner_reboot/issues).

## Credits

- **FaeWulf** – Original creator of SquareMap Banner.
- **Acuadragon100** – Minecraft 26.1 compatibility update.
- **KutiFF** – Minecraft 26.3 compatibility, ongoing maintenance and further development.

Original project: [FaeWulf/squaremap_banner](https://github.com/FaeWulf/squaremap_banner)

## License

This project is released under **CC0 1.0 Universal**.

See [LICENSE](LICENSE) for the full license text.