package dev.alphaserpentis.bots.seed.data.server;

import dev.alphaserpentis.bots.seed.data.contest.SeedContestResults;
import dev.alphaserpentis.bots.seed.handler.OpenAIHandler;
import dev.alphaserpentis.coffeecore.data.server.ServerData;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class SeedServerData extends ServerData {
    private ArrayList<SeedContestResults> contestResults = new ArrayList<>();
    private ArrayList<Long> permissionedUsers = new ArrayList<>();
    private ArrayList<Long> permissionedRoles = new ArrayList<>();
    private ArrayList<String> prompts = new ArrayList<>();
    private ArrayList<String> previouslyUsedPrompts = new ArrayList<>();
    private String promptsSource = "";
    private long leaderboardChannelId = 0;
    private long contestChannelId = 0;
    private long lastLeaderboardMessageId = 0;
    private long contestStartingTimestamp = 0;
    private long contestEndingTimestamp = 0;
    private long lengthOfContestInSeconds = 604800;
    private boolean isContestRecurring = false;

    public SeedServerData() {
        super();
    }

    @NonNull
    public ArrayList<SeedContestResults> getContestResults() {
        return contestResults;
    }
    @NonNull
    public ArrayList<Long> getPermissionedUsers() {
        return permissionedUsers;
    }
    @NonNull
    public ArrayList<Long> getPermissionedRoles() {
        return permissionedRoles;
    }
    @NonNull
    public ArrayList<String> getPrompts() {
        return prompts;
    }
    @NonNull
    public String getPromptsSource() {
        return promptsSource;
    }
    public long getLeaderboardChannelId() {
        return leaderboardChannelId;
    }
    public long getContestChannelId() {
        return contestChannelId;
    }
    public long getLastLeaderboardMessageId() {
        return lastLeaderboardMessageId;
    }
    public long getContestStartingTimestamp() {
        return contestStartingTimestamp;
    }
    public long getContestEndingTimestamp() {
        return contestEndingTimestamp;
    }
    public long getLengthOfContestInSeconds() {
        return lengthOfContestInSeconds;
    }
    public boolean isContestRecurring() {
        return isContestRecurring;
    }
    public void setLeaderboardChannelId(long leaderboardChannelId) {
        this.leaderboardChannelId = leaderboardChannelId;
    }
    public void setContestChannelId(long contestChannelId) {
        this.contestChannelId = contestChannelId;
    }
    public void setLastLeaderboardMessageId(long lastLeaderboardMessageId) {
        this.lastLeaderboardMessageId = lastLeaderboardMessageId;
    }
    public void setContestStartingTimestamp(long contestStartingTimestamp) {
        this.contestStartingTimestamp = contestStartingTimestamp;
    }
    public void setContestEndingTimestamp(long contestEndingTimestamp) {
        this.contestEndingTimestamp = contestEndingTimestamp;
    }
    public void setLengthOfContestInSeconds(long lengthOfContestInSeconds) {
        this.lengthOfContestInSeconds = lengthOfContestInSeconds;
    }
    public void setContestRecurring(boolean contestRecurring) {
        isContestRecurring = contestRecurring;
    }

    public void addContestResults(@NonNull SeedContestResults contestResults) {
        this.contestResults.add(contestResults);
    }
    public void setPromptsSource(@NonNull String promptsSource) {
        this.promptsSource = promptsSource;
    }
    public void addPromptToPreviouslyUsedPrompts(@NonNull String prompt) {
        previouslyUsedPrompts.add(0, prompt);
        if(previouslyUsedPrompts.size() > 6) {
            previouslyUsedPrompts.remove(6);
        }
//        previouslyUsedPrompts[0] = previouslyUsedPrompts[1];
//        previouslyUsedPrompts[1] = previouslyUsedPrompts[2];
//        previouslyUsedPrompts[2] = prompt;
    }
    public void addPermissionedUser(long userId) {
        permissionedUsers.add(userId);
    }
    public void removePermissionedUser(long userId) {
        permissionedUsers.remove(userId);
    }
    public void addPermissionedRole(long roleId) {
        permissionedRoles.add(roleId);
    }
    public void removePermissionedRole(long roleId) {
        permissionedRoles.remove(roleId);
    }
    public void addPrompt(@NonNull String prompt) {
        prompts.add(prompt);
    }
    public void removePrompt(@NonNull String prompt) {
        prompts.remove(prompt);
    }

    public boolean isMemberPermissioned(@NonNull Member member) {
        if(getPermissionedUsers().contains(member.getIdLong())) {
            return true;
        } else if(member.getRoles().stream().anyMatch(role -> getPermissionedRoles().contains(role.getIdLong()))) {
            return true;
        } else return member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR);
    }

    public boolean isContestRunning() {
        return getContestEndingTimestamp() > System.currentTimeMillis() / 1000 || getContestEndingTimestamp() != 0;
    }

    /**
     * Pulls a comma-separated list of prompts from a URL and updates the prompts.
     * @param url The URL to pull the prompts from. The data returned must be a comma-separated list of prompts.
     * @return True if the prompts were successfully updated, false otherwise.
     */
    public boolean pullAndUpdatePrompts(@NonNull String url) {
        // Read a comma-separated list of prompts from a URL.
        ArrayList<String> prompts = new ArrayList<>();
        try {
            URL urlObject = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(urlObject.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] words = inputLine.split("\\s*,\\s*");
                prompts.addAll(Arrays.asList(words));
            }
        } catch (IOException e) {
            return false;
        }

        // Update the prompts.
        setPromptsSource(url);
        this.prompts = prompts;
        return true;
    }

    /**
     * Gets a random prompt from the list of prompts. It will not return the same prompt twice in a row.
     * @return A random prompt.
     */
    public String getContestPrompt() {
        if(getPrompts().size() == 0) {
            throw new RuntimeException("No prompts have been added to the list of prompts.");
        } else if(getPrompts().size() < 6) {
            throw new RuntimeException("Only " + getPrompts().size() + " prompt(s) have been added to the list of prompts. Please add at least six prompts.");
        } else {
            ArrayList<String> availablePrompts = new ArrayList<>(getPrompts());
            String word1, word2;

            prompts.removeIf(prompt -> previouslyUsedPrompts.contains(prompt));
            word1 = availablePrompts.get((int) Math.floor(Math.random() * availablePrompts.size()));

            if(word1.contains(" ")) { // If the prompt is more than one word, use it as-is.
                if(OpenAIHandler.isPromptSafeToUse(word1)) {
                    addPromptToPreviouslyUsedPrompts(word1);
                    return word1;
                } else {
                    System.err.println("[SeedServerData] According to OpenAI, the prompt \"" + word1 + "\" is not safe to use. Generating a new prompt...");
                    return getContestPrompt();
                }
            } else {
                availablePrompts.remove(word1);
                word2 = availablePrompts.get((int) Math.floor(Math.random() * availablePrompts.size()));

                // Verify that the prompt is 'safe'
                if(OpenAIHandler.isPromptSafeToUse(word1 + " " + word2)) {
                    addPromptToPreviouslyUsedPrompts(word2);
                    addPromptToPreviouslyUsedPrompts(word1);
                    return word1 + " " + word2;
                } else {
                    System.err.println("[SeedServerData] According to OpenAI, the prompt \"" + word1 + " " + word2 + "\" is not safe to use. Generating a new prompt...");
                    return getContestPrompt();
                }
            }
        }
    }

    public String getCurrentPrompt() {
        return previouslyUsedPrompts.get(0) + " " + previouslyUsedPrompts.get(1);
    }
}
