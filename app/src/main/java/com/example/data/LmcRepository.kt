package com.example.data

import kotlinx.coroutines.flow.Flow

class LmcRepository(private val lmcDao: LmcDao) {

    val allConfigs: Flow<List<LmcConfig>> = lmcDao.getAllConfigs()
    val allPhotos: Flow<List<CapturedPhoto>> = lmcDao.getAllPhotos()

    suspend fun getConfigById(id: Int): LmcConfig? {
        return lmcDao.getConfigById(id)
    }

    suspend fun insertConfig(config: LmcConfig) {
        lmcDao.insertConfig(config)
    }

    suspend fun deleteConfigById(id: Int) {
        lmcDao.deleteConfigById(id)
    }

    suspend fun getPhotoById(id: Int): CapturedPhoto? {
        return lmcDao.getPhotoById(id)
    }

    suspend fun insertPhoto(photo: CapturedPhoto) {
        lmcDao.insertPhoto(photo)
    }

    suspend fun deletePhotoById(id: Int) {
        lmcDao.deletePhotoById(id)
    }
}
