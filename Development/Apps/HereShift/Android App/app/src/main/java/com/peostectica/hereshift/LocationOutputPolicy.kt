package com.peostectica.hereshift

import kotlin.math.floor

/**
 * Output boundary for Free accounts. A user may type an exact test coordinate,
 * but the displayed and device-wide emitted location must be approximate.
 *
 * No trusted Google Play verification is connected to this build. Consequently
 * all outputs are Free and must remain coarse, including direct service intents.
 * This policy is NOT a billing or entitlement-verification implementation.
 */
internal object LocationOutputPolicy {
    const val FREE_ACCURACY_METERS = 20_000f
    private const val FREE_GRID_DEGREES = 0.1

    fun approximate(point: TestPoint): TestPoint {
        fun center(value: Double, minimum: Double, maximum: Double): Double =
            (floor(value / FREE_GRID_DEGREES + 0.5) * FREE_GRID_DEGREES)
                .coerceIn(minimum, maximum)
        return TestPoint(
            center(point.latitude, -90.0, 90.0),
            center(point.longitude, -180.0, 180.0)
        )
    }

    // No caller-supplied preference, intent flag, or purchase token can upgrade
    // the output. A future verified Premium implementation must replace this
    // boundary in both the Activity and the Service, with independent tests.
    fun forCurrentBuild(point: TestPoint): TestPoint = approximate(point)
}
