package io.soramitsu.examplepoint.sdk.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Canonical identity manifest used to derive Nexus UAIDs. The manifest is hashed (BLAKE2b-256) and
 * prefixed with {@code uaid:} to produce the Universal Account ID.
 */
public final class NexusIdentityManifest {

    private static final int CURRENT_VERSION = 1;
    private static final Gson gson = new Gson();

    private final int version;
    private final String legalName;
    private final String documentType;
    private final String documentNumber;
    private final String residencyCountry;
    private final String contact;
    private final long issuedAtMs;
    private final NexusDeviceReport deviceReport;

    private NexusIdentityManifest(Builder builder, NexusDeviceReport deviceReport, long issuedAtMs) {
        this.version = CURRENT_VERSION;
        this.legalName = builder.legalName;
        this.documentType = builder.documentType;
        this.documentNumber = builder.documentNumber;
        this.residencyCountry = builder.residencyCountry;
        this.contact = builder.contact;
        this.deviceReport = deviceReport;
        this.issuedAtMs = issuedAtMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Serialises the manifest into a canonical JSON string with a stable field order so the UAID
     * derivation is deterministic across platforms.
     */
    @NonNull
    public String toCanonicalJson() {
        try {
            final StringWriter buffer = new StringWriter();
            final JsonWriter writer = new JsonWriter(buffer);
            writer.setSerializeNulls(false);
            writer.beginObject();
            writer.name("version").value(version);
            writer.name("legal_name").value(legalName);
            writer.name("document_type").value(documentType);
            writer.name("document_number").value(documentNumber);
            writer.name("residency_country").value(residencyCountry);
            writer.name("contact").value(contact);
            writer.name("issued_at_ms").value(issuedAtMs);
            writer.name("device_report");
            deviceReport.write(writer);
            writer.endObject();
            writer.flush();
            return buffer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode identity manifest", e);
        }
    }

    /**
     * Encodes the manifest to UTF-8 bytes for hashing.
     */
    @NonNull
    public byte[] toCanonicalBytes() {
        return toCanonicalJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @NonNull
    public NexusDeviceReport getDeviceReport() {
        return deviceReport;
    }

    @NonNull
    public String getLegalName() {
        return legalName;
    }

    @NonNull
    public String getDocumentType() {
        return documentType;
    }

    @NonNull
    public String getDocumentNumber() {
        return documentNumber;
    }

    @NonNull
    public String getResidencyCountry() {
        return residencyCountry;
    }

    @NonNull
    public String getContact() {
        return contact;
    }

    public long getIssuedAtMs() {
        return issuedAtMs;
    }

    public static class Builder {
        private String legalName;
        private String documentType;
        private String documentNumber;
        private String residencyCountry;
        private String contact;

        private Builder() {}

        public Builder setLegalName(@NonNull String legalName) {
            this.legalName = legalName;
            return this;
        }

        public Builder setDocumentType(@NonNull String documentType) {
            this.documentType = documentType;
            return this;
        }

        public Builder setDocumentNumber(@NonNull String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        public Builder setResidencyCountry(@NonNull String residencyCountry) {
            this.residencyCountry = residencyCountry;
            return this;
        }

        public Builder setContact(@NonNull String contact) {
            this.contact = contact;
            return this;
        }

        public NexusIdentityManifest build(@NonNull NexusDeviceReport report, long issuedAtMs) {
            if (report == null) {
                throw new IllegalArgumentException("Device report is required");
            }
            if (isEmpty(legalName) || isEmpty(documentType) || isEmpty(documentNumber)
                    || isEmpty(residencyCountry) || isEmpty(contact)) {
                throw new IllegalStateException("Identity manifest fields must be populated");
            }
            return new NexusIdentityManifest(this, report, issuedAtMs);
        }

        private boolean isEmpty(@Nullable String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    /**
     * Simple DTO for Gson round-trips when persisting the manifest in SharedPreferences.
     */
    public static final class JsonRepresentation {
        @SerializedName("version")
        public int version;
        @SerializedName("legal_name")
        public String legalName;
        @SerializedName("document_type")
        public String documentType;
        @SerializedName("document_number")
        public String documentNumber;
        @SerializedName("residency_country")
        public String residencyCountry;
        @SerializedName("contact")
        public String contact;
        @SerializedName("issued_at_ms")
        public long issuedAtMs;
        @SerializedName("device_report")
        public DeviceReportRepresentation deviceReport;

        public static final class DeviceReportRepresentation {
            @SerializedName("manufacturer")
            public String manufacturer;
            @SerializedName("model")
            public String model;
            @SerializedName("device")
            public String device;
            @SerializedName("os_release")
            public String osRelease;
            @SerializedName("security_patch")
            public String securityPatch;
            @SerializedName("strongbox_capable")
            public boolean strongBoxCapable;
            @SerializedName("captured_at_ms")
            public long capturedAtMs;
        }
    }

    public String toStorageJson() {
        return toCanonicalJson();
    }

    @Nullable
    public static NexusIdentityManifest fromStorageJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JsonRepresentation dto = gson.fromJson(new StringReader(json), JsonRepresentation.class);
            if (dto == null || dto.deviceReport == null) {
                return null;
            }
            NexusDeviceReport report = new NexusDeviceReport(
                    dto.deviceReport.manufacturer,
                    dto.deviceReport.model,
                    dto.deviceReport.device,
                    dto.deviceReport.osRelease,
                    dto.deviceReport.securityPatch,
                    dto.deviceReport.strongBoxCapable,
                    dto.deviceReport.capturedAtMs
            );
            Builder builder = builder()
                    .setLegalName(dto.legalName)
                    .setDocumentType(dto.documentType)
                    .setDocumentNumber(dto.documentNumber)
                    .setResidencyCountry(dto.residencyCountry)
                    .setContact(dto.contact);
            return builder.build(report, dto.issuedAtMs);
        } catch (JsonSyntaxException | IllegalStateException ex) {
            return null;
        }
    }
}
