package com.example.autograbber.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autograbber.ui.theme.*
import java.util.Locale

@Composable
fun SectionHeader(title: String) {
    val colors = LocalV2Colors.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = colors.textSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun DualInputPreference(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isDecimal: Boolean = true,
    unit: String = ""
) {
    val colors = LocalV2Colors.current
    var textValue by remember {
        mutableStateOf(if (isDecimal) String.format(Locale.US, "%.2f", value) else value.toInt().toString())
    }

    LaunchedEffect(value) {
        val parsed = textValue.toFloatOrNull()
        if (parsed != value) {
            textValue = if (isDecimal) String.format(Locale.US, "%.2f", value) else value.toInt().toString()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isDecimal) {
                            val prefix = if (label.contains("Pay", true)) "$" else ""
                            "Range: $prefix${String.format(Locale.US, "%.2f", valueRange.start)} - $prefix${String.format(Locale.US, "%.2f", valueRange.endInclusive)}"
                        } else {
                            val displayUnit = if (unit == "mi") " miles" else unit
                            "Range: ${valueRange.start.toInt()} - ${valueRange.endInclusive.toInt()}$displayUnit"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        // Allow any numeric-like input during typing to avoid jumping
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            textValue = newValue
                            val parsed = newValue.toFloatOrNull()
                            if (parsed != null) {
                                // Only notify parent if it's a valid number, but don't clamp the text field yet
                                onValueChange(parsed)
                            }
                        }
                    },
                    modifier = Modifier.width(105.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End,
                        color = colors.textPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    prefix = { 
                        if (isDecimal && (label.contains("Pay", ignoreCase = true))) {
                            Text("$", color = V2Primary, fontWeight = FontWeight.Bold) 
                        } 
                    },
                    suffix = { 
                        if (unit.isNotEmpty()) {
                            Text(unit, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        } 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
fun PreferenceSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalV2Colors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    label, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    description, 
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = V2Primary,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.background
                )
            )
        }
    }
}

@Composable
fun FilterGridItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalV2Colors.current
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEditDialog(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isDecimal: Boolean,
    unit: String,
    onDismiss: () -> Unit
) {
    var textValue by remember {
        mutableStateOf(if (isDecimal) String.format(Locale.US, "%.2f", value) else value.toInt().toString())
    }

    LaunchedEffect(value) {
        val parsed = textValue.toFloatOrNull()
        if (parsed != value) {
            textValue = if (isDecimal) String.format(Locale.US, "%.2f", value) else value.toInt().toString()
        }
    }

    val colors = LocalV2Colors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = label,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            textValue = newValue
                            val parsed = newValue.toFloatOrNull()
                            if (parsed != null) {
                                onValueChange(parsed)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    prefix = { 
                        if (unit == "$" || label.contains("Pay", true)) {
                            Text("$", color = V2Primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    suffix = { if (unit.isNotEmpty() && unit != "$") Text(unit, color = colors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.textPrimary.copy(alpha = 0.03f),
                        unfocusedContainerColor = colors.textPrimary.copy(alpha = 0.03f)
                    )
                )
                
                Text(
                    text = if (isDecimal) {
                        val prefix = if (unit == "$" || label.contains("Pay", true)) "$" else ""
                        "Min: $prefix${String.format(Locale.US, "%.2f", valueRange.start)} • Max: $prefix${String.format(Locale.US, "%.2f", valueRange.endInclusive)}"
                    } else {
                        val displayUnit = if (unit.trim() == "mi") " miles" else unit
                        "Range: ${valueRange.start.toInt()} - ${valueRange.endInclusive.toInt()}$displayUnit"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = V2Primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Apply Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}
