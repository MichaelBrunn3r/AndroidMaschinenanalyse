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

class MainFragment : Fragment(), View.OnClickListener, Toolbar.OnMenuItemClickListener {

    private lateinit var mNavController:NavController
    private lateinit var mToolbar: Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavController = Navigation.findNavController(view)

        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar.setTitle(R.string.app_name)
        mToolbar.inflateMenu(R.menu.menu_main)
        mToolbar.setOnMenuItemClickListener(this)

        view.findViewById<Button>(R.id.btn_nav_to_recording).setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_nav_to_monitor).setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_nav_to_recordings_list).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.btn_nav_to_recording -> mNavController!!.navigate(R.id.action_mainFragment_to_recordingFragment)
            R.id.btn_nav_to_monitor -> mNavController!!.navigate(R.id.action_mainFragment_to_monitorFragment)
            R.id.btn_nav_to_recordings_list -> mNavController!!.navigate(R.id.action_mainFragment_to_recordingsListFragment)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.miSettings -> {
                mNavController.navigate(R.id.action_mainFragment_to_settingsFragment)
                return true
            }
        }
        return false
    }
}