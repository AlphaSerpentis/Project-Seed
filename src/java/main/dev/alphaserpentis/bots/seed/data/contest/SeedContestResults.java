package dev.alphaserpentis.bots.seed.data.contest;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A record that stores the results of the contest
 * @param participants A map of the participants and their vote count
 * @param contestEndedTimestamp The timestamp when the contest ended
 */
public record SeedContestResults(
        HashMap<Long, Integer> participants,
        String contestPrompt,
        long contestEndedTimestamp,
        long leaderboardMessageId,
        int contestNumber
) {
    /**
     * Gets the participants with the highest vote count
     * @return An {@link ArrayList} of the participants sorted by vote count
     */
    public ArrayList<Long> getParticipants() {
        ArrayList<Long> winners = new ArrayList<>();
        int highestVoteCount = 0;

        for (HashMap.Entry<Long, Integer> entry : participants.entrySet()) {
            if (entry.getValue() > highestVoteCount) {
                winners.clear();
                winners.add(entry.getKey());
                highestVoteCount = entry.getValue();
            } else if (entry.getValue() == highestVoteCount) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }
}
