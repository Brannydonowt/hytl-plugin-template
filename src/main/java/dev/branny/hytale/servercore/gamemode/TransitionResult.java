package dev.branny.hytale.servercore.gamemode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Result of a gamemode transition operation.
 * Contains success status, message, and transition details.
 */
public record TransitionResult(
    boolean success,
    @Nonnull String message,
    @Nullable String fromGamemode,
    @Nullable String toGamemode
) {
    
    /**
     * Creates a successful transition result.
     */
    @Nonnull
    public static TransitionResult success(
            @Nullable String fromGamemode, 
            @Nullable String toGamemode) {
        String msg = toGamemode != null 
            ? "Successfully joined " + toGamemode
            : "Successfully returned to lobby";
        return new TransitionResult(true, msg, fromGamemode, toGamemode);
    }
    
    /**
     * Creates a successful transition result with custom message.
     */
    @Nonnull
    public static TransitionResult success(
            @Nonnull String message,
            @Nullable String fromGamemode, 
            @Nullable String toGamemode) {
        return new TransitionResult(true, message, fromGamemode, toGamemode);
    }
    
    /**
     * Creates a failed transition result.
     */
    @Nonnull
    public static TransitionResult failure(@Nonnull String reason) {
        return new TransitionResult(false, reason, null, null);
    }
    
    /**
     * Creates a failed transition result with context.
     */
    @Nonnull
    public static TransitionResult failure(
            @Nonnull String reason,
            @Nullable String fromGamemode,
            @Nullable String toGamemode) {
        return new TransitionResult(false, reason, fromGamemode, toGamemode);
    }
}
