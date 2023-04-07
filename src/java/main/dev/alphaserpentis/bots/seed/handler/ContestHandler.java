package dev.alphaserpentis.bots.seed.handler;

import com.theokanning.openai.completion.chat.ChatCompletionResult;
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

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContestHandler {

    public static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    public static ScheduledFuture<?> scheduledFuture;
    public static CoffeeCore core;
    private static final String COLOR_PROMPT = "Give me a fitting color in hex code based on this prompt: ";
    private static final String START_CONTEST_PROMPT = "You're a femboy and you have a crazy game where femboys have to submit pictures related to some words. Write a quick exciting prompt to have them submit pictures, knowing the word of the round is ";
    private static final String END_CONTEST_PROMPT = "You're a femboy and you have a crazy game where femboys have to submit pictures related to some words. However, the game has come to an end and you'll be congratulating the winner! Write a quick exciting prompt to congratulate the winner or winners, knowing the contest's prompt was ";
    private static final String EMOJIS_PROMPT = "Give me ONLY up to THREE fitting emojis without any other text for Discord based on this prompt: ";

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
        long endingTimestamp = currentTimeInSeconds + serverData.getLengthOfContestInSeconds();
        String prompt = serverData.getContestPrompt();

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
        eb.setTitle("Contest Started " + getGeneratedEmojis(prompt));
        eb.setDescription("**Prompt**: " + prompt);
        eb.setColor(getGeneratedColor(prompt));
        eb.addField(
                "Submitting Your Seed?",
                "To submit your seed, simply upload your image/video to this channel! If you submit more than one seed, the highest-scoring seed will be used.",
                false
        );
        eb.addField(
                "Voting?",
                "To vote on a seed, react to the seed with any emoji! Only one vote per person per seed will be counted.",
                false
        );
        eb.addField(
                "Contest Ends In",
                "<t:" + endingTimestamp + ":R>",
                false
        );

        // Send messsage to the channel
        guild.getTextChannelById(serverData.getContestChannelId()).sendMessageEmbeds(
                eb.build()
        ).setContent(getGeneratedNewContestPrompt(prompt)).complete();

        serverData.setContestEndingTimestamp(
                endingTimestamp
        );
        serverData.setContestStartingTimestamp(currentTimeInSeconds);
        core.getServerDataHandler().updateServerData();
    }
    public static void endContest(@NonNull Guild guild, @NonNull SeedServerData serverData) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();
        Color color;

        if(scheduledFuture != null)
            scheduledFuture.cancel(false);

        // Try to get an appropriate color
        try {
            color = getGeneratedColor(serverData.getCurrentPrompt());
        } catch (Exception ignored) {
            color = Color.CYAN;
        }

        // Send a contest ending message
        eb.setTitle("Contest Ended!");
        eb.setColor(Color.GREEN);
        eb.setDescription("The contest has ended! The results will be in <#" + serverData.getLeaderboardChannelId() + "> shortly!");
        Message msg = guild.getTextChannelById(serverData.getContestChannelId()).sendMessageEmbeds(
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

        if(participants.size() > 0) {
            // Construct the MessageEmbed
            eb = new EmbedBuilder();
            eb.setTitle("Final Results");
            eb.setColor(color);
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
                if(score == 0)
                    break;
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

            // Send a congratulatory message to the people in 1st place
            if(contestResults.getSortedParticipants().size() > 0) {
                ArrayList<Long> firstPlaceUsers = new ArrayList<>();
                long firstPlaceScore = contestResults.contestParticipants().get(contestResults.getSortedParticipants().get(0));
                for(int i = 0; i < contestResults.getSortedParticipants().size(); i++) {
                    long userId = contestResults.getSortedParticipants().get(i);
                    if(firstPlaceScore == 0)
                        break;
                    if(contestResults.contestParticipants().get(userId) == firstPlaceScore) {
                        firstPlaceUsers.add(userId);
                    } else {
                        break;
                    }
                }

                eb = new EmbedBuilder();
                eb.setColor(color);
//            eb.setTitle("Congratulations on 1st Place! :tada:");
                if(firstPlaceUsers.size() > 1) {
                    // Create a string of all the users in first place and wrap their ID in <@>
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < firstPlaceUsers.size(); i++) {
                        sb.append("<@").append(firstPlaceUsers.get(i)).append(">");
                        if(i != firstPlaceUsers.size() - 1) {
                            sb.append(", ");
                        }
                    }

                    eb.setDescription(sb + " won the contest with " + contestResults.contestParticipants().get(firstPlaceUsers.get(0)) + " votes!");
                } else {
                    eb.setDescription("<@" + firstPlaceUsers.get(0) + "> won the contest with " + contestResults.contestParticipants().get(firstPlaceUsers.get(0)) + " votes!");
                }

                guild.getTextChannelById(serverData.getContestChannelId()).sendMessageEmbeds(
                        eb.build()
                ).setContent(getGeneratedEndContestPrompt(serverData.getCurrentPrompt())).complete();
            }
        }

        msg.delete().queue();
        serverData.addContestResults(contestResults);

        if(serverData.isContestRecurring()) {
            executorService.schedule(
                    () -> {
                        try {
                            startContest(guild, serverData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    },
                    60,
                    TimeUnit.SECONDS
            );
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

    public static Color getGeneratedColor(@NonNull String prompt) {
        ChatCompletionResult result = OpenAIHandler.getCompletion(COLOR_PROMPT + prompt);
        String completion = result.getChoices().get(0).getMessage().getContent();

        // Look for a hex color code
        Pattern pattern = Pattern.compile("#[0-9a-fA-F]{6}");
        Matcher matcher = pattern.matcher(completion);

        if(matcher.find()) {
            return Color.decode(matcher.group());
        } else {
            throw new RuntimeException("No color found in completion: \n\n" + completion);
        }
    }

    public static String getGeneratedEmojis(@NonNull String prompt) {
        ChatCompletionResult result = OpenAIHandler.getCompletion(EMOJIS_PROMPT + prompt);

        return result.getChoices().get(0).getMessage().getContent();
    }

    public static String getGeneratedNewContestPrompt(@NonNull String prompt) {
        ChatCompletionResult result = OpenAIHandler.getCompletion(START_CONTEST_PROMPT + prompt);

        return result.getChoices().get(0).getMessage().getContent().substring(1, result.getChoices().get(0).getMessage().getContent().length() - 1);
    }

    public static String getGeneratedEndContestPrompt(@NonNull String prompt) {
        ChatCompletionResult result = OpenAIHandler.getCompletion(END_CONTEST_PROMPT + prompt);

        return result.getChoices().get(0).getMessage().getContent().substring(1, result.getChoices().get(0).getMessage().getContent().length() - 1);
    }
}