package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.app.Dialog
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
import com.github.michaelbrunn3r.maschinenanalyse.sensors.FFT
import com.github.michaelbrunn3r.maschinenanalyse.viewmodels.RecordingDetailsViewModel

class RecordingDetailsFragment : Fragment() {

    private lateinit var mNavController: NavController
    private val mNavArgs: RecordingDetailsFragmentArgs by navArgs()

    private lateinit var mBinding: FragmentRecordingDetailsBinding
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel
    private lateinit var mVM: RecordingDetailsViewModel

    private var mToolbar: Toolbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recording_details, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavController = Navigation.findNavController(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mToolbar = activity?.findViewById(R.id.toolbar)

        mMachineanalysisViewModel = ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        mMachineanalysisViewModel.recordings.observe(this, Observer { recordings ->
            for (r: Recording in recordings) {
                if (r.uid == mNavArgs.recordingId) {
                    mVM.recording.value = r
                }
            }
        })

        mVM = ViewModelProviders.of(this).get(RecordingDetailsViewModel::class.java)
        mVM.apply {
            dateFormat = DateFormat.getLongDateFormat(activity)
            recording.observe(this@RecordingDetailsFragment, Observer {
                mToolbar?.title = it.name

                mBinding.audioSpectrogram.setFrequencyRange(0f, FFT.nyquist(it.audioSampleRate.toFloat()))
                mBinding.audioSpectrogram.update(it.audioAmplitudesMean.toFloatArray())

                mBinding.accelSpectrogram.setFrequencyRange(0f, FFT.nyquist(it.accelSampleRate))
                mBinding.accelSpectrogram.update(it.accelAmplitudesMean.toFloatArray())

                accelCfg.value = getString(R.string.accelerometer_configuration, it.numAccelSamples, it.accelSampleRate)
                audioCfg.value = getString(R.string.audio_configuration, it.numAudioSamples, it.audioSampleRate)
            })
            accelSpectrogramUnfolded.observe(this@RecordingDetailsFragment, Observer {
                if(it) {
                    mBinding.infoWrapper.visibility = View.GONE
                    mBinding.audioWrapper.visibility = View.GONE
                    mBinding.unfoldAccelButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_less, context!!.theme))
                } else {
                    mBinding.infoWrapper.visibility = View.VISIBLE
                    mBinding.audioWrapper.visibility = View.VISIBLE
                    mBinding.unfoldAccelButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_more, context!!.theme))
                }
            })
            audioSpectrogramUnfolded.observe(this@RecordingDetailsFragment, Observer {
                if(it) {
                    mBinding.infoWrapper.visibility = View.GONE
                    mBinding.accelWrapper.visibility = View.GONE
                    mBinding.unfoldAudioButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_less, context!!.theme))
                } else {
                    mBinding.infoWrapper.visibility = View.VISIBLE
                    mBinding.accelWrapper.visibility = View.VISIBLE
                    mBinding.unfoldAudioButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_more, context!!.theme))
                }
            })
        }

        mBinding.viewmodel = mVM
        mBinding.unfoldAccelButton.setOnClickListener {
            mVM.audioSpectrogramUnfolded.value = false
            mVM.accelSpectrogramUnfolded.value = !mVM.accelSpectrogramUnfolded.value!!
        }
        mBinding.unfoldAudioButton.setOnClickListener {
            mVM.accelSpectrogramUnfolded.value = false
            mVM.audioSpectrogramUnfolded.value = !mVM.audioSpectrogramUnfolded.value!!
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_record_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miShare -> {
                if(mVM.recording.value != null) {
                    startActivity(mVM.createShareRecordingIntent(mVM.recording.value!!))
                }
                return true
            }
            R.id.miDelete -> {
                if(mVM.recording.value != null) {
                    showDeleteDialog()
                }
                return true
            }
        }
        return false
    }

    private fun showDeleteDialog() {
        if (fragmentManager == null) return

        DeleteRecordingDialogFragment {
            mVM.deleteRecording(mMachineanalysisViewModel)
            mNavController.navigateUp() // Leave Detail Fragment, as recording doesn't exist anymore
        }.show(fragmentManager!!, "deleteRecording")
    }
}

class DeleteRecordingDialogFragment(val onPositive: (DeleteRecordingDialogFragment) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Build the dialog and set up the button click handlers
            AlertDialog.Builder(it)
                    .setTitle(R.string.title_delete_recording)
                    .setPositiveButton(R.string.delete) { _, _ -> onPositive(this) }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}