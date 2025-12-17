package io.livekit.android.example.voiceassistant.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ConnectionStatus {
    CONNECTING,
    CONNECTED,
    LISTENING,
    PROCESSING,
    DISCONNECTED
}

@Composable
fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        ConnectionStatus.CONNECTING -> Color(0xFFFFA500) // Orange
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50) // Green
        ConnectionStatus.LISTENING -> Color(0xFF2196F3) // Blue
        ConnectionStatus.PROCESSING -> Color(0xFF9C27B0) // Purple
        ConnectionStatus.DISCONNECTED -> Color(0xFFF44336) // Red
    }
    
    val statusText = when (status) {
        ConnectionStatus.CONNECTING -> "Connecting..."
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.LISTENING -> "Listening..."
        ConnectionStatus.PROCESSING -> "Processing..."
        ConnectionStatus.DISCONNECTED -> "Disconnected"
    }
    
    val pulseAlpha by animateFloatAsState(
        targetValue = if (status == ConnectionStatus.LISTENING || status == ConnectionStatus.PROCESSING) 0.3f else 1f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 0),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                    .alpha(pulseAlpha)
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

