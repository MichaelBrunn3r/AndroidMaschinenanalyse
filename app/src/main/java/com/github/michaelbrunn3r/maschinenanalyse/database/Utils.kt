package com.github.michaelbrunn3r.maschinenanalyse.database

import org.json.JSONArray
import org.json.JSONObject

fun Recording.toJson(): String {
    val r = JSONObject()
    r.put("name", name)
    r.put("captureDate", captureDate)
    r.put("duration", duration)
    r.put("audioSampleRate", audioSampleRate)
    r.put("numAudioSamples", numAudioSamples)
    r.put("accelSampleRate", accelSampleRate)
    r.put("numAccelSamples", numAccelSamples)
    r.put("audioAmplitudesMean", JSONArray(audioAmplitudesMean))
    r.put("accelAmplitudesMean", JSONArray(accelAmplitudesMean))
    return r.toString()
}