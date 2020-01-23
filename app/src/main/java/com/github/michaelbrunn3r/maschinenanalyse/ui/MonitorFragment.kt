package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.github.michaelbrunn3r.maschinenanalyse.*
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentMonitorBinding
import com.github.michaelbrunn3r.maschinenanalyse.sensors.AccelerationRecordingConfiguration
import com.github.michaelbrunn3r.maschinenanalyse.sensors.FFT
import com.github.michaelbrunn3r.maschinenanalyse.viewmodels.MonitorViewModel


class MonitorFragment : Fragment() {

    private lateinit var mBinding: FragmentMonitorBinding
    private lateinit var mVM: MonitorViewModel

    private var mToolbar: Toolbar? = null
    private var mMIStartStop: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_monitor, container, false)
        return mBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mToolbar = activity?.findViewById(R.id.toolbar)
        mBinding.accelSpectrogram.axisLeft.axisMaximum = 50f

        mVM = ViewModelProviders.of(this).get(MonitorViewModel::class.java)
        mVM.apply {
            audioCfg.observe(this@MonitorFragment, Observer {cfg ->
                mBinding.audioSpectrogram.setFrequencyRange(0f, FFT.nyquist(cfg.sampleRate.toFloat()))
            })
            accelFrequency.observeForever {
                mBinding.accelSpectrogram.setFrequencyRange(0f, FFT.nyquist(it))
            }
            audioFrequenciesSource.observe(this@MonitorFragment, Observer {frequencies ->
                mBinding.audioSpectrogram.update(frequencies)
            })
            accelSource.observe(this@MonitorFragment, Observer {
                mBinding.accelSpectrogram.update(it)
            })
            isMonitoring.observe(this@MonitorFragment, Observer {
                setMonitoringBtnState(it)
            })
            isToolbarHidden.observe(this@MonitorFragment, Observer {
                mToolbar?.apply {
                    visibility = if(it) View.GONE
                    else View.VISIBLE
                }
            })
        }

        mBinding.touchOverlay.setOnShortClickListener {
            mVM.isToolbarHidden.apply {
                value = !(value == null || value == true)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mVM.accelCfg.value = AccelerationRecordingConfiguration(sensorManager, 512, 9.81f)

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mVM.onPreferences(preferences)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_monitor, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mMIStartStop = menu.findItem(R.id.miStartStop)
        setMonitoringBtnState(mVM.isMonitoring.value ?: false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miStartStop -> {
                mVM.isMonitoring.apply {
                    value = !(!requestAudioPermissions() || value == null || value == true)
                }
            }
        }
        return false
    }

    private fun setMonitoringBtnState(isSampling: Boolean) {
        mMIStartStop?.apply {
            icon = if(isSampling) resources.getDrawable(R.drawable.pause_btn, activity!!.theme)
            else resources.getDrawable(R.drawable.play_btn, activity!!.theme)
        }
    }

    private fun requestAudioPermissions():Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity!!.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1234)
            println("No Audio Permission granted")
            return false
        }
        return true
    }
}