package dev.alphaserpentis.bots.seed.handler;

import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;

import java.io.IOException;

public class ContestHandler {

    public static void startContest(@NonNull Guild guild, @NonNull SeedServerData serverData) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();

        serverData.setContestEndingTimestamp(
                System.currentTimeMillis() / 1000 + serverData.getLengthOfContestInSeconds()
        );

        ServerDataHandler.updateServerData();

        // Construct the MessageEmbed
        eb.setTitle("Contest Started");
        eb.setDescription("A new contest has started! The prompt for this contest is: " + serverData.getContestPrompt());
        eb.addField(
                "Submitting Your Seed?",
                "To submit your seed, simply upload your seed to this channel! If you submit more than one seed, the highest-scoring seed will be used.",
                false
        );
        eb.addField(
                "Voting?",
                "To vote on a seed, react to the seed with any emoji! Only one vote per seed will be counted.",
                false
        );
        eb.addField(
                "Contest Ends At",
                "<t:" + serverData.getLengthOfContestInSeconds() + ":R>",
                false
        );

        // Send messsage to the channel
        guild.getTextChannelById(serverData.getContestChannelId()).sendMessageEmbeds(
                eb.build()
        ).complete();
    }

    public static boolean verifyConfiguration(@NonNull SeedServerData serverData) {
        return serverData.getContestChannelId() != 0 && serverData.getLeaderboardChannelId() != 0;
    }
}