package com.example.muyinteresante.util;

/**
 * Decides when a remote request may start and how to classify an ambiguous
 * transport failure after the real request has already been attempted.
 */
public final class RemoteRequestPolicy {
    private RemoteRequestPolicy() {
    }

    public enum FailureClassification {
        FEED_UNAVAILABLE,
        GENERAL_CONNECTIVITY_UNAVAILABLE
    }

    /** A cheap guard only; it must not perform DNS, HTTP, TLS, or ICMP probes. */
    public static boolean shouldStartRequest(boolean usableNetwork) {
        return usableNetwork;
    }

    /** Never run the active diagnosis after any valid HTTP response. */
    public static boolean shouldDiagnoseAfterFailure(
            boolean receivedHttpResponse,
            boolean ambiguousConnectivityFailure) {
        return !receivedHttpResponse && ambiguousConnectivityFailure;
    }

    public static FailureClassification classifyAmbiguousFailure(boolean generalInternetReachable) {
        return generalInternetReachable
                ? FailureClassification.FEED_UNAVAILABLE
                : FailureClassification.GENERAL_CONNECTIVITY_UNAVAILABLE;
    }
}
