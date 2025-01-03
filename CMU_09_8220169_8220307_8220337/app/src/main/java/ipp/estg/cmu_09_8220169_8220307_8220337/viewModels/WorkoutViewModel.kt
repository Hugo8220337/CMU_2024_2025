package ipp.estg.cmu_09_8220169_8220307_8220337.viewModels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ipp.estg.cmu_09_8220169_8220307_8220337.data.preferences.UserPreferencesRepository
import ipp.estg.cmu_09_8220169_8220307_8220337.data.retrofit.RemoteApis
import ipp.estg.cmu_09_8220169_8220307_8220337.data.retrofit.models.exerciceDbApi.ExerciseItemDataResponse
import ipp.estg.cmu_09_8220169_8220307_8220337.data.room.LocalDatabase
import ipp.estg.cmu_09_8220169_8220307_8220337.data.room.models.Workout
import ipp.estg.cmu_09_8220169_8220307_8220337.repositories.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val workoutRepository: WorkoutRepository = WorkoutRepository(
        exerciseDbApi = RemoteApis.getExerciseDbApi(),
        workoutDao = LocalDatabase.getDatabase(application).workoutDao
    )
    private val userPreferencesRepository = UserPreferencesRepository(application)

    // Estado de workout
    private val _workout = MutableStateFlow<List<Workout?>>(emptyList())
    val workout = _workout.asStateFlow()

    // Estado de carregamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Estado de erro
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var state by mutableStateOf(ScreenState())
        private set

    fun generateWorkout(bodyParts: List<String>) {
        viewModelScope.launch {
            state = state.copy(isGeneratingWorkout = true)

            // load exercises from the repository
            val exercises = workoutRepository.getExercisesByBodyParts(
                bodyParts = bodyParts,
                limit = 10,
                offset = 0
            )

            // store exercises on the state
            state = state.copy(workout = exercises)

            state = state.copy(isGeneratingWorkout = false)
        }
    }

    fun getWorkouts() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)

            val storedWorkouts = workoutRepository.getAllWorkouts()

            if(storedWorkouts.isNotEmpty()) {
                state = state.copy(storedWorkouts = storedWorkouts)
            }

            state = state.copy(isLoading = false)
        }
    }

    //get all workouts from Firebase by user ID
    fun getWorkoutsByUserID() {
        val userId = userPreferencesRepository.getCurrentUserId()
        viewModelScope.launch {
            state = state.copy(isLoading = true)

            val workouts = workoutRepository.getWorkoutsByUserID(userId)

            if(workouts.isNotEmpty()) {
                state = state.copy(storedWorkouts = workouts)
                _workout.value = workouts
            }

            state = state.copy(isLoading = false)
        }
    }



    data class ScreenState(
        val isLoading: Boolean = false,
        val isGeneratingWorkout: Boolean = false,
        val error: String? = null,
        val workout: List<ExerciseItemDataResponse> = emptyList(), // exercises generated by the API
        val storedWorkouts: List<Workout> = emptyList() // exercises stored on the local database
    )
}

