package com.github.michaelbrunn3r.maschinenanalyse.viewmodels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import org.json.JSONArray
import org.json.JSONObject

class RecordingDetailsViewModel: ViewModel() {

    val recording = MutableLiveData<Recording>()

    fun deleteRecording(dbViewModel: MachineanalysisViewModel) {
        if(recording.value != null) {
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
        r.put("num_fft_audio_samples", recording.numFFTAudioSamples)
        r.put("accel_mean", recording.accelerationMean)
        r.put("duration_ms", recording.duration)
        r.put("capture_date", recording.captureDate)
        r.put("amplitude_means", JSONArray(recording.amplitudeMeans))
        return r
    }
}