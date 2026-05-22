package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LmcDao {

    // --- Configurations (XML Profiles) ---
    @Query("SELECT * FROM lmc_configs ORDER BY isSystemPreset DESC, timestamp DESC")
    fun getAllConfigs(): Flow<List<LmcConfig>>

    @Query("SELECT * FROM lmc_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: Int): LmcConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: LmcConfig)

    @Delete
    suspend fun deleteConfig(config: LmcConfig)

    @Query("DELETE FROM lmc_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Int)


    // --- Captured Photos (Gallery) ---
    @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<CapturedPhoto>>

    @Query("SELECT * FROM captured_photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Int): CapturedPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: CapturedPhoto)

    @Delete
    suspend fun deletePhoto(photo: CapturedPhoto)

    @Query("DELETE FROM captured_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Int)
}
