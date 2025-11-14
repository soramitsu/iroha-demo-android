package io.soramitsu.examplepoint.sdk.registration;

import androidx.annotation.NonNull;

/**
 * Immutable value object that captures the information required to onboard a user on Sora Nexus.
 * This is populated by the registration UI and later combined with device attestation data
 * before the UAID is derived.
 */
public final class AccountRegistrationRequest {

    private final String displayName;
    private final String legalName;
    private final String documentType;
    private final String documentNumber;
    private final String residencyCountry;
    private final String contact;
    private final String keyAlias;

    public AccountRegistrationRequest(
            @NonNull String displayName,
            @NonNull String legalName,
            @NonNull String documentType,
            @NonNull String documentNumber,
            @NonNull String residencyCountry,
            @NonNull String contact,
            @NonNull String keyAlias
    ) {
        this.displayName = displayName;
        this.legalName = legalName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.residencyCountry = residencyCountry;
        this.contact = contact;
        this.keyAlias = keyAlias;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
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

    @NonNull
    public String getKeyAlias() {
        return keyAlias;
    }
}
