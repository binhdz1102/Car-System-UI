package com.android.systemui.core.common

/** Result used at platform boundaries where a vehicle/system service can reject a request. */
sealed interface ActionResult {
    data object Success : ActionResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : ActionResult
}
