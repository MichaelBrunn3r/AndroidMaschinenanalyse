package com.github.michaelbrunn3r.maschinenanalyse.viewmodels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import com.github.michaelbrunn3r.maschinenanalyse.util.formatTime
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RecordingDetailsViewModel : ViewModel() {


    val recording = MutableLiveData<Recording>()
    var dateFormat: DateFormat? = null

    val audioCfg = MutableLiveData("?")
    val accelCfg = MutableLiveData("?")
    val captureDate = MutableLiveData("?")
    val recordingDuration = MutableLiveData("?")


    init {
        recording.observeForever {
            audioCfg.value = "${it.numAudioSamples} @ ${it.audioSampleRate}Hz"
            accelCfg.value = "${it.numAccelSamples} @ ${it.accelSampleRate.toInt()}Hz"

            val cal = Calendar.getInstance()
            cal.timeInMillis = it.captureDate
            captureDate.value = dateFormat?.format(cal.time) ?: "?"

            recordingDuration.value = formatTime(it.duration, TimeUnit.MILLISECONDS)
        }
    }

    fun deleteRecording(dbViewModel: MachineanalysisViewModel) {
        if (recording.value != null) {
            dbViewModel.delete(recording.value!!.copy())
        }
    }

    fun createShareRecordingIntent(recording: Recording): Intent {
        val json = recordingToJSON(recording)

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, json.toString())
            type = "text/json"
        }

        return Intent.createChooser(sendIntent, null)
    }

    private fun recordingToJSON(recording: Recording): JSONObject {
        val r = JSONObject()
        r.put("name", recording.name)
        r.put("captureDate", recording.captureDate)
        r.put("duration", recording.duration)
        r.put("audioSampleRate", recording.audioSampleRate)
        r.put("numAudioSamples", recording.numAudioSamples)
        r.put("accelSampleRate", recording.accelSampleRate)
        r.put("numAccelSamples", recording.numAccelSamples)
        r.put("audioAmplitudesMean", JSONArray(recording.audioAmplitudesMean))
        r.put("accelAmplitudesMean", JSONArray(recording.accelAmplitudesMean))
        return r
    }
}