package io.soramitsu.examplepoint.sdk.identity;

import android.os.Build;

import androidx.annotation.NonNull;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Captures a lightweight device fingerprint that becomes part of the Nexus identity manifest.
 */
public final class NexusDeviceReport {

    private final String manufacturer;
    private final String model;
    private final String device;
    private final String osRelease;
    private final String securityPatch;
    private final boolean strongBoxCapable;
    private final long capturedAtMs;

    NexusDeviceReport(
            String manufacturer,
            String model,
            String device,
            String osRelease,
            String securityPatch,
            boolean strongBoxCapable,
            long capturedAtMs
    ) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.device = device;
        this.osRelease = osRelease;
        this.securityPatch = securityPatch;
        this.strongBoxCapable = strongBoxCapable;
        this.capturedAtMs = capturedAtMs;
    }

    public static NexusDeviceReport capture(boolean hasStrongBox) {
        final String securityPatch = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? Build.VERSION.SECURITY_PATCH : "unknown";
        return new NexusDeviceReport(
                safe(Build.MANUFACTURER),
                safe(Build.MODEL),
                safe(Build.DEVICE),
                safe(Build.VERSION.RELEASE),
                securityPatch != null ? securityPatch : "unknown",
                hasStrongBox,
                System.currentTimeMillis()
        );
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }

    void write(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("manufacturer").value(manufacturer);
        writer.name("model").value(model);
        writer.name("device").value(device);
        writer.name("os_release").value(osRelease);
        writer.name("security_patch").value(securityPatch);
        writer.name("strongbox_capable").value(strongBoxCapable);
        writer.name("captured_at_ms").value(capturedAtMs);
        writer.endObject();
    }

    @NonNull
    public String getManufacturer() {
        return manufacturer;
    }

    @NonNull
    public String getModel() {
        return model;
    }

    @NonNull
    public String getDevice() {
        return device;
    }

    @NonNull
    public String getOsRelease() {
        return osRelease;
    }

    @NonNull
    public String getSecurityPatch() {
        return securityPatch;
    }

    public boolean isStrongBoxCapable() {
        return strongBoxCapable;
    }

    public long getCapturedAtMs() {
        return capturedAtMs;
    }
}
