package dev.branny.hytale.pvptools.match;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Represents the result of a completed match.
 */
public class MatchResult {

    private final UUID matchId;
    private final String gamemodeId;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final MatchState finalState;
    
    // Winners and participants
    @Nullable
    private final MatchParticipant winner; // For 1v1 or FFA
    @Nullable
    private final String winningTeamId;    // For team modes
    private final List<MatchParticipant> participants;
    
    // Stats
    private final Map<UUID, MatchParticipantStats> playerStats;

    private MatchResult(Builder builder) {
        this.matchId = builder.matchId;
        this.gamemodeId = builder.gamemodeId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = Duration.between(startTime, endTime);
        this.finalState = builder.finalState;
        this.winner = builder.winner;
        this.winningTeamId = builder.winningTeamId;
        this.participants = Collections.unmodifiableList(new ArrayList<>(builder.participants));
        this.playerStats = Collections.unmodifiableMap(new HashMap<>(builder.playerStats));
    }

    // ==================== Getters ====================

    @Nonnull
    public UUID getMatchId() {
        return matchId;
    }

    @Nonnull
    public String getGamemodeId() {
        return gamemodeId;
    }

    @Nonnull
    public Instant getStartTime() {
        return startTime;
    }

    @Nonnull
    public Instant getEndTime() {
        return endTime;
    }

    @Nonnull
    public Duration getDuration() {
        return duration;
    }

    @Nonnull
    public MatchState getFinalState() {
        return finalState;
    }

    @Nullable
    public MatchParticipant getWinner() {
        return winner;
    }

    @Nullable
    public String getWinningTeamId() {
        return winningTeamId;
    }

    @Nonnull
    public List<MatchParticipant> getParticipants() {
        return participants;
    }

    @Nonnull
    public Map<UUID, MatchParticipantStats> getPlayerStats() {
        return playerStats;
    }

    // ==================== Utility ====================

    /**
     * Checks if the match was cancelled.
     */
    public boolean wasCancelled() {
        return finalState == MatchState.CANCELLED;
    }

    /**
     * Checks if the match completed normally.
     */
    public boolean wasCompleted() {
        return finalState == MatchState.COMPLETED;
    }

    /**
     * Checks if there was a winner.
     */
    public boolean hasWinner() {
        return winner != null || winningTeamId != null;
    }

    /**
     * Gets the number of participants.
     */
    public int getParticipantCount() {
        return participants.size();
    }

    /**
     * Gets stats for a specific player.
     *
     * @param playerId the player's UUID
     * @return the stats, or null if not found
     */
    @Nullable
    public MatchParticipantStats getStatsFor(@Nonnull UUID playerId) {
        return playerStats.get(playerId);
    }

    /**
     * Gets the participant with the highest score.
     */
    @Nullable
    public MatchParticipant getTopScorer() {
        return participants.stream()
            .max(Comparator.comparingInt(MatchParticipant::getScore))
            .orElse(null);
    }

    /**
     * Gets the participant with the most kills.
     */
    @Nullable
    public MatchParticipant getTopKiller() {
        return participants.stream()
            .max(Comparator.comparingInt(MatchParticipant::getKills))
            .orElse(null);
    }

    @Override
    public String toString() {
        String winnerStr = winner != null ? winner.getUsername() : 
                          (winningTeamId != null ? "Team " + winningTeamId : "None");
        return "MatchResult{" +
                "matchId=" + matchId +
                ", gamemode=" + gamemodeId +
                ", duration=" + duration.toSeconds() + "s" +
                ", winner=" + winnerStr +
                ", participants=" + participants.size() +
                '}';
    }

    // ==================== Builder ====================

    @Nonnull
    public static Builder builder(@Nonnull UUID matchId, @Nonnull String gamemodeId) {
        return new Builder(matchId, gamemodeId);
    }

    public static class Builder {
        private final UUID matchId;
        private final String gamemodeId;
        private Instant startTime = Instant.now();
        private Instant endTime = Instant.now();
        private MatchState finalState = MatchState.COMPLETED;
        private MatchParticipant winner;
        private String winningTeamId;
        private final List<MatchParticipant> participants = new ArrayList<>();
        private final Map<UUID, MatchParticipantStats> playerStats = new HashMap<>();

        private Builder(UUID matchId, String gamemodeId) {
            this.matchId = matchId;
            this.gamemodeId = gamemodeId;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder finalState(MatchState state) {
            this.finalState = state;
            return this;
        }

        public Builder winner(MatchParticipant winner) {
            this.winner = winner;
            return this;
        }

        public Builder winningTeam(String teamId) {
            this.winningTeamId = teamId;
            return this;
        }

        public Builder addParticipant(MatchParticipant participant) {
            this.participants.add(participant);
            this.playerStats.put(participant.getPlayerId(), 
                new MatchParticipantStats(
                    participant.getKills(),
                    participant.getDeaths(),
                    participant.getScore(),
                    participant.isEliminated()
                ));
            return this;
        }

        public Builder addParticipants(Collection<MatchParticipant> participants) {
            participants.forEach(this::addParticipant);
            return this;
        }

        public MatchResult build() {
            return new MatchResult(this);
        }
    }

    /**
     * Immutable stats record for a participant.
     */
    public record MatchParticipantStats(int kills, int deaths, int score, boolean wasEliminated) {
        public double getKDRatio() {
            return deaths > 0 ? (double) kills / deaths : kills;
        }
    }
}
