

# VouchForMe

**VouchForMe** is a Minecraft plugin that allows players to vouch or devouch for each other, with instant cross-server synchronization using Redis Pub/Sub.

## Features

* **Vouch & Devouch System** — Players can vouch or devouch others, optionally with a reason.
* **Cross-Server Synchronization** — Redis Pub/Sub ensures real-time updates across all connected servers.
* **MySQL Storage** — Stores all vouch data persistently, compatible with MariaDB.
* **Interactive GUI** — Displays vouches with player heads, pagination, and HEX color support.
* **Cooldown System** — Enforces a 30-day cooldown between vouch or devouch actions per player pair.
* **Configurable Settings** — Fully customizable messages, GUI design, and sounds.

## Commands

* `/vouch <player> [reason]` — Vouch for a player.
* `/devouch <player> [reason]` — Devouch a player.
* `/vouches <player>` — View a player’s vouches.
* `/remvouch <player>` — Remove all vouches for a player.

## Permissions

* `vouch.user` — Grants access to vouch, devouch, and view commands (default: true).
* `vouch.admin` — Grants access to administrative commands (default: op).

## Installation

1. Build the plugin using Maven:    (or download the latest release [from the releases page](https://github.com/rogueliver/vouchforme/releases))

   ```bash
   mvn clean package
   ```
2. Place the compiled JAR file in your server’s `plugins/` folder.
3. Configure the plugin settings in `config.yml`:
4. Restart/Start your server to apply the configuration.

## Configuration

All configurable options are located in `config.yml`, including:

* **Database and Redis settings**
* **Cooldown duration** (default: 30 days)
* **Messages** with HEX and gradient color support
* **GUI layout and design**
* **Sound effects** for user actions

### HEX Color Formatting

You can use the following formats in any message or GUI text:

```
<#FF0000> — Single HEX color  
<gradient:#00D9FF:#7B2FFF>Text</gradient> — Gradient color
```

## Requirements

* Spigot or Paper 1.20.4+
* Java 17 or higher
* MySQL/MariaDB database
* Redis server

## Dependencies

All required dependencies are shaded into the final JAR:

* HikariCP 5.1.0
* MariaDB Java Client 3.3.2
* Jedis 5.1.0
* Gson 2.10.1

## License ![License](https://img.shields.io/github/license/rogueliver/vouchforme)

This project is under the MIT License. See the [LICENSE](LICENSE) file for details.

## Author

Made by rogueliver (RL)

* GitHub: https://github.com/rogueliver
* Discord: https://discord.com/users/1354013258884972610


