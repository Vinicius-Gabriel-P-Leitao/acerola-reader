package br.acerola.comic.local.dao.archive

import androidx.room.Dao
import androidx.room.Query
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.entity.archive.VolumeArchive
import kotlinx.coroutines.flow.Flow

@Dao
interface VolumeArchiveDao : BaseDao<VolumeArchive> {
    @Query("SELECT * FROM volume_archive WHERE comic_directory_fk = :folderId")
    fun getVolumesByDirectoryId(folderId: Long): Flow<List<VolumeArchive>>

    @Query("SELECT * FROM volume_archive WHERE comic_directory_fk = :folderId")
    suspend fun getVolumesListByDirectoryId(folderId: Long): List<VolumeArchive>

    @Query("SELECT * FROM volume_archive WHERE id = :volumeId")
    suspend fun getVolumeById(volumeId: Long): VolumeArchive?

    @Query("DELETE FROM volume_archive WHERE comic_directory_fk = :folderId")
    suspend fun deleteByDirectoryId(folderId: Long)
}
