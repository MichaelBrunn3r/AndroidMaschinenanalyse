package com.github.michaelbrunn3r.maschinenanalyse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.github.michaelbrunn3r.maschinenanalyse.databinding.ActivityMainBinding
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mNavCtrl: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mNavCtrl = findNavController(R.id.nav_host_fragment)
        mNavCtrl.addOnDestinationChangedListener { controller, destination, arguments ->
            mBinding.toolbar.menu.clear() // Allows for smoother Toolbar animations
            mBinding.toolbar.visibility = View.VISIBLE // Reset Toolbar visibility if it was toggled
        }

        // Configure Toolbar
        setSupportActionBar(mBinding.toolbar)
        val appBarCfg = AppBarConfiguration(mNavCtrl.graph, mBinding.drawerLayout)
        AppBarConfiguration.Builder()
        mBinding.toolbar.setupWithNavController(mNavCtrl, appBarCfg)

        // Config nav drawer
        mBinding.navView.setNavigationItemSelectedListener {
            NavigationUI.onNavDestinationSelected(it, mNavCtrl)
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }
    }
}

fun Toolbar.toggle() {
    visibility = when(visibility) {
        View.VISIBLE -> View.GONE
        else -> View.VISIBLE
    }
}

class MainFragment : Fragment(), View.OnClickListener {

    private lateinit var mBinding: FragmentMainBinding
    private lateinit var mNavController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavController = Navigation.findNavController(view)

        mBinding.apply {
            btnNavToMonitor.setOnClickListener(this@MainFragment)
            btnNavToRecording.setOnClickListener(this@MainFragment)
            btnNavToRecordingsList.setOnClickListener(this@MainFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miSettings -> {
                mNavController.navigate(R.id.action_mainFragment_to_settingsFragment)
                return true
            }
        }
        return false
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_nav_to_recording -> mNavController.navigate(R.id.action_mainFragment_to_recordingFragment)
            R.id.btn_nav_to_monitor -> mNavController.navigate(R.id.action_mainFragment_to_monitorFragment)
            R.id.btn_nav_to_recordings_list -> mNavController.navigate(R.id.action_mainFragment_to_recordingsListFragment)
        }
    }
}