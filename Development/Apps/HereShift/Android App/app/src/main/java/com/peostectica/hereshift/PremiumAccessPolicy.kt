package com.peostectica.hereshift

/**
 * Single decision point for paid features. UI preferences, a purchase button, and a local
 * purchase token are NEVER proof of Premium. Only a trusted server verification can
 * authorize an entitlement, scoped to the current authenticated account.
 *
 * This build has no trusted entitlement-verification backend, so it MUST deny access.
 */
internal object PremiumAccessPolicy {
    enum class Feature { CIRCLE_REMOTE_SHARING, CIRCLE_REMOTE_LOCATION, TAP_VOYAGE_INTEGRATION }

    data class VerifiedEntitlement(
        val accountId: String,
        val verifiedByTrustedServer: Boolean,
        val active: Boolean,
        val expiryEpochMillis: Long,
        val revoked: Boolean,
        val serverVerifiedAtEpochMillis: Long
    )

    private const val MAX_VERIFICATION_AGE_MS = 5 * 60 * 1000L

    fun allowed(
        feature: Feature,
        entitlement: VerifiedEntitlement?,
        authenticatedAccountId: String?,
        nowEpochMillis: Long
    ): Boolean {
        // Every feature is paid, and authorization is independent of Circle contact consent.
        if (feature !in Feature.values()) return false
        val e = entitlement ?: return false
        if (authenticatedAccountId.isNullOrBlank() || e.accountId != authenticatedAccountId) return false
        if (!e.verifiedByTrustedServer || !e.active || e.revoked) return false
        if (nowEpochMillis < 0L || e.expiryEpochMillis <= nowEpochMillis) return false
        if (e.serverVerifiedAtEpochMillis > nowEpochMillis) return false
        if (nowEpochMillis - e.serverVerifiedAtEpochMillis > MAX_VERIFICATION_AGE_MS) return false
        return true
    }

    // Deliberately deny in this version: neither billing nor trusted verification is wired up.
    fun currentBuildCanUse(feature: Feature): Boolean =
        allowed(feature, null, null, System.currentTimeMillis())
}
