package com.github.michaelbrunn3r.maschinenanalyse

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentRecordingDetailsBinding
import org.json.JSONArray
import org.json.JSONObject

class RecordingDetailsFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var mBinding: FragmentRecordingDetailsBinding

    private lateinit var mNavController: NavController
    private val mNavArgs:RecordingDetailsFragmentArgs by navArgs()
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel
    private var mRecording: Recording? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recording_details, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)


        mBinding.apply {
            toolbar as Toolbar
            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                mNavController.navigateUp()
            }
            toolbar.inflateMenu(R.menu.menu_record_details)
            toolbar.setOnMenuItemClickListener(this@RecordingDetailsFragment)
        }

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
                showDeleteDialog(mRecording!!)
                mRecording = null
            }
        }
        return false
    }

    fun showRecordingData(recording:Recording) {
        mBinding.apply {
            toolbar as Toolbar
            toolbar.title = recording.name

            sampleRate.text = recording.audioSampleRate.toString()
            sampleSize.text = recording.numFFTAudioSamples.toString()
            meanAcceleration.text = recording.accelerationMean.toBigDecimal().toString()

            mBinding.apply {
                audioSpectrogram.setFrequencyRange(0f, (recording.audioSampleRate/2).toFloat())
                audioSpectrogram.update(recording.amplitudeMeans.toFloatArray()) {
                    index -> fftFrequenzyBin(index, recording.audioSampleRate, recording.numFFTAudioSamples)
                }
            }

        }
    }

    fun recordingToJSON(recording: Recording): JSONObject {
        val r = JSONObject()
        r.put("name", recording.name)
        r.put("sampleRate", recording.audioSampleRate)
        r.put("samples", recording.numFFTAudioSamples)
        r.put("accelMean", recording.accelerationMean)
        r.put("meanFFT", JSONArray(recording.amplitudeMeans))
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
        if(fragmentManager == null) return

        DeleteRecordingDialogFragment {
            mMachineanalysisViewModel.delete(recording.copy())
            mRecording = null // Make sure recording is invalidated
            mNavController.navigateUp() // Leave Detail Fragment, as recording doesn't exist anymore
        }.show(fragmentManager!!, "deleteRecording")
    }
}

class DeleteRecordingDialogFragment(val onPositive:(DeleteRecordingDialogFragment) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Build the dialog and set up the button click handlers
            AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_delete_recording)
                    .setPositiveButton(R.string.delete) {_,_ -> onPositive(this)}
                    .setNegativeButton(R.string.cancel) {_,_ -> }
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}