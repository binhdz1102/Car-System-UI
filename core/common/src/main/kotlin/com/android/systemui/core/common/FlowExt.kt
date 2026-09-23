package com.android.systemui.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/** Keeps platform callback failures observable without terminating a SystemUI state stream. */
fun <T> Flow<T>.logAndContinue(tag: String): Flow<T> = catch { throwable ->
    Timber.tag(tag).e(throwable, "Platform flow failed; retaining the last known state")
}
