package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min

@Composable
fun TremorScreen(modifier: Modifier = Modifier, viewModel: TremorViewModel = viewModel()) {
    val stats = viewModel.stats
    val level = viewModel.level
    val running = viewModel.isRunning
    val available = viewModel.sensorAvailable
    val waveform = viewModel.waveform

    val accent = levelColor(level)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Тремор-анализатор",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Положите телефон на ладонь и нажмите «Старт». Держите руку вытянутой 10–20 секунд.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        if (!available) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.15f))
            ) {
                Text(
                    text = "Акселерометр не найден на этом устройстве.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFB00020),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LevelCard(level = level, accent = accent)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "RMS",
                value = formatFloat(stats.rms),
                unit = "м/с²",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Пик",
                value = formatFloat(stats.peak),
                unit = "м/с²",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Частота",
                value = formatFloat(stats.frequencyHz),
                unit = "Гц",
                modifier = Modifier.weight(1f)
            )
        }

        IntensityBar(rms = stats.rms, accent = accent)

        WaveformCard(waveform = waveform, accent = accent)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!running) {
                Button(
                    onClick = { viewModel.start() },
                    enabled = available,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Старт")
                }
            } else {
                Button(
                    onClick = { viewModel.stop() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Стоп")
                }
            }
            OutlinedButton(
                onClick = { viewModel.reset() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сброс")
            }
        }
    }
}

@Composable
private fun LevelCard(level: TremorLevel, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = level.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = level.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IntensityBar(rms: Float, accent: Color) {
    val fraction = min(rms / 2.5f, 1f)
    Column {
        Text(
            text = "Интенсивность",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.6f), accent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun WaveformCard(waveform: List<Float>, accent: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (waveform.isEmpty()) return@Canvas
            val maxValue = (waveform.maxOrNull() ?: 1f).coerceAtLeast(0.5f)
            val stepX = size.width / (waveform.size - 1).coerceAtLeast(1)
            val midY = size.height / 2f

            val path = Path()
            waveform.forEachIndexed { index, value ->
                val x = index * stepX
                val normalized = (value / maxValue).coerceIn(0f, 1f)
                val y = midY - normalized * (size.height / 2f - 4f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val pathBottom = Path()
            waveform.forEachIndexed { index, value ->
                val x = index * stepX
                val normalized = (value / maxValue).coerceIn(0f, 1f)
                val y = midY + normalized * (size.height / 2f - 4f)
                if (index == 0) pathBottom.moveTo(x, y) else pathBottom.lineTo(x, y)
            }

            drawLine(
                color = accent.copy(alpha = 0.25f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1f
            )
            drawPath(path = path, color = accent, style = Stroke(width = 3f))
            drawPath(path = pathBottom, color = accent.copy(alpha = 0.6f), style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun levelColor(level: TremorLevel): Color = when (level) {
    TremorLevel.NONE -> Color(0xFF2E7D32)
    TremorLevel.LIGHT -> Color(0xFFFBC02D)
    TremorLevel.MODERATE -> Color(0xFFEF6C00)
    TremorLevel.SEVERE -> Color(0xFFC62828)
}

private fun formatFloat(value: Float): String = "%.2f".format(value)
