package dev.alphaserpentis.bots.seed.handler;

import dev.alphaserpentis.bots.seed.data.contest.SeedContestResults;
import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ContestHandler {

    public static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    public static ScheduledFuture<?> scheduledFuture;
    public static CoffeeCore core;

    public static void init(@NonNull CoffeeCore core, @NonNull Guild guild, @NonNull SeedServerData serverData) throws IOException {
        ContestHandler.core = core;

        if(serverData.isContestRunning()) {
            // Verify the contest is still running
            if(serverData.getContestEndingTimestamp() <= System.currentTimeMillis() / 1000) {
                endContest(guild, serverData);
            } else {
                scheduledFuture = executorService.schedule(
                        () -> {
                            try {
                                endContest(guild, serverData);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        },
                        serverData.getContestEndingTimestamp() - System.currentTimeMillis() / 1000,
                        TimeUnit.SECONDS
                );
            }
        }
    }

    public static void startContest(@NonNull Guild guild, @NonNull SeedServerData serverData) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;
        String prompt = serverData.getContestPrompt();

        serverData.setContestEndingTimestamp(
                currentTimeInSeconds + serverData.getLengthOfContestInSeconds()
        );
        serverData.setContestStartingTimestamp(currentTimeInSeconds);
        serverData.addPromptToPreviouslyUsedPrompts(prompt);
        core.getServerDataHandler().updateServerData();

        // Schedule the end of the contest
        scheduledFuture = executorService.schedule(
                () -> {
                    try {
                        endContest(guild, serverData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                },
                serverData.getLengthOfContestInSeconds(),
                TimeUnit.SECONDS
        );

        // Construct the MessageEmbed
        eb.setTitle("Contest Started");
        eb.setDescription("A new contest has started! The prompt for this contest is: \n\n**" + prompt + "**");
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
                "<t:" + serverData.getContestEndingTimestamp() + ":R>",
                false
        );

        // Send messsage to the channel
        guild.getTextChannelById(serverData.getContestChannelId()).sendMessageEmbeds(
                eb.build()
        ).complete();
    }
    public static void endContest(@NonNull Guild guild, @NonNull SeedServerData serverData) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();

        if(scheduledFuture != null)
            scheduledFuture.cancel(false);

        // Send a contest ending message
        eb.setTitle("Contest Ended!");
        eb.setDescription("The contest has ended! The results will be in <#" + serverData.getLeaderboardChannelId() + "> shortly!");
        guild.getTextChannelById(serverData.getContestChannelId()).sendMessageEmbeds(
                eb.build()
        ).complete();

        // Set the contest results
        HashMap<Long, Integer> participants = getContestParticipants(serverData);
        SeedContestResults contestResults = new SeedContestResults(
                participants,
                serverData.getCurrentPrompt(),
                serverData.getContestStartingTimestamp(),
                serverData.getContestEndingTimestamp(),
                serverData.getContestResults().size() + 1
        );

        // Construct the MessageEmbed
        eb = new EmbedBuilder();
        eb.setTitle("Final Results");
        eb.setDescription("The contest has ended! Here are the final results:");
        eb.addField(
                "Prompt",
                contestResults.contestPrompt(),
                false
        );

        long prevScore = 0;
        long adjustedPlace = 0;
        boolean previousWasTie = false;
        for(int i = 0; i < contestResults.getSortedParticipants().size() && i < 5 + adjustedPlace; i++) {
            long score = contestResults.contestParticipants().get(contestResults.getSortedParticipants().get(i));
            if(prevScore == score && !previousWasTie) {
                previousWasTie = true;
            } else {
                previousWasTie = false;
                prevScore = score;
            }

            if(previousWasTie) {
                adjustedPlace++;
            }

            long userId = contestResults.getSortedParticipants().get(i);
            eb.addField(
                    "Place " + (i + 1 - adjustedPlace) + (previousWasTie ? " (Tie)" : ""),
                    "<@" + userId + "> with " + contestResults.contestParticipants().get(userId) + " votes",
                    false
            );
        }

        // Send messsage to the channel
        guild.getTextChannelById(serverData.getLeaderboardChannelId()).sendMessageEmbeds(
                eb.build()
        ).complete();

        serverData.addContestResults(contestResults);

        if(serverData.isContestRecurring()) {
            startContest(guild, serverData);
        } else {
            serverData.setContestStartingTimestamp(0);
            serverData.setContestEndingTimestamp(0);
            core.getServerDataHandler().updateServerData();
        }
    }

    public static boolean verifyConfiguration(@NonNull SeedServerData serverData) {
        return serverData.getContestChannelId() != 0 && serverData.getLeaderboardChannelId() != 0;
    }

    public static HashMap<Long, Integer> getContestParticipants(@NonNull SeedServerData serverData) {
        HashMap<Long, Integer> participants = new HashMap<>();
        ArrayList<Message> eligibleMessages = new ArrayList<>();
        MessageChannel contestChannel = core.getJda().getTextChannelById(serverData.getContestChannelId());
        MessageHistory messageHistory;
        AtomicBoolean messagePastTimestamp = new AtomicBoolean(false);

        // Start search after the contest started message
        messageHistory = contestChannel.getHistoryBefore(
                contestChannel.getLatestMessageId(),
                100
        ).complete();

        while(!messagePastTimestamp.get()) {
            messageHistory.getRetrievedHistory().forEach(
                    message -> {
                        if(message.getTimeCreated().toInstant().getEpochSecond() > serverData.getContestStartingTimestamp()) {
                            if(message.getAttachments().size() > 0)
                                eligibleMessages.add(message);
                        } else {
                            messagePastTimestamp.set(true);
                        }
                    }
            );

            if(!messagePastTimestamp.get() && messageHistory.getRetrievedHistory().size() < 100)
                messagePastTimestamp.set(true);

            // If we haven't reached the end of the submissions
            if(!messagePastTimestamp.get() && messageHistory.getRetrievedHistory().size() == 100) {
                messageHistory = contestChannel.getHistoryBefore(
                        messageHistory.getRetrievedHistory().get(messageHistory.getRetrievedHistory().size() - 1).getId(),
                        100
                ).complete();
            }
        }

        for(Message msg: eligibleMessages) {
            long authorId = msg.getAuthor().getIdLong();

            if(participants.containsKey(authorId)) { // Checks if this user already participated
                int voteCount = countVotes(msg);
                if(participants.get(authorId) < voteCount) { // Only puts in the new submission if it received more votes
                    participants.put(authorId, voteCount);
                }
            }

            participants.put(authorId, countVotes(msg));
        }

        return participants;
    }

    public static int countVotes(@NonNull Message message) {
        ArrayList<MessageReaction> reactions = new ArrayList<>(message.getReactions());
        HashMap<Long, Boolean> voters = new HashMap<>();
        AtomicInteger votes = new AtomicInteger();

        for(MessageReaction reaction: reactions) {
            reaction.retrieveUsers().complete().forEach(
                    user -> {
                        if(!voters.containsKey(user.getIdLong())) {
                            voters.put(user.getIdLong(), true);
                            votes.getAndIncrement();
                        }
                    }
            );
        }

        return votes.get();
    }
}