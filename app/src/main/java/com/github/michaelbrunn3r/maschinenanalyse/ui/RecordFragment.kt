package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
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
import com.github.michaelbrunn3r.maschinenanalyse.FFT
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentRecordBinding
import com.github.michaelbrunn3r.maschinenanalyse.viewmodels.RecordViewModel

class RecordFragment : Fragment() {

    private lateinit var mBinding: FragmentRecordBinding
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel
    private lateinit var mRecordViewModel: RecordViewModel

    private var mMIRecord: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_record, container, false)
        return mBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mMachineanalysisViewModel = activity?.run {
            ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        mRecordViewModel = ViewModelProviders.of(this).get(RecordViewModel::class.java)
        mRecordViewModel.apply {
            sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            audioCfg.observe(this@RecordFragment, Observer { cfg ->
                mBinding.spectrogram.setFrequencyRange(0f, FFT.nyquist(cfg.sampleRate.toFloat()))
            })
            recordedFrequencies.observe(this@RecordFragment, Observer {
                mBinding.spectrogram.update(it)
            })
            recordedAccelMean.observe(this@RecordFragment, Observer {
                mBinding.meanAccel.text = it.toString()
            })
            isRecording.observe(this@RecordFragment, Observer {
                if (it && requestAudioPermissions()) setRecordBtnState(true)
                else setRecordBtnState(false)
            })
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mRecordViewModel.onPreferences(preferences)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_record, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mMIRecord = menu.findItem(R.id.miRecord)
        setRecordBtnState(mRecordViewModel.isRecording.value ?: false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miRecord -> {
                mRecordViewModel.isRecording.value = true
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
        if (mRecordViewModel.recordedFrequencies.value == null || fragmentManager == null) return

        SaveRecordingAsDialogFragment { dialog ->
            mRecordViewModel.saveRecording(dialog.recordingName, mMachineanalysisViewModel)
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