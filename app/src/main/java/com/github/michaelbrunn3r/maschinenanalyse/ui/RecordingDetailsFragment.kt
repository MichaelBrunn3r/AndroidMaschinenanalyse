package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.github.michaelbrunn3r.maschinenanalyse.*
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentRecordingDetailsBinding
import kotlinx.android.synthetic.main.fragment_recording_details.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class RecordingDetailsFragment : Fragment() {

    private lateinit var mNavController: NavController
    private val mNavArgs: RecordingDetailsFragmentArgs by navArgs()

    private lateinit var mBinding: FragmentRecordingDetailsBinding
    private var mToolbar: Toolbar? = null

    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel
    private var mRecording: Recording? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recording_details, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mMachineanalysisViewModel = ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        mMachineanalysisViewModel.recordings.observe(this, Observer { recordings ->
            println("There are ${recordings.size} recordings and I show index ${mNavArgs.recordingId}")
            for (r: Recording in recordings) {
                if (r.uid == mNavArgs.recordingId) {
                    mRecording = r
                    showRecordingData(r)
                }
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mToolbar = activity?.findViewById(R.id.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_record_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miShare -> {
                if (mRecording != null) {
                    shareRecording(mRecording!!)
                }
                return true
            }
            R.id.miDelete -> {
                showDeleteDialog(mRecording!!)
                mRecording = null
            }
        }
        return false
    }

    fun showRecordingData(recording: Recording) {
        mToolbar?.title = recording.name

        mBinding.apply {
            sampleRate.text = recording.audioSampleRate.toString()
            sampleSize.text = recording.numFFTAudioSamples.toString()
            meanAcceleration.text = recording.accelerationMean.toString()

            mBinding.apply {
                audioSpectrogram.setFrequencyRange(0f, (recording.audioSampleRate / 2).toFloat())
                audioSpectrogram.update(recording.amplitudeMeans.toFloatArray()) { index ->
                    fftFrequenzyBin(index, recording.audioSampleRate, recording.numFFTAudioSamples)
                }
            }

            val cal = Calendar.getInstance()
            cal.timeInMillis = recording.captureDate
            val f = DateFormat.getDateFormat(activity)
            captureDate.text = f.format(cal.time)

            recording_duration.text = String.format(
                        "%02d s %02d ms",
                        TimeUnit.MILLISECONDS.toSeconds(recording.duration),
                        recording.duration - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(recording.duration))
                    )
        }
    }

    fun recordingToJSON(recording: Recording): JSONObject {
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

    fun shareRecording(recording: Recording) {
        val json = recordingToJSON(recording)

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, json.toString())
            type = "text/json"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun showDeleteDialog(recording: Recording) {
        if (fragmentManager == null) return

        DeleteRecordingDialogFragment {
            mMachineanalysisViewModel.delete(recording.copy())
            mRecording = null // Make sure recording is invalidated
            mNavController.navigateUp() // Leave Detail Fragment, as recording doesn't exist anymore
        }.show(fragmentManager!!, "deleteRecording")
    }
}

class DeleteRecordingDialogFragment(val onPositive: (DeleteRecordingDialogFragment) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Build the dialog and set up the button click handlers
            AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_delete_recording)
                    .setPositiveButton(R.string.delete) { _, _ -> onPositive(this) }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}