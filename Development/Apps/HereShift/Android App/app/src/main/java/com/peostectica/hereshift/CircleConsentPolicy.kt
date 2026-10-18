package com.peostectica.hereshift

/**
 * A Premium subscription is NEVER consent to view someone else's location.
 * This policy evaluates a single contact's explicit, current, revocable grant.
 * The authoritative grant must be fetched from an authenticated server; locally
 * saved contacts, UI selection, invitations, and cached approvals are insufficient.
 */
internal object CircleConsentPolicy {
    data class ServerGrant(
        val ownerAccountId: String,
        val viewerAccountId: String,
        val authenticatedByServer: Boolean,
        val approvedByOwner: Boolean,
        val revoked: Boolean,
        val expiresAtEpochMillis: Long,
        val verifiedAtEpochMillis: Long,
        val grantId: String
    )

    private const val MAX_GRANT_VERIFICATION_AGE_MS = 60_000L

    fun canView(
        grant: ServerGrant?,
        signedInViewerAccountId: String?,
        selectedOwnerAccountId: String?,
        nowEpochMillis: Long,
        premiumAuthorized: Boolean
    ): Boolean {
        if (!premiumAuthorized) return false
        val g = grant ?: return false
        if (signedInViewerAccountId.isNullOrBlank() || selectedOwnerAccountId.isNullOrBlank()) return false
        if (g.ownerAccountId.isBlank() || g.grantId.isBlank()) return false
        if (g.ownerAccountId == g.viewerAccountId) return false
        if (g.viewerAccountId != signedInViewerAccountId || g.ownerAccountId != selectedOwnerAccountId) return false
        if (!g.authenticatedByServer || !g.approvedByOwner || g.revoked) return false
        if (nowEpochMillis < 0 || g.expiresAtEpochMillis <= nowEpochMillis) return false
        if (g.verifiedAtEpochMillis > nowEpochMillis) return false
        if (nowEpochMillis - g.verifiedAtEpochMillis > MAX_GRANT_VERIFICATION_AGE_MS) return false
        return true
    }

    // No authenticated Circle backend exists in this build. Never use local
    // SharedPreferences to synthesize or restore a server-side grant.
    fun currentBuildCanView(): Boolean = canView(null, null, null, System.currentTimeMillis(), false)
}
