package com.flame.astronaut.assistant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flame.astronaut.domain.ChatRepository
import com.flame.astronaut.domain.ChatSummary
import com.flame.astronaut.net.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the request currently in flight. */
sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data object Loading : AssistantUiState
    data object Success : AssistantUiState
    class Error(val message: String, val retry: () -> Unit) : AssistantUiState
}

data class ChatMessageUi(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val response: AskResponse?,
)

// ------------------------------------------------------------------ chat history list (GET /chats)

sealed interface ChatListState {
    data object Loading : ChatListState
    data object Empty : ChatListState
    class Error(val message: String) : ChatListState
    class Content(val chats: List<ChatSummary>) : ChatListState
}

class AssistantListViewModel(private val repo: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ChatListState.Loading
            fetch()
        }
    }

    /** Quiet refresh (e.g. coming back from a conversation): keeps the current list on screen while loading. */
    fun refresh() {
        if (_state.value is ChatListState.Loading) return
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        repo.chats()
            .onSuccess { list -> _state.value = if (list.isEmpty()) ChatListState.Empty else ChatListState.Content(list) }
            .onFailure { if (_state.value !is ChatListState.Content) _state.value = ChatListState.Error(it.userMessage()) }
    }
}

// ------------------------------------------------------------------ one conversation

/** Loading of the saved conversation (GET /chats/{id}). */
sealed interface HistoryState {
    data object Ready : HistoryState
    data object Loading : HistoryState
    class Error(val message: String) : HistoryState
}

class AssistantChatViewModel(
    private val repo: ChatRepository,
    handle: SavedStateHandle,
) : ViewModel() {

    /** null = a new conversation: it is created on the first question, so no empty chats are left behind. */
    private var chatId: Long? = handle.get<Long>("conversationId")?.takeIf { it > 0 }

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(emptyList())
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    private val _history = MutableStateFlow<HistoryState>(if (chatId != null) HistoryState.Loading else HistoryState.Ready)
    val history: StateFlow<HistoryState> = _history.asStateFlow()

    private val _state = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    private var requestJob: Job? = null
    private var localId = -1L // negative ids for messages created on this device

    init {
        if (chatId != null) loadHistory()
        else handle.get<String>("q")?.takeIf { it.isNotBlank() }?.let { send(it) } // e.g. asked from the emergency screen
    }

    fun loadHistory() {
        val id = chatId ?: return
        viewModelScope.launch {
            _history.value = HistoryState.Loading
            repo.chat(id)
                .onSuccess { detail ->
                    _messages.value = detail.turns.flatMap { t ->
                        listOfNotNull(
                            t.question.takeIf { it.isNotBlank() }?.let { ChatMessageUi(t.id * 2, true, it, null) },
                            t.answer?.let { ChatMessageUi(t.id * 2 + 1, false, it.answer.orEmpty(), it) },
                        )
                    }
                    _history.value = HistoryState.Ready
                }
                .onFailure { _history.value = HistoryState.Error(it.userMessage()) }
        }
    }

    fun canSend(): Boolean = _state.value !is AssistantUiState.Loading && _history.value is HistoryState.Ready

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty() || !canSend()) return
        _messages.update { it + ChatMessageUi(localId--, true, question, null) }
        runRequest(question)
    }

    private fun runRequest(question: String) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _state.value = AssistantUiState.Loading
            try {
                var id = chatId
                if (id == null) {
                    val created = repo.createChat()
                    val newId = created.getOrNull()
                    if (newId == null) {
                        val problem = created.exceptionOrNull()?.userMessage() ?: "Couldn't start a new chat."
                        _state.value = AssistantUiState.Error(problem) { runRequest(question) }
                        return@launch
                    }
                    chatId = newId
                    id = newId
                }
                repo.ask(question, id)
                    .onSuccess { r ->
                        chatId = r.chatId
                        _messages.update { it + ChatMessageUi(localId--, false, r.answer.answer.orEmpty(), r.answer) }
                        _state.value = AssistantUiState.Success
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        _state.value = AssistantUiState.Error(e.userMessage()) { runRequest(question) }
                    }
            } catch (e: CancellationException) {
                throw e // leaving the screen cancels the in-flight request
            }
        }
    }
}
