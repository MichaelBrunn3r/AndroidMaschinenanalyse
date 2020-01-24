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

    val sampleRate = MutableLiveData("?")
    val numSamples = MutableLiveData("?")
    val meanAcceleration = MutableLiveData("?")
    val captureDate = MutableLiveData("?")
    val recordingDuration = MutableLiveData("?")


    init {
        recording.observeForever {
            sampleRate.value = it.audioSampleRate.toString()
            numSamples.value = it.numAudioSamples.toString()
            meanAcceleration.value = it.accelerationMean.toString()

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
        r.put("audio_sample_rate_hz", recording.audioSampleRate)
        r.put("num_fft_audio_samples", recording.numAudioSamples)
        r.put("accel_mean", recording.accelerationMean)
        r.put("duration_ms", recording.duration)
        r.put("capture_date", recording.captureDate)
        r.put("amplitude_means", JSONArray(recording.amplitudeMeans))
        return r
    }
}