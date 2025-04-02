package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vibecoder.purrytify.presentation.theme.OutlineColor

/**
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback.
 * @param label The text label displayed above the text field.
 * @param modifier Modifier to be applied to the Column wrapping the label and field.
 * @param placeholderText The optional placeholder text to be displayed inside the field when empty.
 * @param keyboardOptions Software keyboard options that contains configuration such as KeyboardType
 * and ImeAction.
 * @param keyboardActions When the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * KeyboardOptions.imeAction.
 * @param visualTransformation Transforms the visual representation of the input value. For example,
 * you can use PasswordVisualTransformation to create a password text field. By default, no
 * visual transformation is applied.
 * @param trailingIcon A composable slot for displaying an icon at the end of the text field container.
 * @param isError Indicates if the text field's current value is in error. Affects label color.
 * @param enabled Controls the enabled state of the text field. When false, the text field will
 * be neither editable nor focusable, the input of the text field will not change.
 * @param singleLine When set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines.
 */
@Composable
fun TextInputComponent(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OutlineColor, MaterialTheme.shapes.small),
            placeholder = placeholderText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            shape = MaterialTheme.shapes.small,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                focusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
                disabledPlaceholderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                focusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                errorCursorColor = MaterialTheme.colorScheme.error,

            ),

            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
        )
    }
}