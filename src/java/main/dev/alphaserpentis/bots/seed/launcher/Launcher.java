package dev.alphaserpentis.bots.seed.launcher;

import dev.alphaserpentis.bots.seed.commands.Contest;
import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.bots.seed.handler.ContestHandler;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Guild;

import java.io.IOException;

public class Launcher {
    public static CoffeeCore core;

    public static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load();
        CoffeeCoreBuilder builder = new CoffeeCoreBuilder();
        Guild guild;

        builder.setSettings(
                new BotSettings(
                        Long.getLong(dotenv.get("BOT_OWNER_ID")),
                        dotenv.get("SERVER_DATA_PATH"),
                        Boolean.parseBoolean(dotenv.get("UPDATE_COMMANDS_AT_LAUNCH")),
                        Boolean.parseBoolean(dotenv.get("REGISTER_DEFAULT_COMMANDS"))
                )
        ).setServerDataClass(SeedServerData.class);

        core = builder.build(dotenv.get("DISCORD_BOT_TOKEN"));
        core.registerCommands(new Contest());

        guild = core.getJda().getGuildById(dotenv.get("GUILD_ID"));

        ContestHandler.init(
                guild,
                (SeedServerData) ServerDataHandler.getServerData(guild.getIdLong())
        );
    }
}
