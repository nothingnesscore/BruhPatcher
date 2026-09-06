package com.nothingness.bruhpatcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nothingness.bruhpatcher.model.Feature
import com.nothingness.bruhpatcher.ui.theme.AppColors

@Composable
fun FeatureCheckbox(
    feature: Feature,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (feature.isEnabled) {
            AppColors.Primary.copy(alpha = 0.1f)
        } else {
            AppColors.DarkSurfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(200), label = "featureBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled && !feature.requiresMiui) {
                onCheckedChange(!feature.isEnabled)
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = feature.isEnabled,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled && (!feature.requiresMiui || feature.isEnabled),
            colors = CheckboxDefaults.colors(
                checkedColor = AppColors.Primary,
                uncheckedColor = AppColors.TextMuted,
                checkmarkColor = AppColors.TextPrimary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = feature.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) AppColors.TextPrimary else AppColors.TextMuted
                )
                if (feature.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.Primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (feature.requiresMiui && !feature.isEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MIUI Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Warning,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.Warning.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}
