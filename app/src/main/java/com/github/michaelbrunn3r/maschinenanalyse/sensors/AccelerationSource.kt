package com.github.michaelbrunn3r.maschinenanalyse.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class NormalizedAccelerationMagnitudeSamplesSource(val sensorManager:SensorManager, val numSamples:Int, val constantForce: Float = GRAVITY) {

    private val mFlowable: Flowable<FloatArray>
    var updateFrequency = 0f
    private var mTLastUpdate = System.currentTimeMillis()

    init {
        mFlowable = Flowable.create<FloatArray>({ emitter ->
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            // Create Acceleration listener
            val listener = object: SensorEventListener {
                val buffer = FloatArray(numSamples)
                var numSamplesRead = 0

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null) {
                        when (event.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> {
                                val totalAccel = vec3DMagnitude(event.values[0], event.values[1], event.values[2])
                                val normalizedAccel = max( totalAccel - constantForce, 0.0f)
                                buffer[numSamplesRead] = normalizedAccel

                                numSamplesRead++
                                if(numSamplesRead >= buffer.size) {
                                    // Calculate frequency
                                    val deltaT = System.currentTimeMillis() - mTLastUpdate
                                    mTLastUpdate = System.currentTimeMillis()
                                    updateFrequency = (1000f * numSamples) / deltaT

                                    // Emit new data
                                    val cpy = FloatArray(buffer.size)
                                    System.arraycopy(buffer, 0, cpy, 0, buffer.size)
                                    emitter.onNext(cpy)

                                    // Prepare for next emit
                                    numSamplesRead = 0
                                }
                            }
                        }
                    }
                }
            }

            // Manage listener registration
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
            emitter.setCancellable {
                sensorManager.unregisterListener(listener, accelerometer)
            }
        }, BackpressureStrategy.DROP).subscribeOn(Schedulers.io()).share()
    }

    fun stream(): Flowable<FloatArray> {
        return mFlowable
    }

    companion object {
        const val GRAVITY = 9.81f

        private fun vec3DMagnitude(x:Float, y:Float, z:Float): Float {
            return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
        }
    }
}