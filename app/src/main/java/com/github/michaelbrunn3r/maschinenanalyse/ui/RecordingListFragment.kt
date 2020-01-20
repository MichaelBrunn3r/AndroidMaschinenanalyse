package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import java.util.*

class RecordingsListFragment : Fragment() {

    private lateinit var mNavController: NavController
    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recording_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RecordingListAdapter(context!!, object : RecordingListAdapter.RecordingClickedListener {
            override fun onClicked(idx: Int) {
                val recording_id = mMachineanalysisViewModel.recordings.value!![idx].uid
                val action = RecordingsListFragmentDirections.actionRecordingsListFragmentToRecordingDetailsFragment(recording_id)
                mNavController.navigate(action)
            }
        })
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        mMachineanalysisViewModel = ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        mMachineanalysisViewModel.recordings.observe(this, Observer { recordings ->
            recordings?.let { adapter.setRecordings(recordings) }
        })
    }
}

class RecordingListAdapter internal constructor(val context: Context, val recordingListener: RecordingClickedListener) : RecyclerView.Adapter<RecordingListAdapter.RecordingViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mRecordings = emptyList<Recording>() // Cached Recordings

    inner class RecordingViewHolder(itemView: View, l: RecordingClickedListener) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = itemView.findViewById(R.id.name)
        val sampleRateView: TextView = itemView.findViewById(R.id.sample_rate)
        val sampleSizeView: TextView = itemView.findViewById(R.id.sample_size)
        val captureDateView: TextView = itemView.findViewById(R.id.capture_date)

        init {
            itemView.setOnClickListener {
                l.onClicked(adapterPosition)
            }
        }

        fun bind(recording: Recording) {
            nameView.text = recording.name
            sampleRateView.text = recording.audioSampleRate.toString()
            sampleSizeView.text = recording.numFFTAudioSamples.toString()

            val cal = Calendar.getInstance()
            cal.timeInMillis = recording.captureDate
            val f = DateFormat.getDateFormat(context)
            captureDateView.text = f.format(cal.time)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val itemView: View = mInflater.inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(itemView, recordingListener)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(mRecordings[position])
    }

    override fun getItemCount() = mRecordings.size

    internal fun setRecordings(recordings: List<Recording>) {
        mRecordings = recordings
        notifyDataSetChanged()
    }

    interface RecordingClickedListener {
        fun onClicked(idx: Int)
    }
}