package com.github.michaelbrunn3r.maschinenanalyse

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecordingsListFragment: Fragment() {

    private lateinit var mNavController:NavController
    private lateinit var mToolbar: Toolbar
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recordings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar.setNavigationIcon(R.drawable.back)
        mToolbar.setNavigationOnClickListener {
            mNavController.navigateUp()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RecordingListAdapter(context!!)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context!!)

        mMachineanalysisViewModel = ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        mMachineanalysisViewModel.recordings.observe(this, Observer { recordings ->
            recordings?.let { adapter.setRecordings(recordings) }
        })
    }
}

class RecordingListAdapter internal constructor(context: Context) : RecyclerView.Adapter<RecordingListAdapter.RecordingViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mRecordings = emptyList<Recording>() // Cached Recordings

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = itemView.findViewById(R.id.name)
        val sampleRateView: TextView = itemView.findViewById(R.id.sample_rate)
        val sampleSizeView: TextView = itemView.findViewById(R.id.sample_size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val itemView = mInflater.inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val current = mRecordings[position]
        holder.nameView.text = current.name
        holder.sampleRateView.text = current.audioSampleRate.toString()
        holder.sampleSizeView.text = current.audioFFTSamples.toString()
    }

    override fun getItemCount() = mRecordings.size

    internal fun setRecordings(recordings: List<Recording>) {
        mRecordings = recordings
        notifyDataSetChanged()
    }
}