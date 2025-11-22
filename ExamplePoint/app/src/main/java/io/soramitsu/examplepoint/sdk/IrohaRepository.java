package io.soramitsu.examplepoint.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
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
import io.soramitsu.examplepoint.sdk.backup.KeyBackupManager;
import io.soramitsu.examplepoint.sdk.identity.NexusDeviceReport;
import io.soramitsu.examplepoint.sdk.identity.NexusIdentityManifest;
import io.soramitsu.examplepoint.sdk.identity.NexusUaidFactory;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountReceiveState;
import io.soramitsu.examplepoint.sdk.model.AccountShareInfo;
import io.soramitsu.examplepoint.sdk.model.AccountTransaction;
import io.soramitsu.examplepoint.sdk.registration.AccountRegistrationRequest;
import org.hyperledger.iroha.android.IrohaKeyManager;
import org.hyperledger.iroha.android.address.AccountAddress;
import org.hyperledger.iroha.android.address.AccountAddress.AccountAddressException;
import org.hyperledger.iroha.android.crypto.Signer;
import org.hyperledger.iroha.android.model.InstructionBox;
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

    private final Context context;
    private final ToriiConfig toriiConfig;
    private final ToriiClient toriiClient;
    private final AccountPrefs accountPrefs;
    private final IrohaKeyManager keyManager;
    private final KeyBackupManager keyBackupManager;
    private final NoritoCodecAdapter codecAdapter;
    private final ExecutorService executor;

    public IrohaRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.toriiConfig = ToriiConfig.fromBuildConfig();
        this.toriiClient = new ToriiClient(toriiConfig);
        this.accountPrefs = new AccountPrefs(this.context);
        this.keyManager = IrohaKeyManager.withDefaultProviders();
        this.codecAdapter = new NoritoJavaCodecAdapter();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "IrohaRepository");
            thread.setDaemon(true);
            return thread;
        });
        this.keyBackupManager = new KeyBackupManager(this.context, this.keyManager, executor);
    }

    public boolean hasAccountProfile() {
        return accountPrefs.load().isPresent();
    }

    public AccountProfile getAccountProfile() {
        return accountPrefs.load().orElseThrow(() ->
                new IllegalStateException("Account profile is not initialised. Register an account first."));
    }

    public KeyBackupManager getKeyBackupManager() {
        return keyBackupManager;
    }

    public CompletableFuture<AccountProfile> registerAccount(AccountRegistrationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return registerAccountInternal(request);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    private AccountProfile registerAccountInternal(AccountRegistrationRequest request) throws Exception {
        if (!toriiConfig.hasAdminCredentials()) {
            throw new IllegalStateException("TORII_ADMIN_ACCOUNT_ID and TORII_ADMIN_PRIVATE_KEY must be configured");
        }
        final String adminAccountId = toriiConfig.adminAccountId();
        final byte[] adminPrivateKey = toriiConfig.adminPrivateKey();
        if (adminAccountId == null || adminPrivateKey == null) {
            throw new IllegalStateException("Admin credentials are invalid");
        }

        String keyAlias = request.getKeyAlias();
        if (keyAlias == null || keyAlias.trim().isEmpty()) {
            keyAlias = "acct-" + UUID.randomUUID();
        }
        final KeyPair keyPair = keyManager.generateOrLoad(
                keyAlias,
                IrohaKeyManager.KeySecurityPreference.HARDWARE_PREFERRED);

        final byte[] publicKey = KeyEncodingUtils.extractEd25519PublicKey(keyPair.getPublic());
        final NexusIdentityManifest identityManifest = buildIdentityManifest(request);
        final String uaid = NexusUaidFactory.derive(identityManifest);
        final AccountAddress accountAddress = AccountAddress.fromAccount(
                toriiConfig.domain(),
                publicKey,
                "ed25519");

        final String canonicalHex = accountAddress.canonicalHex();
        final String accountId = canonicalHex + "@" + toriiConfig.domain();

        final RegisterAccountInstruction.Builder instructionBuilder =
                RegisterAccountInstruction.builder()
                        .setAccountId(accountId)
                        .setUaid(uaid);
        if (request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()) {
            instructionBuilder.putMetadata("display_name", request.getDisplayName().trim());
        }
        instructionBuilder
                .putMetadata("legal_name", request.getLegalName())
                .putMetadata("document_type", request.getDocumentType())
                .putMetadata("document_number", request.getDocumentNumber())
                .putMetadata("residency_country", request.getResidencyCountry())
                .putMetadata("contact", request.getContact());
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
                toriiConfig.domain(),
                request.getDisplayName(),
                keyAlias,
                toriiConfig.defaultAssetId(),
                identityManifest);
        accountPrefs.save(profile);
        return profile;
    }

    public CompletableFuture<List<AccountAsset>> fetchAccountAssets() {
        return CompletableFuture.supplyAsync(() -> {
            AccountProfile profile = getAccountProfile();
            try {
                return toriiClient.fetchAccountAssets(profile.getAccountId());
            } catch (IOException | ToriiException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<AccountShareInfo> loadAccountShareInfo() {
        return CompletableFuture.supplyAsync(() -> buildAccountShareInfo(getAccountProfile()), executor);
    }

    public CompletableFuture<AccountReceiveState> fetchAccountReceiveState() {
        return CompletableFuture.supplyAsync(() -> {
            AccountProfile profile = getAccountProfile();
            AccountShareInfo shareInfo = buildAccountShareInfo(profile);
            try {
                List<AccountAsset> assets = toriiClient.fetchAccountAssets(profile.getAccountId());
                ToriiClient.ExplorerAccountQrSnapshot qrSnapshot =
                        toriiClient.fetchExplorerAccountQr(shareInfo.getAccountId(), "ih58");
                return new AccountReceiveState(shareInfo, assets, qrSnapshot);
            } catch (IOException | ToriiException | RuntimeException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> transferAsset(String receiverAccountId, String amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                transferInternal(receiverAccountId, amount);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    public CompletableFuture<List<AccountTransaction>> fetchAccountTransactions(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            AccountProfile profile = getAccountProfile();
            try {
                return toriiClient.fetchAccountTransactions(profile.getAccountId(), limit);
            } catch (IOException | ToriiException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public void clearAccountProfile() {
        accountPrefs.clear();
    }

    private AccountShareInfo buildAccountShareInfo(AccountProfile profile) {
        try {
            AccountAddress address = AccountAddress.fromCanonicalHex(profile.getAccountAddressHex());
            AccountAddress.DisplayFormats formats = address.displayFormats(toriiConfig.ih58Prefix());
            String accountId = formats.ih58 + "@" + profile.getDomain();
            String identityJson = profile.getIdentityManifest() != null
                    ? profile.getIdentityManifest().toCanonicalJson()
                    : null;
            return new AccountShareInfo(
                    profile.getDisplayName(),
                    accountId,
                    address.canonicalHex(),
                    formats.ih58,
                    formats.compressed,
                    formats.compressedWarning,
                    formats.networkPrefix,
                    profile.getDomain(),
                    profile.getPreferredAssetId(),
                    identityJson
            );
        } catch (AccountAddressException e) {
            throw new CompletionException(e);
        }
    }

    private NexusIdentityManifest buildIdentityManifest(AccountRegistrationRequest request) {
        boolean hasStrongBox = keyManager.hasStrongBoxProvider();
        NexusDeviceReport report = NexusDeviceReport.capture(hasStrongBox);
        return NexusIdentityManifest.builder()
                .setLegalName(request.getLegalName())
                .setDocumentType(request.getDocumentType())
                .setDocumentNumber(request.getDocumentNumber())
                .setResidencyCountry(request.getResidencyCountry())
                .setContact(request.getContact())
                .build(report, System.currentTimeMillis());
    }

    private void transferInternal(String receiverAccountId, String amount) throws Exception {
        if (receiverAccountId == null || !receiverAccountId.contains("@")) {
            throw new IllegalArgumentException("Receiver account must include domain, e.g., <address>@<domain>");
        }
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount must not be empty");
        }
        AccountProfile profile = getAccountProfile();
        final String assetId = toriiConfig.defaultAssetId();
        final InstructionBox instruction = InstructionBox.of(
                org.hyperledger.iroha.android.model.instructions.TransferAssetInstruction.builder()
                        .setAssetId(assetId)
                        .setQuantity(amount.trim())
                        .setDestinationAccountId(receiverAccountId.trim())
                        .build()
        );

        final TransactionPayload payload = TransactionPayload.builder()
                .setChainId(toriiConfig.chainId())
                .setAuthority(profile.getAccountId())
                .setCreationTimeMs(System.currentTimeMillis())
                .setTimeToLiveMs(60_000L)
                .setNonce(generateNonce())
                .setInstructions(Collections.singletonList(instruction))
                .build();

        final Signer signer = keyManager.signerForAlias(
                profile.getKeyAlias(),
                IrohaKeyManager.KeySecurityPreference.HARDWARE_PREFERRED);

        final TransactionBuilder builder = new TransactionBuilder(codecAdapter, keyManager);
        final SignedTransaction transaction = builder.encodeAndSign(payload, signer);
        toriiClient.submitTransaction(transaction);
    }

    private static int generateNonce() {
        long value = System.currentTimeMillis();
        return (int) (value & 0x7FFFFFFF);
    }
}
