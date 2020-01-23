package com.github.michaelbrunn3r.maschinenanalyse.sensors

import android.hardware.SensorManager

data class AccelerationRecordingConfiguration(val sensorManager: SensorManager, val numSamples:Int, val constantForce:Float)

data class AudioRecordingConfiguration(val sampleRate: Int, val numSamples: Int, val source: Int, val channelCfg: Int, val format: Int)