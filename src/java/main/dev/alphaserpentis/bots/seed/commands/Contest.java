package dev.alphaserpentis.bots.seed.commands;

import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.bots.seed.handler.ContestHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

public class Contest extends BotCommand<MessageEmbed> {

    private static final MessageEmbed PERMISSION_DENIED = new EmbedBuilder()
            .setTitle("Permission Denied")
            .setDescription("You do not have permission to use this command.")
            .setColor(0xff0000)
            .build();

    public Contest() {
        super(
                new BotCommandOptions(
                        "contest",
                        "Contest-related information and commands",
                        true,
                        true,
                        TypeOfEphemeral.DEFAULT
                )
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        String subcommand = event.getSubcommandName();
        String subcommandGroup = event.getSubcommandGroup();
        SeedServerData serverData = (SeedServerData) ServerDataHandler.getServerData(event.getGuild().getIdLong());

        if(subcommandGroup.equalsIgnoreCase("config")) {
            if(!serverData.isMemberPermissioned(event.getMember())) {
                return new CommandResponse<>(PERMISSION_DENIED, isOnlyEphemeral());
            }

            switch(subcommand) {
                case "start" -> startContest(event.getGuild().getIdLong(), eb);
                case "end" -> {}
            }
        }

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandGroupData config = new SubcommandGroupData("config", "Configure the contest")
                .addSubcommands(
                        new SubcommandData("start", "Start the contest. If a contest is already running, this will do nothing."),
                        new SubcommandData("end", "End the contest. If a contest is running, it will be ended and the winner will be announced. If no contest is running, this will do nothing."),
                        new SubcommandData("addperm", "Adds a user or role to be permissioned to configure the contest.").addOption(OptionType.MENTIONABLE, "mention", "The user or role to add.", true),
                        new SubcommandData("removeperm", "Removes a user or role from being permissioned to configure the contest.").addOption(OptionType.MENTIONABLE, "mention", "The user or role to remove.", true),
                        new SubcommandData("addprompt", "Adds a prompt to the list of prompts.").addOption(OptionType.STRING, "prompt", "The prompt to add.", true),
                        new SubcommandData("removeprompt", "Removes a prompt from the list of prompts.").addOption(OptionType.STRING, "prompt", "The prompt to remove.", true),
                        new SubcommandData("leaderboard", "Sets the channel where the leaderboard will be at").addOption(OptionType.CHANNEL, "channel", "The channel to set.", true),
                        new SubcommandData("channel", "Sets the channel where the contest will be at").addOption(OptionType.CHANNEL, "channel", "The channel to set.", true),
                        new SubcommandData("length", "Sets the default length of the contest in days").addOption(OptionType.INTEGER, "days", "The number of days to set.", true),
                        new SubcommandData("recurring", "Sets whether the contest will be recurring or not").addOption(OptionType.BOOLEAN, "recurring", "Whether the contest will be recurring or not.", true)
                );

        jda.upsertCommand(name, description).addSubcommandGroups(config).queue((cmd) -> setCommandId(cmd.getIdLong()));
    }

    private void startContest(long guildId, @NonNull EmbedBuilder eb) {
        SeedServerData serverData = (SeedServerData) ServerDataHandler.getServerData(guildId);
        if(ContestHandler.verifyConfiguration(serverData)) {
            if(serverData.isContestRunning()) {
                eb.setDescription("A contest is already running.");
                eb.setColor(0xff0000);
            } else {
                eb.setDescription("The contest has been started.");
                eb.setColor(0x00ff00);
            }
        } else {
            eb.setDescription("The contest cannot be started because the configuration is not complete. Ensure that the leaderboard channel and contest channel are set.");
            eb.setColor(0xff0000);
        }
    }
}
