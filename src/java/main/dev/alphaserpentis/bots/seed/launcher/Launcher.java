package dev.alphaserpentis.bots.seed.launcher;

import dev.alphaserpentis.bots.seed.commands.Contest;
import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.bots.seed.handler.ContestHandler;
import dev.alphaserpentis.bots.seed.handler.OpenAIHandler;
import dev.alphaserpentis.bots.seed.handler.SeedServerDataHandler;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.AboutInformation;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Launcher {
    public static CoffeeCore core;

    public static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load();
        CoffeeCoreBuilder<?> builder = new CoffeeCoreBuilder<>();
        AboutInformation aboutInformation;
        Guild guild;

        aboutInformation = new AboutInformation(
                """
                        A competition bot for FemboyDAO
                        
                        [GitHub](https://github.com/AlphaSerpentis/Project-Seed)
                        """,
                "v1.0.2",
                null,
                false,
                false
        );

        builder.setSettings(
                new BotSettings(
                        Long.parseLong(dotenv.get("BOT_OWNER_ID")),
                        dotenv.get("SERVER_DATA_PATH"),
                        Boolean.parseBoolean(dotenv.get("UPDATE_COMMANDS_AT_LAUNCH")),
                        Boolean.parseBoolean(dotenv.get("REGISTER_DEFAULT_COMMANDS")),
                        aboutInformation
                )
        ).setServerDataHandler(
                new SeedServerDataHandler(
                        Path.of(dotenv.get("SERVER_DATA_PATH"))
                )
        ).setEnabledGatewayIntents(
                List.of(
                        GatewayIntent.MESSAGE_CONTENT
                )
        );

        core = builder.build(dotenv.get("DISCORD_BOT_TOKEN"));
        core.registerCommands(new Contest());

        guild = core.getJda().getGuildById(dotenv.get("GUILD_ID"));

        ContestHandler.init(
                core,
                guild,
                (SeedServerData) core.getServerDataHandler().getServerData(guild.getIdLong())
        );

        OpenAIHandler.init(dotenv.get("OPENAI_API_KEY"));
    }
}
