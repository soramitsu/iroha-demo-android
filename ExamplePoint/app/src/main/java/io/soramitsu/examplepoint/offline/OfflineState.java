package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * In-memory view of offline balance, counters, and anti-replay log.
 */
public final class OfflineState {

    private final BigDecimal balance;
    private final long nextCounter;
    private final Set<String> replayLog;
    private final List<OfflineTransferRecord> history;

    public OfflineState(
            @NonNull BigDecimal balance,
            long nextCounter,
            @NonNull Set<String> replayLog,
            @NonNull List<OfflineTransferRecord> history
    ) {
        this.balance = Objects.requireNonNull(balance, "balance");
        this.nextCounter = nextCounter;
        this.replayLog = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(replayLog, "replayLog")));
        this.history = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(history, "history")));
    }

    public static OfflineState empty() {
        return new OfflineState(BigDecimal.ZERO, 0L, Collections.emptySet(), Collections.emptyList());
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getNextCounter() {
        return nextCounter;
    }

    public Set<String> getReplayLog() {
        return replayLog;
    }

    public List<OfflineTransferRecord> getHistory() {
        return history;
    }

    public boolean hasSeen(String txId) {
        return replayLog.contains(txId);
    }

    public OfflineState markSeen(String txId) {
        Set<String> updated = new LinkedHashSet<>(replayLog);
        updated.add(txId);
        return new OfflineState(balance, nextCounter, updated, history);
    }

    public OfflineState withUpdatedBalance(BigDecimal newBalance) {
        return new OfflineState(newBalance, nextCounter, replayLog, history);
    }

    public OfflineState nextCounterState() {
        return new OfflineState(balance, nextCounter + 1, replayLog, history);
    }

    public OfflineState appendHistory(OfflineTransferRecord record) {
        List<OfflineTransferRecord> updated = new ArrayList<>(history);
        updated.add(record);
        return new OfflineState(balance, nextCounter, replayLog, updated);
    }

    public OfflineState credit(BigDecimal amount, OfflineTransferRecord record) {
        return new OfflineState(
                balance.add(amount),
                nextCounter,
                replayLog,
                appendHistory(record).history);
    }

    public OfflineState debit(BigDecimal amount, OfflineTransferRecord record) {
        return new OfflineState(
                balance.subtract(amount),
                nextCounter,
                replayLog,
                appendHistory(record).history);
    }
}
