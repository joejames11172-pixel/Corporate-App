package com.peostectica.hereshift

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationPolicyTest {
    private val now = 1_800_000_000_000L

    private fun premium() = PremiumAccessPolicy.VerifiedEntitlement(
        accountId = "viewer", verifiedByTrustedServer = true, active = true,
        expiryEpochMillis = now + 600_000L, revoked = false,
        serverVerifiedAtEpochMillis = now - 1_000L
    )

    private fun grant() = CircleConsentPolicy.ServerGrant(
        ownerAccountId = "owner", viewerAccountId = "viewer",
        authenticatedByServer = true, approvedByOwner = true, revoked = false,
        expiresAtEpochMillis = now + 600_000L,
        verifiedAtEpochMillis = now - 1_000L, grantId = "grant-1"
    )

    @Test fun premiumRejectsAbsentRevokedExpiredAndWrongAccount() {
        val f = PremiumAccessPolicy.Feature.CIRCLE_REMOTE_SHARING
        assertFalse(PremiumAccessPolicy.allowed(f, null, "viewer", now))
        assertFalse(PremiumAccessPolicy.allowed(f, premium().copy(revoked = true), "viewer", now))
        assertFalse(PremiumAccessPolicy.allowed(f, premium().copy(expiryEpochMillis = now), "viewer", now))
        assertFalse(PremiumAccessPolicy.allowed(f, premium(), "attacker", now))
        assertFalse(PremiumAccessPolicy.allowed(f, premium().copy(serverVerifiedAtEpochMillis = now - 600_000L), "viewer", now))
        assertTrue(PremiumAccessPolicy.allowed(f, premium(), "viewer", now))
    }

    @Test fun circleRejectsMissingPremiumWrongViewerAndRevocation() {
        assertFalse(CircleConsentPolicy.canView(grant(), "viewer", "owner", now, false))
        assertFalse(CircleConsentPolicy.canView(grant(), "attacker", "owner", now, true))
        assertFalse(CircleConsentPolicy.canView(grant().copy(revoked = true), "viewer", "owner", now, true))
        assertFalse(CircleConsentPolicy.canView(grant().copy(approvedByOwner = false), "viewer", "owner", now, true))
        assertFalse(CircleConsentPolicy.canView(grant().copy(expiresAtEpochMillis = now), "viewer", "owner", now, true))
        assertFalse(CircleConsentPolicy.canView(grant().copy(verifiedAtEpochMillis = now - 120_000L), "viewer", "owner", now, true))
        assertTrue(CircleConsentPolicy.canView(grant(), "viewer", "owner", now, true))
    }

    @Test fun currentBuildDeniesUnverifiedRemoteAccess() {
        assertFalse(CircleConsentPolicy.currentBuildCanView())
        assertFalse(PremiumAccessPolicy.currentBuildCanUse(PremiumAccessPolicy.Feature.CIRCLE_REMOTE_LOCATION))
    }
}
