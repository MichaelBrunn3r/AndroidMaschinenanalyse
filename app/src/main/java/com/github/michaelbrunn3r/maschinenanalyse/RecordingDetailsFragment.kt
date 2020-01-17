package com.github.michaelbrunn3r.maschinenanalyse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs

class RecordingDetailsFragment : Fragment() {

    private lateinit var mNavController: NavController
    private val mNavArgs:RecordingDetailsFragmentArgs by navArgs()
    private lateinit var mToolbar: Toolbar
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel

    private lateinit var mNameView: TextView
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

        mNameView = view.findViewById(R.id.name)
        mSampleRateView = view.findViewById(R.id.sample_rate)
        mSampleSizeView = view.findViewById(R.id.sample_size)
        mMeanAccelView = view.findViewById(R.id.mean_acceleration)
        mChart = view.findViewById(R.id.chartAudio)

        mMachineanalysisViewModel = ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        mMachineanalysisViewModel.recordings.observe(this, Observer {recordings ->
            println("There are ${recordings.size} recordings and I show index ${mNavArgs.recordingId}")
            for(r:Recording in recordings) {
                if(r.uid == mNavArgs.recordingId) {
                    populate(r)
                }
            }
        })
    }

    fun populate(recording:Recording) {
        mNameView.setText(recording.name)
        mSampleRateView.text = recording.audioSampleRate.toString()
        mSampleSizeView.text = recording.audioFFTSamples.toString()
        mMeanAccelView.text = recording.accelPeakMean.toBigDecimal().toString()


        val chartData = recording.audioMeanFFT.split(';').map{it.toFloat()}.toFloatArray()
        mChart.update(chartData) { index -> fftFrequenzyBin(index, recording.audioSampleRate, recording.audioFFTSamples)}
    }
}