package com.fitplan.app.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.ExerciseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseListViewModel(private val daos: AllDaos) : ViewModel() {
    val exercises = daos.exerciseDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(exercise: ExerciseEntity) = viewModelScope.launch { daos.exerciseDao.delete(exercise) }

    companion object {
        val Factory = viewModelFactory { initializer { ExerciseListViewModel(Graph.daos) } }
    }
}

class ExerciseEditViewModel(
    private val daos: AllDaos,
    private val exerciseId: Long,
) : ViewModel() {
    val categories = daos.exerciseDao.observeAll()
        .map { list -> list.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaults = daos.settingsDao.observe()
        .map { it ?: com.fitplan.app.data.db.SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.fitplan.app.data.db.SettingsEntity())

    suspend fun loadExisting(): ExerciseEntity? = daos.exerciseDao.getById(exerciseId)

    suspend fun loadDefaults(): com.fitplan.app.data.db.SettingsEntity =
        daos.settingsDao.get() ?: com.fitplan.app.data.db.SettingsEntity()

    /** 保存动作。ackMaxWeight（已处理重量）由训练流程维护、编辑页不携带，以库内现值为准避免被保存覆盖。 */
    fun save(exercise: ExerciseEntity, onDone: () -> Unit) = viewModelScope.launch {
        val ackMaxWeight = daos.exerciseDao.getById(exercise.id)?.ackMaxWeight
        daos.exerciseDao.upsert(exercise.copy(ackMaxWeight = ackMaxWeight))
        onDone()
    }

    companion object {
        fun factory(exerciseId: Long) = viewModelFactory {
            initializer { ExerciseEditViewModel(Graph.daos, exerciseId) }
        }
    }
}
