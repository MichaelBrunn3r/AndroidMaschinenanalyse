package com.github.michaelbrunn3r.maschinenanalyse.viewmodels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import com.github.michaelbrunn3r.maschinenanalyse.database.toJson
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
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, recording.toJson())
            type = "text/json"
        }

        return Intent.createChooser(sendIntent, null)
    }
}