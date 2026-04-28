package io.soramitsu.examplepoint.offline;

/**
 * Result container for applying an incoming offline payment.
 */
public final class OfflineReceiveResult {
    private final OfflinePaymentPayload payload;
    private final OfflineState state;

    public OfflineReceiveResult(OfflinePaymentPayload payload, OfflineState state) {
        this.payload = payload;
        this.state = state;
    }

    public OfflinePaymentPayload getPayload() {
        return payload;
    }

    public OfflineState getState() {
        return state;
    }
}
