package com.abhinav.dodaily.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder, id")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder, id")
    suspend fun getTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): Task?

    @Query("SELECT MAX(sortOrder) FROM tasks")
    suspend fun maxSortOrder(): Int?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM completions")
    fun observeCompletions(): Flow<List<Completion>>

    @Query("SELECT * FROM completions")
    suspend fun getCompletions(): List<Completion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: Completion)

    @Query("DELETE FROM completions WHERE taskId = :taskId AND day = :day")
    suspend fun deleteCompletion(taskId: Long, day: Long)
}
