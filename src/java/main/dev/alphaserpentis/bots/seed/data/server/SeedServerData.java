package dev.alphaserpentis.bots.seed.data.server;

import dev.alphaserpentis.bots.seed.data.contest.SeedContestResults;
import dev.alphaserpentis.coffeecore.data.server.ServerData;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;

public class SeedServerData extends ServerData {
    private ArrayList<SeedContestResults> contestResults = new ArrayList<>();
    private ArrayList<Long> permissionedUsers = new ArrayList<>();
    private ArrayList<Long> permissionedRoles = new ArrayList<>();
    private ArrayList<String> prompts = new ArrayList<>();
    private long leaderboardChannelId = 0;
    private long contestChannelId = 0;
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
    public long getLeaderboardChannelId() {
        return leaderboardChannelId;
    }
    public long getContestChannelId() {
        return contestChannelId;
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
    public void setContestEndingTimestamp(long contestEndingTimestamp) {
        this.contestEndingTimestamp = contestEndingTimestamp;
    }
    public void setLengthOfContestInSeconds(long lengthOfContestInSeconds) {
        this.lengthOfContestInSeconds = lengthOfContestInSeconds;
    }
    public void setContestRecurring(boolean contestRecurring) {
        isContestRecurring = contestRecurring;
    }

    public boolean isMemberPermissioned(@NonNull Member member) {
        if(getPermissionedUsers().contains(member.getIdLong())) {
            return true;
        } else if(member.getRoles().stream().anyMatch(role -> getPermissionedRoles().contains(role.getIdLong()))) {
            return true;
        } else return member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR);
    }

    public boolean isContestRunning() {
        return getContestEndingTimestamp() > System.currentTimeMillis() / 1000;
    }

    /**
     * Gets a random prompt from the list of prompts. It will not return the same prompt twice in a row.
     * @return A random prompt.
     */
    public String getContestPrompt() {
        if(getPrompts().size() == 0) {
            throw new RuntimeException("No prompts have been added to the list of prompts.");
        } else if(getPrompts().size() == 1) {
            throw new RuntimeException("Only one prompt has been added to the list of prompts. Please add more prompts.");
        } else {
            String prompt = getPrompts().get((int) (Math.random() * getPrompts().size()));
            while(prompt.equals(getContestPrompt())) {
                prompt = getPrompts().get((int) (Math.random() * getPrompts().size()));
            }
            return prompt;
        }
    }
}
