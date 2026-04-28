package io.soramitsu.examplepoint.connect;

import androidx.annotation.NonNull;

import java.util.Objects;

import org.hyperledger.iroha.android.crypto.Signer;

/** Active signer identity used by wallet-role Iroha Connect sessions. */
public final class ConnectSigningIdentity {

    private final String accountId;
    private final String displayName;
    private final Signer signer;

    public ConnectSigningIdentity(@NonNull String accountId,
                                  @NonNull String displayName,
                                  @NonNull Signer signer) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.signer = Objects.requireNonNull(signer, "signer");
    }

    @NonNull
    public String accountId() {
        return accountId;
    }

    @NonNull
    public String displayName() {
        return displayName;
    }

    @NonNull
    public Signer signer() {
        return signer;
    }
}
