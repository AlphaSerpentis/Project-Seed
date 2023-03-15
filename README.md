# Project Seed (Proof of Seed)

Project Seed is a custom bot built on top of [Coffee Core](https://github.com/AlphaSerpentis/CoffeeCore) to hold
automatic competitions based on prompts and submissions.

This bot was designed for FemboyDAO

## Note

This bot is only designed to run on a single server. It was not designed to be run on multiple servers.

This project requires Java 17.

## How to Run the Bot Jar

1. Download the latest release from the [releases page](https://github.com/AlphaSerpentis/Project-Seed/releases).
2. Place the jar into a directory of your choice.
3. Create a blank JSON file (you can name it whatever, but it's suggested you call it `serverData.json`).
4. Create a file named `.env` in the same directory as the jar.
5. Edit the `.env` file to contain the following:

```
DISCORD_BOT_TOKEN=YOUR_BOT_TOKEN
BOT_OWNER_ID=YOUR_DISCORD_ID
GUILD_ID=YOUR_GUILD_ID
SERVER_DATA_PATH=PATH_TO_SERVER_DATA_JSON
UPDATE_COMMANDS_AT_LAUNCH=true (this is optional, but recommended)
REGISTER_DEFAULT_COMMANDS=true (this is optional, but recommended)
```

6. Run the jar file

## How to Configure the Bot

1. To configure the bot, you must either have `Manage Server` or `Administrator` permissions on the server.
2. Run `/contest config [subcommand]` to configure the bot.