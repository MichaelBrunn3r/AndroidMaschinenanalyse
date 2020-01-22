package com.github.michaelbrunn3r.maschinenanalyse.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.michaelbrunn3r.maschinenanalyse.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mNavCtrl: NavController
    private val mToplevelDestinations = setOf(R.id.recordingListFragment, R.id.recordFragment, R.id.monitorFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mNavCtrl = findNavController(R.id.nav_host_fragment)
        mNavCtrl.addOnDestinationChangedListener { _, _, _->
            mBinding.toolbar.menu.clear() // Allows for smoother Toolbar animations
            mBinding.toolbar.visibility = View.VISIBLE // Reset Toolbar visibility if it was toggled
        }

        // Configure Toolbar
        setSupportActionBar(mBinding.toolbar)
        val appBarCfg = AppBarConfiguration.Builder(mToplevelDestinations)
                .setDrawerLayout(mBinding.drawerLayout)
                .build()
        mBinding.toolbar.setupWithNavController(mNavCtrl, appBarCfg)

        // Config nav drawer
        mBinding.navView.setNavigationItemSelectedListener {
            NavigationUI.onNavDestinationSelected(it, mNavCtrl)
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }
    }
}