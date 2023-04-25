package dev.alphaserpentis.bots.seed.data.contest;

import io.reactivex.rxjava3.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A record that stores the results of the contest
 * @param contestParticipants A map of the participants and their vote count
 * @param contestPrompt The prompt for the contest
 * @param contestStartedTimestamp The timestamp when the contest started
 * @param contestEndedTimestamp The timestamp when the contest ended
 * @param contestNumber The ID of the contest
 */
public record SeedContestResults(
        @NonNull Map<Long, Integer> contestParticipants,
        @NonNull String contestPrompt,
        @NonNull String urlToWinningSeed,
        long contestStartedTimestamp,
        long contestEndedTimestamp,
        int contestNumber
) {

    /**
     * Gets a list of participants with the highest vote count
     * @return An {@link ArrayList} of the participants sorted by vote count
     */
    @NonNull
    public ArrayList<Long> getSortedParticipants() {
        ArrayList<Long> participants = new ArrayList<>(contestParticipants.keySet());

        participants.sort((participant1, participant2) -> {
            int participant1VoteCount = contestParticipants.get(participant1);
            int participant2VoteCount = contestParticipants.get(participant2);

            return Integer.compare(participant2VoteCount, participant1VoteCount);
        });

//        participants.forEach(participant -> System.out.println(participant + " " + contestParticipants.get(participant)));

        return participants;
    }

    public static HashMap<Long, Integer> getParticipantsTotalVotes(@NonNull ArrayList<SeedContestResults> contestResults) {
        HashMap<Long, Integer> participantsTotalVotes = new HashMap<>();

        contestResults.forEach(contestResult -> contestResult.getSortedParticipants().forEach(participant -> {
            if (participantsTotalVotes.containsKey(participant)) {
                int currentVoteCount = participantsTotalVotes.get(participant);
                int newVoteCount = currentVoteCount + contestResult.contestParticipants.get(participant);

                participantsTotalVotes.put(participant, newVoteCount);
            } else {
                participantsTotalVotes.put(participant, contestResult.contestParticipants.get(participant));
            }
        }));

        return participantsTotalVotes;
    }

    public static ArrayList<Long> getSortedParticipants(@NonNull HashMap<Long, Integer> participantsTotalVotes) {
        ArrayList<Long> participants = new ArrayList<>(participantsTotalVotes.keySet());

        participants.sort((participant1, participant2) -> {
            int participant1VoteCount = participantsTotalVotes.get(participant1);
            int participant2VoteCount = participantsTotalVotes.get(participant2);

            return Integer.compare(participant2VoteCount, participant1VoteCount);
        });

        return participants;
    }
}
