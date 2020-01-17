package com.github.michaelbrunn3r.maschinenanalyse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import org.json.JSONArray
import org.json.JSONObject

class RecordingDetailsFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var mNavController: NavController
    private val mNavArgs:RecordingDetailsFragmentArgs by navArgs()
    private lateinit var mToolbar: Toolbar
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel
    private var mRecording: Recording? = null

    private lateinit var mSampleRateView: TextView
    private lateinit var mSampleSizeView: TextView
    private lateinit var mMeanAccelView: TextView
    private lateinit var mChart: SpectrogramView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recording_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar.setNavigationIcon(R.drawable.back)
        mToolbar.setNavigationOnClickListener {
            mNavController.navigateUp()
        }
        mToolbar.inflateMenu(R.menu.menu_record_details)
        mToolbar.setOnMenuItemClickListener(this)

        mSampleRateView = view.findViewById(R.id.sample_rate)
        mSampleSizeView = view.findViewById(R.id.sample_size)
        mMeanAccelView = view.findViewById(R.id.mean_acceleration)
        mChart = view.findViewById(R.id.chartAudio)

        mMachineanalysisViewModel = ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        mMachineanalysisViewModel.recordings.observe(this, Observer {recordings ->
            println("There are ${recordings.size} recordings and I show index ${mNavArgs.recordingId}")
            for(r:Recording in recordings) {
                if(r.uid == mNavArgs.recordingId) {
                    mRecording = r
                    showRecordingData(r)
                }
            }
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.miShare -> {
                if(mRecording != null) {
                    shareRecording(mRecording!!)
                }
                return true
            }
            R.id.miDelete -> {
                deleteRecording(mRecording!!)
                mRecording = null
            }
        }
        return false
    }

    fun showRecordingData(recording:Recording) {
        mToolbar.title = recording.name
        mSampleRateView.text = recording.audioSampleRate.toString()
        mSampleSizeView.text = recording.audioFFTSamples.toString()
        mMeanAccelView.text = recording.accelPeakMean.toBigDecimal().toString()

        mChart.setFrequencyRange(0f, (recording.audioSampleRate/2).toFloat())

        val chartData = recording.audioMeanFFT.split(';').map{it.toFloat()}.toFloatArray()
        mChart.update(chartData) { index -> fftFrequenzyBin(index, recording.audioSampleRate, recording.audioFFTSamples)}
    }

    fun convertRecordingToJSON(recording: Recording): JSONObject {
        val r = JSONObject()
        r.put("name", recording.name)
        r.put("sampleRate", recording.audioSampleRate)
        r.put("samples", recording.audioFFTSamples)
        r.put("accelMean", recording.accelPeakMean)

        val frequencies = JSONArray()
        for(f:Float in recording.audioMeanFFT.split(';').map{it.toFloat()}) {
            frequencies.put(f)
        }

        r.put("meanFFT", frequencies)
        return r
    }

    fun shareRecording(recording: Recording) {
        val json = convertRecordingToJSON(recording)

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, json.toString())
            type = "text/json"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    fun deleteRecording(recording: Recording) {
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(R.string.popup_delete_recording_title)

        builder.setPositiveButton(R.string.popup_delete_recording_positive) { dialog, which ->
            mMachineanalysisViewModel.delete(recording.copy())
            mRecording = null

            mNavController.navigateUp()
        }
        builder.setNegativeButton(R.string.popup_default_negative) { dialog, which ->  }

        builder.show()
    }
}