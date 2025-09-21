# VouchForMe Plugin



A Bukkit/Spigot plugin that allows players to vouch for and devouch other players with configurable cooldowns and MiniMessage formatting support.



## Features



- Vouch system: Players can vouch for other players with optional reasons

- Devouch system: Players can remove their vouches with optional reasons

- Configurable cooldown: Set custom cooldown periods (default 30 days)

- MiniMessage support: Full MiniMessage formatting for all messages

- SQLite database: Efficient database storage with in-memory caching

- Administrative commands: Reload configuration without restarting



## Commands



- ``/vouch <player> [reason]`` - Vouch for a player

- ``/devouch <player> [reason]`` - Devouches a player (negative review)

- ``/remvouch <player> [reason]`` - Remove your vouch from a player
  
- ``/vouches <player>`` - View all vouches for a player

- ``/vouch reload`` - Reload plugin configuration (requires permission)



## Permissions



- vouchforme.use - Allow using vouch commands (default true)

- vouchforme.reload - Allow reloading configuration (default op)

- vouchforme.admin - Administrative permissions (default op)



## Installation



1. Download the plugin JAR file from [releases](https://github.com/rogueliver/vouchforme/releases) (or build it yourself)

2. Place it in your server's plugins folder

3. Start or restart your server

4. Configure the plugin in `plugins/VouchForMe/config.yml`



## Configuration



The plugin creates a `config.yml` file with these options:



- Cooldown duration: How long players must wait between vouches

- Devouch cooldown: Whether devouching applies a cooldown

- Messages: Customize all messages with MiniMessage formatting

- Time format: Configure how cooldown time is displayed



## Building



Requires Java 17 or higher and Maven.



```bash

mvn clean package

```

The compiled JAR will be in the ``target`` directory.



## License

This project is under the MIT License. See the [LICENSE](LICENSE) file for details.

## Author
Made by rogueliver (RL)

* GitHub: https://github.com/rogueliver
* Discord: https://discord.com/users/1354013258884972610
