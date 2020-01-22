package com.github.michaelbrunn3r.maschinenanalyse

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class NormalizedAccelerationLiveData : LiveData<Float>(0f), SensorEventListener {

    var sensorManager: SensorManager? = null
    var accelerometer: Sensor? = null
    private var mIsSampling: Boolean = false

    override fun onActive() {
        if (mIsSampling) startSampling()
    }

    override fun onInactive() {
        if (mIsSampling) stopSampling()
    }

    fun setSamplingState(isSampling: Boolean) {
        mIsSampling = isSampling
        if (isSampling) startSampling()
        else stopSampling()
    }

    private fun startSampling() {
        sensorManager?.unregisterListener(this, accelerometer)
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopSampling() {
        sensorManager?.unregisterListener(this, accelerometer)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        println("sampling accelerometer")
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val totalAcceleration = vec3DLength(event.values[0], event.values[1], event.values[2])
                    val normalizedAcceleration = max( totalAcceleration - GRAVITY, 0.0f)
                    setValue(normalizedAcceleration)
                }
            }
        }
    }

    private fun vec3DLength(x:Float, y:Float, z:Float): Float {
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    companion object {
        const val GRAVITY = 9.81f
    }
}