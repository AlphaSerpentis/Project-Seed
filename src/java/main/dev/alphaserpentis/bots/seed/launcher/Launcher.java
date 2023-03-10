package dev.alphaserpentis.bots.seed.launcher;

import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;

public class Launcher {
    public static CoffeeCore core;

    public static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load();
        CoffeeCoreBuilder builder = new CoffeeCoreBuilder();

        builder.setSettings(
                new BotSettings(
                        Long.getLong(dotenv.get("BOT_OWNER_ID")),
                        dotenv.get("SERVER_DATA_PATH"),
                        Boolean.parseBoolean(dotenv.get("UPDATE_COMMANDS_AT_LAUNCH")),
                        Boolean.parseBoolean(dotenv.get("REGISTER_DEFAULT_COMMANDS"))
                )
        ).setServerDataClass(SeedServerData.class);

        core = builder.build(dotenv.get("DISCORD_BOT_TOKEN"));
    }
}
