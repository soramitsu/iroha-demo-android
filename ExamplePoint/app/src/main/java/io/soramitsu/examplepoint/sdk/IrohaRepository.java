package io.soramitsu.examplepoint.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.soramitsu.examplepoint.data.AccountPrefs;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.network.ToriiClient;
import io.soramitsu.examplepoint.network.ToriiException;
import io.soramitsu.examplepoint.util.CrashReporter;
import org.hyperledger.iroha.android.IrohaKeyManager;
import org.hyperledger.iroha.android.KeyManagementException;
import org.hyperledger.iroha.android.address.AccountAddress;
import org.hyperledger.iroha.android.crypto.Signer;
import org.hyperledger.iroha.android.model.instructions.InstructionBox;
import org.hyperledger.iroha.android.model.instructions.RegisterAccountInstruction;
import org.hyperledger.iroha.android.norito.NoritoJavaCodecAdapter;
import org.hyperledger.iroha.android.norito.NoritoCodecAdapter;
import org.hyperledger.iroha.android.tx.SignedTransaction;
import org.hyperledger.iroha.android.tx.TransactionBuilder;
import org.hyperledger.iroha.android.model.TransactionPayload;

/**
 * High-level entrypoint that coordinates key management, transaction building, and Torii RPC calls.
 */
public class IrohaRepository {

    private final ToriiConfig toriiConfig;
    private final ToriiClient toriiClient;
    private final AccountPrefs accountPrefs;
    private final IrohaKeyManager keyManager;
    private final NoritoCodecAdapter codecAdapter;
    private final ExecutorService executor;

    public IrohaRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.toriiConfig = ToriiConfig.fromBuildConfig();
        this.toriiClient = new ToriiClient(toriiConfig);
        this.accountPrefs = new AccountPrefs(appContext);
        this.keyManager = IrohaKeyManager.withDefaultProviders();
        this.codecAdapter = new NoritoJavaCodecAdapter();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "IrohaRepository");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<AccountProfile> registerAccount(String displayName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return registerAccountInternal(displayName);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    private AccountProfile registerAccountInternal(String displayName) throws Exception {
        if (!toriiConfig.hasAdminCredentials()) {
            throw new IllegalStateException("TORII_ADMIN_ACCOUNT_ID and TORII_ADMIN_PRIVATE_KEY must be configured");
        }
        final String adminAccountId = toriiConfig.adminAccountId();
        final byte[] adminPrivateKey = toriiConfig.adminPrivateKey();
        if (adminAccountId == null || adminPrivateKey == null) {
            throw new IllegalStateException("Admin credentials are invalid");
        }

        final String keyAlias = "acct-" + UUID.randomUUID();
        final KeyPair keyPair = keyManager.generateOrLoad(
                keyAlias,
                IrohaKeyManager.KeySecurityPreference.HARDWARE_PREFERRED);

        final byte[] publicKey = KeyEncodingUtils.extractEd25519PublicKey(keyPair.getPublic());
        final AccountAddress accountAddress = AccountAddress.fromAccount(
                toriiConfig.domain(),
                publicKey,
                "ed25519");

        final String canonicalHex = accountAddress.canonicalHex();
        final String accountId = canonicalHex + "@" + toriiConfig.domain();

        final RegisterAccountInstruction.Builder instructionBuilder =
                RegisterAccountInstruction.builder().setAccountId(accountId);
        if (displayName != null && !displayName.trim().isEmpty()) {
            instructionBuilder.putMetadata("display_name", displayName.trim());
        }
        final InstructionBox registerInstruction = InstructionBox.of(instructionBuilder.build());

        final TransactionPayload payload = TransactionPayload.builder()
                .setChainId(toriiConfig.chainId())
                .setAuthority(adminAccountId)
                .setCreationTimeMs(System.currentTimeMillis())
                .setTimeToLiveMs(60_000L)
                .setNonce(generateNonce())
                .setInstructions(Collections.singletonList(registerInstruction))
                .build();

        final TransactionBuilder builder = new TransactionBuilder(codecAdapter, keyManager);
        final Signer adminSigner = Signers.ed25519(adminPrivateKey);

        final SignedTransaction signedTransaction = builder.encodeAndSign(payload, adminSigner);
        toriiClient.submitTransaction(signedTransaction);

        final AccountProfile profile = new AccountProfile(
                canonicalHex,
                displayName,
                keyAlias,
                toriiConfig.defaultAssetId());
        accountPrefs.save(profile);
        return profile;
    }

    public AccountProfile requireAccountProfile() {
        return accountPrefs.load().orElseThrow(() ->
                new IllegalStateException("Account profile is not initialised. Register an account first."));
    }

    public void clearAccountProfile() {
        accountPrefs.clear();
    }

    private static int generateNonce() {
        long value = System.currentTimeMillis();
        return (int) (value & 0x7FFFFFFF);
    }
}
