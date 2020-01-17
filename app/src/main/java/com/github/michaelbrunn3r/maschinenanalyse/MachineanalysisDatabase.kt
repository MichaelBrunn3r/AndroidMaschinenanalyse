package com.github.michaelbrunn3r.maschinenanalyse

import android.app.Application
import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @NonNull @ColumnInfo(name = "name") val name: String?,
    @NonNull @ColumnInfo(name = "audio_sample_rate_hz") val audioSampleRate: Int, // Sample rate in Hz
    @NonNull @ColumnInfo(name = "num_fft_audio_samples") val numFFTAudioSamples: Int, // Number of Samples per FFT Frame
    @NonNull @ColumnInfo(name = "accel_mean") val accelerationMean: Float, // Mean over acceleration intensity peaks
    @NonNull @ColumnInfo(name = "amplitude_means") val amplitudeMeans: String // Mean Amplitude per Frequency
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings")
    fun getRecordings(): LiveData<List<Recording>>

    @Query("SELECT * FROM recordings WHERE name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): Recording

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()
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
    private var mRecordingsDao:RecordingDao
    lateinit var recordings:LiveData<List<Recording>>

    init {
        mRecordingsDao = MachineanalysisDatabase.instance(application).recordingDao()
        recordings = mRecordingsDao.getRecordings()
    }

    fun insert(recording: Recording) = viewModelScope.launch {
        mRecordingsDao.insert(recording)
    }

    fun delete(recording: Recording) = viewModelScope.launch {
        mRecordingsDao.delete(recording)
    }
}