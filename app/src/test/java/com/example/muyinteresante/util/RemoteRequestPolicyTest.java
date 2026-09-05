package com.example.muyinteresante.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteRequestPolicyTest {
    @Test
    public void offlineGuardSkipsRemoteRequest() {
        assertFalse(RemoteRequestPolicy.shouldStartRequest(false));
        assertFalse(RemoteRequestPolicy.shouldDiagnoseAfterFailure(false, false));
    }

    @Test
    public void usableNetworkStartsTheRealRequestDirectly() {
        assertTrue(RemoteRequestPolicy.shouldStartRequest(true));
    }

    @Test
    public void validHttpFailureDoesNotTriggerGeneralDiagnosis() {
        assertFalse(RemoteRequestPolicy.shouldDiagnoseAfterFailure(true, true));
    }

    @Test
    public void ambiguousFeedFailureWithWorkingInternetIsFeedUnavailable() {
        assertTrue(RemoteRequestPolicy.shouldDiagnoseAfterFailure(false, true));
        assertEquals(
                RemoteRequestPolicy.FailureClassification.FEED_UNAVAILABLE,
                RemoteRequestPolicy.classifyAmbiguousFailure(true));
    }

    @Test
    public void ambiguousFailureWithoutWorkingInternetIsConnectivityUnavailable() {
        assertEquals(
                RemoteRequestPolicy.FailureClassification.GENERAL_CONNECTIVITY_UNAVAILABLE,
                RemoteRequestPolicy.classifyAmbiguousFailure(false));
    }
}
