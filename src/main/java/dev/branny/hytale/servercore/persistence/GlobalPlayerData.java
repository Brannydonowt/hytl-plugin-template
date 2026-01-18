package dev.branny.hytale.servercore.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Global server-wide player data that persists across all gamemodes.
 * Contains currency, play time, and other server-level statistics.
 */
public class GlobalPlayerData {

    public static final BuilderCodec<GlobalPlayerData> CODEC = BuilderCodec.builder(
            GlobalPlayerData.class,
            GlobalPlayerData::new
        )
        .addField(new KeyedCodec<>("ServerCurrency", Codec.INTEGER),
            (data, value) -> data.serverCurrency = value,
            data -> data.serverCurrency)
        .addField(new KeyedCodec<>("TotalPlayTimeSeconds", Codec.LONG),
            (data, value) -> data.totalPlayTimeSeconds = value,
            data -> data.totalPlayTimeSeconds)
        .addField(new KeyedCodec<>("FirstJoinTimestamp", Codec.LONG),
            (data, value) -> data.firstJoinTimestamp = value,
            data -> data.firstJoinTimestamp)
        .addField(new KeyedCodec<>("LastSeenTimestamp", Codec.LONG),
            (data, value) -> data.lastSeenTimestamp = value,
            data -> data.lastSeenTimestamp)
        .build();

    private int serverCurrency;
    private long totalPlayTimeSeconds;
    private long firstJoinTimestamp;
    private long lastSeenTimestamp;

    public GlobalPlayerData() {
        this.serverCurrency = 0;
        this.totalPlayTimeSeconds = 0;
        this.firstJoinTimestamp = 0;
        this.lastSeenTimestamp = 0;
    }

    // ==================== Currency ====================

    public int getServerCurrency() {
        return serverCurrency;
    }

    public void setServerCurrency(int serverCurrency) {
        this.serverCurrency = serverCurrency;
    }

    public void addCurrency(int amount) {
        this.serverCurrency += amount;
    }

    public boolean spendCurrency(int amount) {
        if (serverCurrency >= amount) {
            serverCurrency -= amount;
            return true;
        }
        return false;
    }

    // ==================== Play Time ====================

    public long getTotalPlayTimeSeconds() {
        return totalPlayTimeSeconds;
    }

    public void setTotalPlayTimeSeconds(long totalPlayTimeSeconds) {
        this.totalPlayTimeSeconds = totalPlayTimeSeconds;
    }

    public void addPlayTime(long seconds) {
        this.totalPlayTimeSeconds += seconds;
    }

    // ==================== Timestamps ====================

    public long getFirstJoinTimestamp() {
        return firstJoinTimestamp;
    }

    public void setFirstJoinTimestamp(long firstJoinTimestamp) {
        this.firstJoinTimestamp = firstJoinTimestamp;
    }

    /**
     * Records the first join if not already set.
     */
    public void recordFirstJoinIfNeeded() {
        if (firstJoinTimestamp == 0) {
            firstJoinTimestamp = System.currentTimeMillis();
        }
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public void setLastSeenTimestamp(long lastSeenTimestamp) {
        this.lastSeenTimestamp = lastSeenTimestamp;
    }

    /**
     * Updates the last seen timestamp to now.
     */
    public void updateLastSeen() {
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    @Override
    @Nonnull
    public String toString() {
        return "GlobalPlayerData{" +
                "currency=" + serverCurrency +
                ", playTime=" + totalPlayTimeSeconds + "s" +
                ", firstJoin=" + firstJoinTimestamp +
                ", lastSeen=" + lastSeenTimestamp +
                '}';
    }
}
