package com.shorts.flowdrama

internal fun playerExitAdEligible(enteredAtMillis: Long, nowMillis: Long): Boolean =
    enteredAtMillis > 0 && nowMillis - enteredAtMillis >= 30_000L
