package com.github.michaelbrunn3r.maschinenanalyse

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.launch

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "recording_name") val recordingName: String?,
    @ColumnInfo(name = "audio_fft_samples") val audioFFTSamples: Int,
    @ColumnInfo(name = "audio_sample_rate") val audioSampleRate: Int,
    @ColumnInfo(name = "accel_peak_mean") val accelPeakMean: Float,
    @ColumnInfo(name = "audio_mean_fft") val audioMeanFFT: String
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings")
    fun getAll(): LiveData<List<Recording>>

    @Query("SELECT * FROM recordings WHERE recording_name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): Recording

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(recording: Recording)

    @Delete
    fun delete(recording: Recording)
}

@Database(entities = arrayOf(Recording::class), version = 1, exportSchema = false)
abstract class MachineanalysisDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: MachineanalysisDatabase? = null

        fun instance(context: Context): MachineanalysisDatabase {
            if(INSTANCE != null) {
                return INSTANCE!!
            } else {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            MachineanalysisDatabase::class.java,
                            "machineanalysis_database"
                    ).build()
                    return INSTANCE!!
                }
            }
        }
    }
}

class MachineanalysisViewModel(application: Application): AndroidViewModel(application) {
    private var mRecordingsDao = MachineanalysisDatabase.instance(application).recordingDao()
    var recordings = mRecordingsDao.getAll()

    fun insert(recording: Recording) = viewModelScope.launch {
        mRecordingsDao.insert(recording)
    }
}