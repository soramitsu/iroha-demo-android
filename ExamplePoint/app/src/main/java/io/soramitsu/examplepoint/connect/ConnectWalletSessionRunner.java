package io.soramitsu.examplepoint.connect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hyperledger.iroha.android.connect.ConnectCrypto;
import org.hyperledger.iroha.android.connect.ConnectDirection;
import org.hyperledger.iroha.android.connect.ConnectEnvelopeCodec;
import org.hyperledger.iroha.android.connect.ConnectFrameCodec;
import org.hyperledger.iroha.android.connect.ConnectProtocolException;
import org.hyperledger.iroha.android.connect.ConnectWalletRequest;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/** Wallet-role Connect protocol runner for approve/reject/sign flows. */
public final class ConnectWalletSessionRunner {

    private static final int REJECT_CODE_USER_DENIED = 403;
    private static final String REJECT_CODE_ID_USER_DENIED = "USER_DENIED";

    private static final long OPEN_TIMEOUT_MS = 20_000L;
    private static final long FRAME_TIMEOUT_MS = 120_000L;

    private ConnectWalletSessionRunner() {
    }

    public enum Decision {
        APPROVE,
        REJECT,
        REJECT_AFTER_REQUEST
    }

    public enum Outcome {
        SIGNED,
        REJECTED_BY_USER,
        REJECTED_AFTER_REQUEST,
        CLOSED_WITHOUT_SIGN
    }

    public interface SessionListener {
        void onStage(@NonNull String stage);
    }

    public static final class Result {
        private final Outcome outcome;
        @Nullable
        private final String domainTag;

        public Result(@NonNull Outcome outcome, @Nullable String domainTag) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.domainTag = domainTag;
        }

        @NonNull
        public Outcome outcome() {
            return outcome;
        }

        @Nullable
        public String domainTag() {
            return domainTag;
        }
    }

    @NonNull
    public static Result run(@NonNull ConnectWalletRequest request,
                             @NonNull ConnectSigningIdentity signer,
                             @NonNull Decision decision,
                             @Nullable SessionListener listener) throws Exception {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(signer, "signer");
        Objects.requireNonNull(decision, "decision");

        notifyStage(listener, "ws_connect");
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        ConnectWebSocketChannel channel = new ConnectWebSocketChannel(client);

        byte[] sid = request.sessionId();
        try {
            Request wsRequest = new Request.Builder()
                    .url(request.webSocketUri().toString())
                    .addHeader("Authorization", "Bearer " + request.token())
                    .build();
            channel.connect(wsRequest, OPEN_TIMEOUT_MS);

            notifyStage(listener, "wait_open_control");
            ConnectFrameCodec.DecodedFrame openFrame =
                    ConnectFrameCodec.decode(channel.nextBinary(FRAME_TIMEOUT_MS));
            if (openFrame.type() != ConnectFrameCodec.FrameType.OPEN) {
                throw new ConnectProtocolException("Expected OPEN control frame from app");
            }
            if (openFrame.direction() != ConnectDirection.APP_TO_WALLET) {
                throw new ConnectProtocolException("Connect OPEN frame direction mismatch");
            }

            if (decision == Decision.REJECT) {
                notifyStage(listener, "send_reject");
                byte[] reject = ConnectFrameCodec.encodeRejectFrame(
                        sid,
                        1L,
                        REJECT_CODE_USER_DENIED,
                        REJECT_CODE_ID_USER_DENIED,
                        "Rejected by wallet user");
                channel.sendBinary(reject);
                return new Result(Outcome.REJECTED_BY_USER, null);
            }

            notifyStage(listener, "send_approve");
            ConnectCrypto.KeyPair connectKeys = ConnectCrypto.generateKeyPair();
            byte[] approvePreimage = ConnectCrypto.buildApprovePreimage(
                    sid,
                    openFrame.open().appPublicKey(),
                    connectKeys.publicKey(),
                    signer.accountId(),
                    null,
                    null);
            byte[] approveSignature = signer.signer().sign(approvePreimage);
            byte[] approveFrame = ConnectFrameCodec.encodeApproveFrame(
                    sid,
                    1L,
                    connectKeys.publicKey(),
                    signer.accountId(),
                    approveSignature);
            channel.sendBinary(approveFrame);

            ConnectCrypto.DirectionKeys directionKeys = ConnectCrypto.deriveDirectionKeys(
                    connectKeys.privateKey(),
                    openFrame.open().appPublicKey(),
                    sid);

            boolean sawSignRequest = false;
            while (true) {
                notifyStage(listener, "wait_next_frame");
                ConnectFrameCodec.DecodedFrame frame =
                        ConnectFrameCodec.decode(channel.nextBinary(FRAME_TIMEOUT_MS));
                if (frame.direction() != ConnectDirection.APP_TO_WALLET) {
                    continue;
                }

                if (frame.type() == ConnectFrameCodec.FrameType.REJECT) {
                    ConnectFrameCodec.RejectControl reject = frame.reject();
                    throw new ConnectProtocolException(
                            "Connect rejected by app: " + reject.codeId() + " " + reject.reason());
                }
                if (frame.type() == ConnectFrameCodec.FrameType.CLOSE) {
                    return new Result(Outcome.CLOSED_WITHOUT_SIGN, sawSignRequest ? "signed_request_missing_result" : null);
                }
                if (frame.type() != ConnectFrameCodec.FrameType.CIPHERTEXT) {
                    continue;
                }

                ConnectFrameCodec.Ciphertext ciphertext = frame.ciphertext();
                byte[] plaintext;
                try {
                    plaintext = ConnectCrypto.decryptCiphertext(
                            ciphertext.aead(),
                            directionKeys.appToWallet(),
                            sid,
                            ciphertext.direction(),
                            frame.sequence());
                } catch (ConnectProtocolException decryptError) {
                    throw new ConnectProtocolException(
                            "Connect decrypt failed"
                                    + " frame_dir=" + frame.direction()
                                    + " frame_seq=" + frame.sequence()
                                    + " ct_dir=" + ciphertext.direction()
                                    + " ct_aead_len=" + ciphertext.aead().length,
                            decryptError);
                }
                ConnectEnvelopeCodec.DecodedEnvelope envelope;
                try {
                    envelope = ConnectEnvelopeCodec.decodeEnvelope(plaintext);
                } catch (ConnectProtocolException decodeError) {
                    throw new ConnectProtocolException(
                            "Connect envelope decode failed"
                                    + " frame_dir=" + frame.direction()
                                    + " frame_seq=" + frame.sequence()
                                    + " pt_len=" + plaintext.length
                                    + " pt_hex_head=" + hexHead(plaintext, 256),
                            decodeError);
                }
                if (envelope.sequence() != frame.sequence()) {
                    throw new ConnectProtocolException("Envelope sequence mismatch");
                }

                ConnectEnvelopeCodec.EnvelopePayload payload = envelope.payload();
                if (payload.kind() == ConnectEnvelopeCodec.PayloadKind.CONTROL_REJECT) {
                    ConnectEnvelopeCodec.ControlRejectPayload reject =
                            (ConnectEnvelopeCodec.ControlRejectPayload) payload;
                    throw new ConnectProtocolException(
                            "Connect rejected by app: " + reject.codeId() + " " + reject.reason());
                }
                if (payload.kind() == ConnectEnvelopeCodec.PayloadKind.CONTROL_CLOSE) {
                    return new Result(Outcome.CLOSED_WITHOUT_SIGN, sawSignRequest ? "signed_request_missing_result" : null);
                }
                if (payload.kind() == ConnectEnvelopeCodec.PayloadKind.DISPLAY_REQUEST) {
                    continue;
                }

                if (payload.kind() == ConnectEnvelopeCodec.PayloadKind.SIGN_REQUEST_RAW
                        || payload.kind() == ConnectEnvelopeCodec.PayloadKind.SIGN_REQUEST_TX) {
                    sawSignRequest = true;
                    byte[] bytesToSign;
                    String domainTag;
                    if (payload.kind() == ConnectEnvelopeCodec.PayloadKind.SIGN_REQUEST_RAW) {
                        ConnectEnvelopeCodec.SignRequestRawPayload raw =
                                (ConnectEnvelopeCodec.SignRequestRawPayload) payload;
                        bytesToSign = raw.bytes();
                        domainTag = raw.domainTag();
                    } else {
                        ConnectEnvelopeCodec.SignRequestTxPayload tx =
                                (ConnectEnvelopeCodec.SignRequestTxPayload) payload;
                        bytesToSign = tx.txBytes();
                        domainTag = "SIGN_REQUEST_TX";
                    }

                    if (decision == Decision.REJECT_AFTER_REQUEST) {
                        notifyStage(listener, "reject_after_request");
                        byte[] envelopeBytes = ConnectEnvelopeCodec.encodeControlRejectEnvelope(
                                envelope.sequence(),
                                REJECT_CODE_USER_DENIED,
                                REJECT_CODE_ID_USER_DENIED,
                                "Rejected after sign request by wallet user");
                        sendEncrypted(channel, sid, directionKeys, envelope.sequence(), envelopeBytes);
                        return new Result(Outcome.REJECTED_AFTER_REQUEST, domainTag);
                    }

                    notifyStage(listener, "send_sign_result");
                    byte[] signature = signer.signer().sign(bytesToSign);
                    byte[] envelopeBytes = ConnectEnvelopeCodec.encodeSignResultOkEnvelope(
                            envelope.sequence(),
                            signature,
                            "ed25519");
                    sendEncrypted(channel, sid, directionKeys, envelope.sequence(), envelopeBytes);
                    return new Result(Outcome.SIGNED, domainTag);
                }
            }
        } finally {
            channel.closeQuietly();
            client.dispatcher().executorService().shutdown();
        }
    }

    private static void sendEncrypted(@NonNull ConnectWebSocketChannel channel,
                                      @NonNull byte[] sid,
                                      @NonNull ConnectCrypto.DirectionKeys directionKeys,
                                      long sequence,
                                      @NonNull byte[] envelopeBytes) throws Exception {
        byte[] ciphertext = ConnectCrypto.encryptEnvelope(
                envelopeBytes,
                directionKeys.walletToApp(),
                sid,
                ConnectDirection.WALLET_TO_APP,
                sequence);
        byte[] frame = ConnectFrameCodec.encodeCiphertextFrame(
                sid,
                ConnectDirection.WALLET_TO_APP,
                sequence,
                ciphertext);
        channel.sendBinary(frame);
    }

    private static void notifyStage(@Nullable SessionListener listener, @NonNull String stage) {
        if (listener != null) {
            listener.onStage(stage);
        }
    }

    @NonNull
    private static String hexHead(@NonNull byte[] bytes, int maxBytes) {
        int len = Math.max(0, Math.min(bytes.length, maxBytes));
        StringBuilder sb = new StringBuilder(len * 2 + (bytes.length > len ? 3 : 0));
        for (int i = 0; i < len; i++) {
            int v = bytes[i] & 0xFF;
            sb.append(Character.forDigit((v >>> 4) & 0x0F, 16));
            sb.append(Character.forDigit(v & 0x0F, 16));
        }
        if (bytes.length > len) {
            sb.append("...");
        }
        return sb.toString();
    }

    private static final class ConnectWebSocketChannel {
        private final OkHttpClient client;
        private final BlockingQueue<InboundEvent> events = new LinkedBlockingQueue<>();
        private final CountDownLatch openLatch = new CountDownLatch(1);

        @Nullable
        private volatile WebSocket webSocket;
        @Nullable
        private volatile Throwable openFailure;

        ConnectWebSocketChannel(@NonNull OkHttpClient client) {
            this.client = client;
        }

        void connect(@NonNull Request request, long timeoutMs) throws Exception {
            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                    ConnectWebSocketChannel.this.webSocket = webSocket;
                    openLatch.countDown();
                }

                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                    events.offer(InboundEvent.binary(bytes.toByteArray()));
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket,
                                      @NonNull Throwable t,
                                      @Nullable Response response) {
                    if (openLatch.getCount() > 0) {
                        openFailure = t;
                        openLatch.countDown();
                    }
                    events.offer(InboundEvent.error(t));
                }

                @Override
                public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    events.offer(InboundEvent.closed(code, reason));
                }

                @Override
                public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    events.offer(InboundEvent.closed(code, reason));
                }
            });

            boolean opened = openLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!opened) {
                throw new IOException("Timed out waiting for connect websocket open");
            }
            if (openFailure != null) {
                throw new IOException("Connect websocket failed to open", openFailure);
            }
            if (webSocket == null) {
                throw new IOException("Connect websocket did not open");
            }
        }

        @NonNull
        byte[] nextBinary(long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                long remaining = Math.max(1L, deadline - System.currentTimeMillis());
                InboundEvent event = events.poll(remaining, TimeUnit.MILLISECONDS);
                if (event == null) {
                    continue;
                }
                if (event.type == InboundEvent.Type.BINARY) {
                    return event.data;
                }
                if (event.type == InboundEvent.Type.ERROR) {
                    throw new IOException("Connect websocket failed", event.error);
                }
                if (event.type == InboundEvent.Type.CLOSED) {
                    throw new IOException("Connect websocket closed code=" + event.closeCode + " reason=" + event.closeReason);
                }
            }
            throw new IOException("Timed out waiting for connect websocket frame");
        }

        void sendBinary(@NonNull byte[] bytes) throws IOException {
            WebSocket socket = webSocket;
            if (socket == null) {
                throw new IOException("Connect websocket not open");
            }
            boolean accepted = socket.send(ByteString.of(bytes));
            if (!accepted) {
                throw new IOException("Connect websocket rejected outgoing frame");
            }
        }

        void closeQuietly() {
            WebSocket socket = webSocket;
            if (socket != null) {
                try {
                    socket.close(1000, "done");
                } catch (RuntimeException ignored) {
                    // no-op
                }
            }
        }
    }

    private static final class InboundEvent {
        enum Type {
            BINARY,
            ERROR,
            CLOSED
        }

        final Type type;
        @Nullable
        final byte[] data;
        @Nullable
        final Throwable error;
        final int closeCode;
        @Nullable
        final String closeReason;

        private InboundEvent(Type type,
                             @Nullable byte[] data,
                             @Nullable Throwable error,
                             int closeCode,
                             @Nullable String closeReason) {
            this.type = type;
            this.data = data;
            this.error = error;
            this.closeCode = closeCode;
            this.closeReason = closeReason;
        }

        static InboundEvent binary(byte[] data) {
            return new InboundEvent(Type.BINARY, data, null, 0, null);
        }

        static InboundEvent error(Throwable error) {
            return new InboundEvent(Type.ERROR, null, error, 0, null);
        }

        static InboundEvent closed(int closeCode, String closeReason) {
            return new InboundEvent(Type.CLOSED, null, null, closeCode, closeReason);
        }
    }
}
