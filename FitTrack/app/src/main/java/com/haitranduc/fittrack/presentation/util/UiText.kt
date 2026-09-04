package com.haitranduc.fittrack.presentation.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.repository.DataError

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args.toTypedArray())
    }

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(resId, *args.toTypedArray())
    }
}

fun DataError.toUiText(): UiText = when (this) {
    is DataError.Database -> UiText.StringResource(R.string.error_database)
    is DataError.Unknown -> UiText.StringResource(R.string.error_unknown)
}
