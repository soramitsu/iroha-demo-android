package io.soramitsu.examplepoint.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.AccountPrefs;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.network.ToriiClient;
import io.soramitsu.examplepoint.network.ToriiException;
import io.soramitsu.examplepoint.subscription.SubscriptionBackendRepository;
import io.soramitsu.examplepoint.subscription.SubscriptionCreateInput;
import io.soramitsu.examplepoint.subscription.SubscriptionRecord;
import io.soramitsu.examplepoint.subscription.SubscriptionUiMetadataStore;
import io.soramitsu.examplepoint.subscription.SubscriptionUsageInput;
import io.soramitsu.examplepoint.connect.ConnectSigningIdentity;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupManager;
import io.soramitsu.examplepoint.sdk.identity.NexusDeviceReport;
import io.soramitsu.examplepoint.sdk.identity.NexusIdentityManifest;
import io.soramitsu.examplepoint.sdk.identity.NexusUaidFactory;
import io.soramitsu.examplepoint.sdk.key.ExposedPrivateKeyEncoder;
import io.soramitsu.examplepoint.sdk.key.ExportableKeyManagerFactory;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountReceiveState;
import io.soramitsu.examplepoint.sdk.model.AccountShareInfo;
import io.soramitsu.examplepoint.sdk.model.AccountTransaction;
import io.soramitsu.examplepoint.sdk.registration.AccountRegistrationRequest;
import org.hyperledger.iroha.android.IrohaKeyManager;
import org.hyperledger.iroha.android.client.ClientConfig;
import org.hyperledger.iroha.android.client.HttpClientTransport;
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
    private final SubscriptionBackendRepository subscriptionRepository;
    private final ExecutorService executor;

    public IrohaRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.toriiConfig = ToriiConfig.fromBuildConfig();
        this.toriiClient = new ToriiClient(toriiConfig);
        this.accountPrefs = new AccountPrefs(this.context);
        this.keyManager = ExportableKeyManagerFactory.create(this.context);
        this.codecAdapter = new NoritoJavaCodecAdapter();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "IrohaRepository");
            thread.setDaemon(true);
            return thread;
        });
        ClientConfig clientConfig = ClientConfig.builder()
                .setBaseUri(URI.create(toriiConfig.baseUrl().toString()))
                .setRequestTimeout(Duration.ofSeconds(30))
                .build();
        HttpClientTransport transport = HttpClientTransport.withDefaultExecutor(clientConfig);
        this.subscriptionRepository = new SubscriptionBackendRepository(
                transport.subscriptionToriiClient(),
                toriiConfig.defaultAssetId(),
                toriiConfig.domain(),
                new SubscriptionUiMetadataStore(this.context)
        );
        this.keyBackupManager = new KeyBackupManager(this.context, this.keyManager, executor);
    }

    public boolean hasAccountProfile() {
        return accountPrefs.load().isPresent();
    }

    public List<AccountProfile> getAccountProfiles() {
        return accountPrefs.loadAll();
    }

    public AccountProfile getAccountProfile() {
        return accountPrefs.load().orElseThrow(() ->
                new IllegalStateException("Account profile is not initialised. Register an account first."));
    }

    @NonNull
    public ConnectSigningIdentity loadActiveConnectSigningIdentity() {
        AccountProfile profile = getAccountProfile();
        try {
            KeyPair keyPair = keyManager.generateOrLoad(
                    profile.getKeyAlias(),
                    IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);
            byte[] privateKey = ExposedPrivateKeyEncoder.extractEd25519PrivateKey(keyPair.getPrivate());
            Signer signer = Signers.ed25519(privateKey);
            return new ConnectSigningIdentity(
                    profile.getAccountId(),
                    profile.getDisplayName(),
                    signer);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load active connect signing identity", ex);
        }
    }

    public boolean setActiveAccount(String accountId) {
        return accountPrefs.setActiveAccount(accountId);
    }

    public boolean removeAccount(String accountId) {
        return accountPrefs.removeAccount(accountId);
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
                IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);

        final byte[] publicKey = KeyEncodingUtils.extractEd25519PublicKey(keyPair.getPublic());
        final NexusIdentityManifest identityManifest = buildIdentityManifest(request);
        final String uaid = NexusUaidFactory.derive(identityManifest);
        final String accountId = AccountIdCodec.encodeDomainlessAccount(
                publicKey,
                "ed25519",
                toriiConfig.i105Discriminant());

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
                accountId,
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
                return new AccountReceiveState(shareInfo, assets, shareInfo.getAccountId());
            } catch (IOException | ToriiException | RuntimeException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<String> transferAsset(String receiverAccountLiteral, String amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String receiverAccountId = resolveAccountTargetInternal(receiverAccountLiteral);
                transferInternal(receiverAccountId, amount);
                return receiverAccountId;
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    public CompletableFuture<String> resolveAccountTarget(String rawLiteral) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resolveAccountTargetInternal(rawLiteral);
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

    public CompletableFuture<List<SubscriptionRecord>> fetchSubscriptions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AccountProfile profile = getAccountProfile();
                return subscriptionRepository.fetchSubscriptions(profile.getAccountId());
            } catch (RuntimeException ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    public CompletableFuture<SubscriptionRecord> createSubscription(@NonNull SubscriptionCreateInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AccountProfile profile = getAccountProfile();
                String privateKey = deriveExposedPrivateKey(profile);
                return subscriptionRepository.createSubscription(profile.getAccountId(), privateKey, input);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    public CompletableFuture<Void> pauseSubscription(@NonNull String subscriptionId) {
        return CompletableFuture.runAsync(() -> executeSubscriptionAction(subscriptionId, Action.PAUSE), executor);
    }

    public CompletableFuture<Void> resumeSubscription(@NonNull String subscriptionId) {
        return CompletableFuture.runAsync(() -> executeSubscriptionAction(subscriptionId, Action.RESUME), executor);
    }

    public CompletableFuture<Void> cancelSubscription(@NonNull String subscriptionId) {
        return CompletableFuture.runAsync(() -> executeSubscriptionAction(subscriptionId, Action.CANCEL), executor);
    }

    public CompletableFuture<Void> keepSubscription(@NonNull String subscriptionId) {
        return CompletableFuture.runAsync(() -> executeSubscriptionAction(subscriptionId, Action.KEEP), executor);
    }

    public CompletableFuture<Void> chargeNowSubscription(@NonNull String subscriptionId) {
        return CompletableFuture.runAsync(() -> executeSubscriptionAction(subscriptionId, Action.CHARGE_NOW), executor);
    }

    public CompletableFuture<Void> recordSubscriptionUsage(@NonNull SubscriptionUsageInput input) {
        return CompletableFuture.runAsync(() -> {
            try {
                AccountProfile profile = getAccountProfile();
                String privateKey = deriveExposedPrivateKey(profile);
                subscriptionRepository.recordSubscriptionUsage(profile.getAccountId(), privateKey, input);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    public void clearAccountProfile() {
        accountPrefs.load().ifPresent(profile -> accountPrefs.removeAccount(profile.getAccountId()));
    }

    private AccountShareInfo buildAccountShareInfo(AccountProfile profile) {
        String identityJson = profile.getIdentityManifest() != null
                ? profile.getIdentityManifest().toCanonicalJson()
                : null;
        return new AccountShareInfo(
                profile.getDisplayName(),
                profile.getAccountId(),
                profile.getDomain(),
                profile.getPreferredAssetId(),
                identityJson
        );
    }

    private String resolveAccountTargetInternal(String rawLiteral) throws Exception {
        AccountLiteralFormatter.ParsedAccountLiteral parsed =
                AccountLiteralFormatter.normalize(rawLiteral, toriiConfig);
        if (!parsed.isAlias()) {
            return parsed.literal();
        }
        AccountProfile profile = getAccountProfile();
        KeyPair keyPair = keyManager.generateOrLoad(
                profile.getKeyAlias(),
                IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);
        return toriiClient.resolveAccountAlias(parsed.literal(), profile.getAccountId(), keyPair.getPrivate());
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
        if (receiverAccountId == null || receiverAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver account ID is required");
        }
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount must not be empty");
        }
        AccountProfile profile = getAccountProfile();
        if (receiverAccountId.equalsIgnoreCase(profile.getAccountId())) {
            throw new IllegalArgumentException(context.getString(R.string.error_message_cannot_send_to_myself));
        }
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
                IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);

        final TransactionBuilder builder = new TransactionBuilder(codecAdapter, keyManager);
        final SignedTransaction transaction = builder.encodeAndSign(payload, signer);
        toriiClient.submitTransaction(transaction);
    }

    private void executeSubscriptionAction(String subscriptionId, Action action) {
        try {
            AccountProfile profile = getAccountProfile();
            String privateKey = deriveExposedPrivateKey(profile);
            if (action == Action.PAUSE) {
                subscriptionRepository.pauseSubscription(subscriptionId, profile.getAccountId(), privateKey);
            } else if (action == Action.RESUME) {
                subscriptionRepository.resumeSubscription(subscriptionId, profile.getAccountId(), privateKey);
            } else if (action == Action.CANCEL) {
                subscriptionRepository.cancelSubscription(subscriptionId, profile.getAccountId(), privateKey);
            } else if (action == Action.KEEP) {
                subscriptionRepository.keepSubscription(subscriptionId, profile.getAccountId(), privateKey);
            } else if (action == Action.CHARGE_NOW) {
                subscriptionRepository.chargeNowSubscription(subscriptionId, profile.getAccountId(), privateKey);
            } else {
                throw new IllegalStateException("Unknown subscription action");
            }
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
    }

    private String deriveExposedPrivateKey(AccountProfile profile) throws Exception {
        try {
            KeyPair keyPair = keyManager.generateOrLoad(
                    profile.getKeyAlias(),
                    IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);
            return ExposedPrivateKeyEncoder.encodePrivateKey(keyPair.getPrivate());
        } catch (Exception ex) {
            throw new IllegalStateException("Key management error: failed to derive account private key");
        }
    }

    private enum Action {
        PAUSE,
        RESUME,
        CANCEL,
        KEEP,
        CHARGE_NOW
    }

    private static int generateNonce() {
        long value = System.currentTimeMillis();
        return (int) (value & 0x7FFFFFFF);
    }
}
