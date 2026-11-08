package com.oxygen.jadwalin.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JadwalDao {
    @Query("SELECT * FROM jadwal ORDER BY waktu_mulai ASC")
    fun getAllJadwal(): Flow<List<JadwalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJadwal(jadwal: JadwalEntity): Long

    @Update
    suspend fun updateJadwal(jadwal: JadwalEntity)

    @Delete
    suspend fun deleteJadwal(jadwal: JadwalEntity)

    @Query("DELETE FROM jadwal")
    suspend fun deleteAllJadwal()
}