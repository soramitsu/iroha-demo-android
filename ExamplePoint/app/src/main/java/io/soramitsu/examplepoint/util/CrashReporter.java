package io.soramitsu.examplepoint.util;

import android.content.Context;
import android.util.Log;

/**
 * Lightweight placeholder crash reporter so we can keep logging hooks in place
 * without depending on the deprecated Fabric Crashlytics SDK.
 */
public final class CrashReporter {

    private static final String DEFAULT_TAG = "CrashReporter";
    private static boolean initialized;

    private CrashReporter() {
        // no-op
    }

    public static void init(Context context) {
        initialized = true;
        Log.i(DEFAULT_TAG, "CrashReporter initialized (no-op implementation)");
    }

    public static void logError(String tag, Throwable throwable) {
        logError(tag, throwable != null ? throwable.getMessage() : null, throwable);
    }

    public static void logError(String tag, String message) {
        logError(tag, message, null);
    }

    public static void logError(String tag, String message, Throwable throwable) {
        if (tag == null || tag.isEmpty()) {
            tag = DEFAULT_TAG;
        }
        if (!initialized) {
            Log.w(DEFAULT_TAG, "CrashReporter used before initialization");
        }
        if (throwable != null) {
            Log.e(tag, message != null ? message : throwable.getMessage(), throwable);
        } else if (message != null) {
            Log.e(tag, message);
        } else {
            Log.e(tag, "Unknown error reported");
        }
    }
}
