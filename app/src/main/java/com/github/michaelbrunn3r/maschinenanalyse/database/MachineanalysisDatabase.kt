package com.github.michaelbrunn3r.maschinenanalyse.database

import android.app.Application
import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.launch

@Entity(tableName = "recordings")
data class Recording(
        @PrimaryKey(autoGenerate = true) val uid: Int,
        @NonNull @ColumnInfo(name = "name") val name: String?,
        @NonNull @ColumnInfo(name = "audio_sample_rate") val audioSampleRate: Int, // Sample rate in Hz
        @NonNull @ColumnInfo(name = "num_audio_samples") val numAudioSamples: Int, // Number of audio samples per FFT
        @NonNull @ColumnInfo(name = "mean_audio_spectrogram") val audioAmplitudesMean: List<Float>, // Audio spectrogram
        @NonNull @ColumnInfo(name = "accel_sample_rate") val accelSampleRate: Float,
        @NonNull @ColumnInfo(name = "num_accel_samples") val numAccelSamples: Int, // Number of accelerometer samples per FFT
        @NonNull @ColumnInfo(name = "mean_accel_spectrogram") val accelAmplitudesMean: List<Float>, // Accelerometer Spectrogram
        @NonNull @ColumnInfo(name = "duration") val duration: Long, // Recording duration
        @NonNull @ColumnInfo(name = "capture_date") val captureDate: Long // When was the recording captured
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
@TypeConverters(Converters::class)
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
    private var mRecordingsDao: RecordingDao
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