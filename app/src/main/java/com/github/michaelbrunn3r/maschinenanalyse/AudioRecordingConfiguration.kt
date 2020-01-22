package com.github.michaelbrunn3r.maschinenanalyse

data class AudioRecordingConfiguration(val sampleRate: Int, val numSamples: Int, val source: Int, val channelCfg: Int, val format: Int)