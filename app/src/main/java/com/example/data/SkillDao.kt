package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for the [Skill] entity representing the multi-class skill tree.
 * Provides queries to filter skills by character class, power level, unlock status, and manage skills.
 */
@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY powerLevel ASC, name ASC")
    fun getAllSkills(): Flow<List<Skill>>

    @Query("SELECT * FROM skills WHERE characterClass = :characterClass ORDER BY powerLevel ASC")
    fun getSkillsByClass(characterClass: String): Flow<List<Skill>>

    @Query("SELECT * FROM skills WHERE powerLevel <= :maxPowerLevel ORDER BY powerLevel ASC")
    fun getSkillsByPowerLevel(maxPowerLevel: Int): Flow<List<Skill>>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getSkillById(id: String): Skill?

    @Query("SELECT * FROM skills WHERE isUnlocked = 1")
    fun getUnlockedSkills(): Flow<List<Skill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: Skill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<Skill>)

    @Update
    suspend fun updateSkill(skill: Skill)

    @Delete
    suspend fun deleteSkill(skill: Skill)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: String)

    @Query("DELETE FROM skills")
    suspend fun clearAllSkills()
}
