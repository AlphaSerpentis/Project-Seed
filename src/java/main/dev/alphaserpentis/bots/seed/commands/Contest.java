package dev.alphaserpentis.bots.seed.commands;

import dev.alphaserpentis.bots.seed.data.contest.SeedContestResults;
import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.bots.seed.handler.ContestHandler;
import dev.alphaserpentis.bots.seed.handler.SeedServerDataHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.io.IOException;
import java.util.List;

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
                        TypeOfEphemeral.DEFAULT,
                        true
                )
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        SeedServerDataHandler sdh = (SeedServerDataHandler) core.getServerDataHandler();
        String subcommand = event.getSubcommandName();
        String subcommandGroup = event.getSubcommandGroup();
        Guild guild = event.getGuild();
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());

        if(subcommandGroup.equalsIgnoreCase("config")) {
            if(!serverData.isMemberPermissioned(event.getMember())) {
                return new CommandResponse<>(PERMISSION_DENIED, isOnlyEphemeral());
            }

            switch(subcommand) {
                case "start" -> startContest(guild, eb, sdh);
                case "end" -> endContest(guild, eb, sdh);
                case "addperm" -> addPermission(guild, event.getOptions().get(0).getAsLong(), eb, sdh);
                case "removeperm" -> removePermission(guild, event.getOptions().get(0).getAsLong(), eb, sdh);
                case "addprompt" -> addPrompt(guild, event.getOptions().get(0).getAsString(), eb, sdh);
                case "removeprompt" -> removePrompt(guild, event.getOptions().get(0).getAsString(), eb, sdh);
                case "leaderboard" -> setLeaderboardChannel(guild, event.getOptions().get(0).getAsLong(), eb, sdh);
                case "channel" -> setContestChannel(guild, event.getOptions().get(0).getAsLong(), eb, sdh);
                case "length" -> setContestLength(guild, event.getOptions().get(0).getAsLong(), eb, sdh);
                case "recurring" -> setContestRecurring(guild, event.getOptions().get(0).getAsBoolean(), eb, sdh);
            }
        } else {
            switch(subcommand) {
                case "contest" -> viewPastContests(guild, event.getOptions().get(0).getAsInt(), eb, sdh);
                case "prompts" -> viewPrompts(guild, eb, sdh);
            }
        }

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandGroupData config = new SubcommandGroupData("config", "Configure the contest")
                .addSubcommands(
                        new SubcommandData("start", "Start the contest. If a contest is already running, this will do nothing."),
                        new SubcommandData("end", "End the contest. If a contest is running, it will be ended and the winner will be announced."),
                        new SubcommandData("addperm", "Adds a user or role to be permissioned to configure the contest.").addOption(OptionType.MENTIONABLE, "mention", "The user or role to add.", true),
                        new SubcommandData("removeperm", "Removes a user or role from being permissioned to configure the contest.").addOption(OptionType.MENTIONABLE, "mention", "The user or role to remove.", true),
                        new SubcommandData("addprompt", "Adds a prompt to the list of prompts.").addOption(OptionType.STRING, "prompt", "The prompt to add.", true),
                        new SubcommandData("removeprompt", "Removes a prompt from the list of prompts.").addOption(OptionType.STRING, "prompt", "The prompt to remove.", true),
                        new SubcommandData("leaderboard", "Sets the channel where the leaderboard will be at").addOption(OptionType.CHANNEL, "channel", "The channel to set.", true),
                        new SubcommandData("channel", "Sets the channel where the contest will be at").addOption(OptionType.CHANNEL, "channel", "The channel to set.", true),
                        new SubcommandData("length", "Sets the length of the contest in days").addOption(OptionType.INTEGER, "days", "The number of days to set.", true),
                        new SubcommandData("recurring", "Sets whether the contest will be recurring or not").addOption(OptionType.BOOLEAN, "recurring", "Whether the contest will be recurring or not.", true)
                );

        SubcommandGroupData view = new SubcommandGroupData("view", "View contest-related information")
                .addSubcommands(
                        new SubcommandData("contest", "View past contests").addOption(OptionType.INTEGER, "id", "The ID of the contest to view.", true),
                        new SubcommandData("prompts", "View the list of prompts added to the bot")
                );

        jda.upsertCommand(name, description).addSubcommandGroups(config, view).queue((cmd) -> setCommandId(cmd.getIdLong()));
    }

    private void startContest(@NonNull Guild guild, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        if(ContestHandler.verifyConfiguration(serverData)) {
            if(serverData.isContestRunning()) {
                eb.setDescription("A contest is already running.");
                eb.setColor(0xff0000);
            } else {
                eb.setDescription("The contest has been started.");
                eb.setColor(0x00ff00);

                try {
                    ContestHandler.startContest(guild, serverData);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            eb.setDescription(
                    "The contest cannot be started because the configuration is not complete. " +
                            "Ensure that the leaderboard channel and contest channel are set. " +
                            "Run </contest config leaderboard:" + getCommandId() + "> and </contest config channel:" + getCommandId() + "> to set them."
            );
            eb.setColor(0xff0000);
        }
    }

    private void endContest(@NonNull Guild guild, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        if(serverData.isContestRunning()) {
            eb.setDescription("The contest has been ended.");
            eb.setColor(0x00ff00);

            try {
                ContestHandler.endContest(guild, serverData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            eb.setDescription("There is no contest running.");
            eb.setColor(0xff0000);
        }
    }

    private void addPermission(@NonNull Guild guild, long id, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        // Determine if id is a user or a role
        try {
            guild.retrieveMemberById(id).complete();

            SeedServerData serverData = sdh.getServerData(guild.getIdLong());
            serverData.addPermissionedUser(id);

            eb.setDescription("<@" + id + "> has been added to the list of permissioned users.");
            eb.setColor(0x00ff00);
        } catch(ErrorResponseException e) {
            if(guild.getRoleById(id) == null) {
                eb.setDescription("The mentionable could not be found.");
                eb.setColor(0xff0000);
                return;
            } else {
                SeedServerData serverData = sdh.getServerData(guild.getIdLong());
                serverData.addPermissionedRole(id);
                eb.setDescription("<@&" + id + "> has been added to the list of permissioned roles.");
                eb.setColor(0x00ff00);
            }
        }

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removePermission(@NonNull Guild guild, long id, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        // Determine if id is a user or a role
        try {
            guild.retrieveMemberById(id).complete();

            SeedServerData serverData = sdh.getServerData(guild.getIdLong());
            serverData.removePermissionedUser(id);

            eb.setDescription("<@" + id + "> has been removed from the list of permissioned users.");
            eb.setColor(0x00ff00);
        } catch(ErrorResponseException e) {
            if(guild.getRoleById(id) == null) {
                eb.setDescription("The mentionable could not be found.");
                eb.setColor(0xff0000);
                return;
            } else {
                SeedServerData serverData = sdh.getServerData(guild.getIdLong());
                serverData.removePermissionedRole(id);
                eb.setDescription("<@&" + id + "> has been removed from the list of permissioned roles.");
                eb.setColor(0x00ff00);
            }
        }

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addPrompt(@NonNull Guild guild, @NonNull String prompt, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        serverData.addPrompt(prompt);

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eb.setDescription(String.format("The prompt \"%s\" has been added to the list of prompts.", prompt));
        eb.setColor(0x00ff00);
    }

    private void removePrompt(@NonNull Guild guild, @NonNull String prompt, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        serverData.removePrompt(prompt);

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eb.setDescription(String.format("The prompt \"%s\" has been removed from the list of prompts.", prompt));
        eb.setColor(0x00ff00);
    }

    private void setLeaderboardChannel(@NonNull Guild guild, long id, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        serverData.setLeaderboardChannelId(id);

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eb.setDescription("The leaderboard channel has been set.");
        eb.setColor(0x00ff00);
    }

    private void setContestChannel(@NonNull Guild guild, long id, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        serverData.setContestChannelId(id);

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eb.setDescription("The contest channel has been set.");
        eb.setColor(0x00ff00);
    }

    private void setContestLength(@NonNull Guild guild, long days, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        serverData.setLengthOfContestInSeconds(days * 24 * 60 * 60);

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eb.setDescription("The contest length has been set.");
        eb.setColor(0x00ff00);
    }

    private void setContestRecurring(@NonNull Guild guild, boolean recurring, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        serverData.setContestRecurring(recurring);

        try {
            sdh.updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eb.setDescription("The recurring contest setting has been set.");
        eb.setColor(0x00ff00);
    }

    private void viewPastContests(@NonNull Guild guild, int contestId, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        SeedContestResults pastContest = serverData.getContestResults().get(contestId-1);

        if(pastContest != null) {
            eb.setTitle("Past Contest #" + contestId);
            eb.setDescription("The following are the results of the past contest.");
            eb.addField("Prompt", pastContest.contestPrompt(), false);
            eb.addField("Winner", "<@" + pastContest.getParticipants().get(0) + ">", false);
            eb.addField("Entries", String.valueOf(pastContest.participants().size()), false);
            eb.addField("Date", "The contest happened from <t:" + pastContest.contestStartedTimestamp() + "> and ended at <t:" + pastContest.contestEndedTimestamp() + ">.", false);
            eb.setColor(0x00ff00);
        } else {
            eb.setDescription("There is no contest with that ID.");
            eb.setColor(0xff0000);
        }
    }

    private void viewPrompts(@NonNull Guild guild, @NonNull EmbedBuilder eb, @NonNull SeedServerDataHandler sdh) {
        SeedServerData serverData = sdh.getServerData(guild.getIdLong());
        List<String> prompts = serverData.getPrompts();

        if(prompts.size() > 0) {
            eb.setTitle("Prompts");
            eb.setDescription("Currently added prompts:");
            for(int i = 0; i < prompts.size(); i++) {
                eb.addField("Prompt #" + (i+1), prompts.get(i), false);
            }
            eb.setColor(0x00ff00);
        } else {
            eb.setDescription("There are no prompts available.");
            eb.setColor(0xff0000);
        }
    }
}
