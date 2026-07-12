package com.lumacam.feature.ai.vision

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Accelerometer-backed [TiltProvider] (PRD: sensor-based horizon is lighter than
 * image-based detection). Keeps the latest smoothed gravity vector and derives
 * the roll angle via [TiltMath]; requires no runtime permission.
 */
class SensorTiltProvider(context: Context) : TiltProvider, SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    private var gx = 0f

    @Volatile
    private var gy = -SensorManager.GRAVITY_EARTH

    private var registered = false

    /** Begin listening. Safe to call repeatedly; a no-op if already active. */
    fun start() {
        val manager = sensorManager ?: return
        val sensor = accelerometer ?: return
        if (!registered) {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            registered = true
        }
    }

    /** Stop listening and release the sensor. */
    fun stop() {
        if (registered) {
            sensorManager?.unregisterListener(this)
            registered = false
        }
    }

    override fun currentTiltDegrees(): Float = TiltMath.rollDegrees(gx, gy)

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        // Low-pass filter to damp jitter.
        gx = ALPHA * gx + (1 - ALPHA) * event.values[0]
        gy = ALPHA * gy + (1 - ALPHA) * event.values[1]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val ALPHA = 0.8f
    }
}
