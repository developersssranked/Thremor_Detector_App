package com.example.myapplication

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import kotlin.math.abs
import kotlin.math.sqrt

data class TremorStats(
    val currentMagnitude: Float = 0f,
    val rms: Float = 0f,
    val peak: Float = 0f,
    val frequencyHz: Float = 0f,
    val sampleCount: Int = 0
)

enum class TremorLevel(val title: String, val description: String) {
    NONE("Норма", "Тремор не обнаружен"),
    LIGHT("Лёгкий", "Незначительное дрожание"),
    MODERATE("Умеренный", "Заметное дрожание рук"),
    SEVERE("Сильный", "Выраженный тремор")
}

class TremorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager =
        application.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val isLinearSensor =
        accelerometer?.type == Sensor.TYPE_LINEAR_ACCELERATION

    private val windowSize = 128
    private val magnitudes = ArrayDeque<Float>(windowSize)

    private var lastEventTimestamp = 0L
    private var lastSignSample = 0f
    private var zeroCrossings = 0
    private var windowStartTime = 0L

    private val gravity = FloatArray(3)
    private val alpha = 0.8f

    var isRunning by mutableStateOf(false)
        private set

    var sensorAvailable by mutableStateOf(accelerometer != null)
        private set

    var stats by mutableStateOf(TremorStats())
        private set

    var level by mutableStateOf(TremorLevel.NONE)
        private set

    val waveform = mutableStateListOf<Float>()

    fun start() {
        if (isRunning || accelerometer == null) return
        magnitudes.clear()
        waveform.clear()
        zeroCrossings = 0
        windowStartTime = System.currentTimeMillis()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    fun reset() {
        stop()
        magnitudes.clear()
        waveform.clear()
        stats = TremorStats()
        level = TremorLevel.NONE
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ax: Float
        val ay: Float
        val az: Float

        if (isLinearSensor) {
            ax = event.values[0]
            ay = event.values[1]
            az = event.values[2]
        } else {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
            ax = event.values[0] - gravity[0]
            ay = event.values[1] - gravity[1]
            az = event.values[2] - gravity[2]
        }

        val magnitude = sqrt(ax * ax + ay * ay + az * az)

        if (magnitudes.size >= windowSize) magnitudes.removeFirst()
        magnitudes.addLast(magnitude)

        if (waveform.size >= 80) waveform.removeAt(0)
        waveform.add(magnitude)

        if (lastSignSample > 0.05f && magnitude <= 0.05f ||
            lastSignSample <= 0.05f && magnitude > 0.05f
        ) {
            zeroCrossings++
        }
        lastSignSample = magnitude

        val now = System.currentTimeMillis()
        val elapsedSec = (now - windowStartTime) / 1000f
        val frequency = if (elapsedSec > 0f) (zeroCrossings / 2f) / elapsedSec else 0f

        var sumSq = 0f
        var peak = 0f
        for (m in magnitudes) {
            sumSq += m * m
            if (m > peak) peak = m
        }
        val rms = if (magnitudes.isNotEmpty()) sqrt(sumSq / magnitudes.size) else 0f

        stats = TremorStats(
            currentMagnitude = magnitude,
            rms = rms,
            peak = peak,
            frequencyHz = frequency,
            sampleCount = magnitudes.size
        )

        level = when {
            rms < 0.15f -> TremorLevel.NONE
            rms < 0.6f -> TremorLevel.LIGHT
            rms < 1.5f -> TremorLevel.MODERATE
            else -> TremorLevel.SEVERE
        }

        if (elapsedSec > 5f) {
            windowStartTime = now
            zeroCrossings = 0
        }

        lastEventTimestamp = event.timestamp
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
