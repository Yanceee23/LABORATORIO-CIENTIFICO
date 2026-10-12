package com.laboratoriofisico.app.backend.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laboratoriofisico.app.backend.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE id = :id")
    fun observeById(id: Long): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getById(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(usuario: UsuarioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(usuario: UsuarioEntity): Long

    @Query("UPDATE usuarios SET nombre = :nombre, email = :email, rol = :rol WHERE id = :id")
    suspend fun updateProfile(id: Long, nombre: String, email: String, rol: String)

    @Query("UPDATE usuarios SET passwordHash = :passwordHash WHERE id = :id")
    suspend fun updatePasswordHash(id: Long, passwordHash: String)

    @Query("SELECT * FROM usuarios ORDER BY id ASC LIMIT 1")
    suspend fun findFirstUsuario(): UsuarioEntity?

    @Query("SELECT * FROM usuarios ORDER BY id ASC")
    suspend fun getAll(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE email = 'demo@laboratoriofisico.app' LIMIT 1")
    suspend fun findDemoUsuario(): UsuarioEntity?

    @Delete
    suspend fun delete(usuario: UsuarioEntity)
}
