package dev.alphaserpentis.bots.seed.data.contest;

import io.reactivex.rxjava3.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A record that stores the results of the contest
 * @param participants A map of the participants and their vote count
 * @param contestPrompt The prompt for the contest
 * @param contestStartedTimestamp The timestamp when the contest started
 * @param contestEndedTimestamp The timestamp when the contest ended
 * @param contestNumber The ID of the contest
 */
public record SeedContestResults(
        @NonNull Map<Long, Integer> participants,
        @NonNull String contestPrompt,
        long contestStartedTimestamp,
        long contestEndedTimestamp,
        int contestNumber
) {

    /**
     * Gets a list of participants with the highest vote count
     * @return An {@link ArrayList} of the participants sorted by vote count
     */
    @NonNull
    public ArrayList<Long> getSortedWinners() {
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
