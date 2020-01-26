package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentRecordBinding
import com.github.michaelbrunn3r.maschinenanalyse.sensors.AccelerationRecordingConfiguration
import com.github.michaelbrunn3r.maschinenanalyse.sensors.FFT
import com.github.michaelbrunn3r.maschinenanalyse.viewmodels.RecordViewModel

class RecordFragment : Fragment() {

    private lateinit var mBinding: FragmentRecordBinding
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel
    private lateinit var mVM: RecordViewModel

    private var mMIRecord: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_record, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mMachineanalysisViewModel = activity?.run {
            ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        mVM = ViewModelProviders.of(this).get(RecordViewModel::class.java)
        mVM.apply {
            audioCfg.observe(this@RecordFragment, Observer { cfg ->
                mBinding.audioSpectrogram.setFrequencyRange(0f, FFT.nyquist(cfg.sampleRate.toFloat()))
            })
            accelFrequency.observeForever {
                mBinding.accelSpectrogram.setFrequencyRange(0f, FFT.nyquist(it))
            }
            recordedAudioFrequencies.observe(this@RecordFragment, Observer {
                mBinding.audioSpectrogram.update(it)
            })
            recordedAccelFrequencies.observe(this@RecordFragment, Observer {
                mBinding.accelSpectrogram.update(it)
            })
            isRecording.observe(this@RecordFragment, Observer {
                if (it) setRecordBtnState(true)
                else setRecordBtnState(false)
            })
            accelSpectrogramUnfolded.observe(this@RecordFragment, Observer {
                if(it) {
                    mBinding.audioWrapper.visibility = View.GONE
                    mBinding.unfoldAccelButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_less, context!!.theme))
                } else {
                    mBinding.audioWrapper.visibility = View.VISIBLE
                    mBinding.unfoldAccelButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_more, context!!.theme))
                }
            })
            audioSpectrogramUnfolded.observe(this@RecordFragment, Observer {
                if(it) {
                    mBinding.accelWrapper.visibility = View.GONE
                    mBinding.unfoldAudioButton.setImageDrawable(resources.getDrawable(R.drawable.unfold_less, context!!.theme))
                } else {
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

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mVM.onPreferences(preferences)

        val sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val numAccelSamples = preferences.getString("numAccelSamples", Settings.DEFAULT_NUM_ACCEL_SAMPLES.toString())!!.toInt()
        mVM.accelCfg.value = AccelerationRecordingConfiguration(sensorManager, numAccelSamples, 9.81f)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_record, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mMIRecord = menu.findItem(R.id.miRecord)
        setRecordBtnState(mVM.isRecording.value ?: false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miRecord -> {
                mVM.isRecording.value = requestAudioPermissions()
                return true
            }
            R.id.miSaveRecording -> {
                saveRecording()
                return true
            }
        }
        return false
    }

    private fun setRecordBtnState(isSampling: Boolean) {
        if (isSampling) mMIRecord?.icon = resources.getDrawable(R.drawable.stop_recording, activity!!.theme)
        else mMIRecord?.icon = resources.getDrawable(R.drawable.record, activity!!.theme)
    }

    private fun saveRecording() {
        if (mVM.recordedAudioFrequencies.value == null || fragmentManager == null) return

        SaveRecordingAsDialogFragment { dialog ->
            mVM.saveRecording(dialog.recordingName, mMachineanalysisViewModel)
        }.show(fragmentManager!!, "saveRecordingAs")
    }

    private fun requestAudioPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity!!.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1234)
            println("No Audio Permission granted")
            return false
        }
        return true
    }
}

class SaveRecordingAsDialogFragment(val onPositive: (SaveRecordingAsDialogFragment) -> Unit) : DialogFragment() {
    private lateinit var mInput: EditText
    var recordingName = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            mInput = EditText(it)
            mInput.inputType = InputType.TYPE_CLASS_TEXT

            // Build the dialog and set up the button click handlers
            AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_save_recording_as)
                    .setPositiveButton(R.string.save) { _, _ ->
                        recordingName = mInput.text.toString()
                        onPositive(this)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setView(mInput)
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}