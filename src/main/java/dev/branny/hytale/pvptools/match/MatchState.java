package dev.branny.hytale.pvptools.match;

/**
 * Represents the current state of a match.
 */
public enum MatchState {
    /**
     * Match is being set up, players are joining.
     */
    WAITING("Waiting"),

    /**
     * Match is starting (countdown phase).
     */
    STARTING("Starting"),

    /**
     * Match is actively in progress.
     */
    ACTIVE("Active"),

    /**
     * Match is ending (cleanup phase).
     */
    ENDING("Ending"),

    /**
     * Match has completed.
     */
    COMPLETED("Completed"),

    /**
     * Match was cancelled.
     */
    CANCELLED("Cancelled");

    private final String displayName;

    MatchState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this state represents an active match.
     */
    public boolean isActive() {
        return this == STARTING || this == ACTIVE;
    }

    /**
     * Checks if this state represents a finished match.
     */
    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Checks if players can join in this state.
     */
    public boolean canJoin() {
        return this == WAITING;
    }

    /**
     * Checks if players can leave without penalty in this state.
     */
    public boolean canLeaveFreely() {
        return this == WAITING || this == COMPLETED || this == CANCELLED;
    }
}
