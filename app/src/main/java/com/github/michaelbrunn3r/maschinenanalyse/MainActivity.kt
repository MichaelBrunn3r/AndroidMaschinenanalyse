package com.github.michaelbrunn3r.maschinenanalyse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

class MainFragment : Fragment(), View.OnClickListener {

    private lateinit var navController:NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        view.findViewById<Button>(R.id.btn_nav_to_spectrogram).setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_nav_to_recording).setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_nav_to_settings).setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_nav_to_monitor).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.btn_nav_to_recording -> navController!!.navigate(R.id.action_mainFragment_to_recordingFragment)
            R.id.btn_nav_to_settings -> navController!!.navigate(R.id.action_mainFragment_to_settingsFragment)
            R.id.btn_nav_to_monitor -> navController!!.navigate(R.id.action_mainFragment_to_monitorFragment)
        }
    }
}