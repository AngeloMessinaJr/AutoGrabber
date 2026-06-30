package com.example.autograbber.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autograbber.data.ChatRepository
import io.agora.chat.ChatMessage
import io.agora.chat.TextMessageBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessageUi(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean
)

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(emptyList())
    val messages: StateFlow<List<ChatMessageUi>> = _messages

    private val _connectionState = MutableStateFlow("Connecting...")
    val connectionState: StateFlow<String> = _connectionState

    // Agora user IDs MUST be alphanumeric and lowercase.
    private val currentUserId = (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
        ?.filter { it.isLetterOrDigit() } ?: "guest${(1000..9999).random()}").lowercase()
    
    // Using a shared room ID for the community
    private val chatroomId = "316766812176388"

    init {
        setupChat()
    }

    private fun setupChat() {
        chatRepository.init()
        viewModelScope.launch {
            // Attempt to login. 
            val (loggedIn, errorCode) = chatRepository.login(currentUserId, "")
            
            if (loggedIn) {
                _connectionState.value = "Joined Community"
                chatRepository.joinChatroom(chatroomId)
                
                chatRepository.observeMessages().collect { newMessages ->
                    val uiMessages = newMessages.map { it.toUi() }
                    _messages.value = (_messages.value + uiMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                }
            } else {
                _connectionState.value = when (errorCode) {
                    110 -> "Login Refused (Invalid Parameters or AppKey)"
                    208 -> "Security Error (Token Required in Console)"
                    else -> "Offline (Error: $errorCode)"
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        chatRepository.sendMessage(content, chatroomId, isChatroom = true)
        
        // Add locally for instant feedback
        val myMessage = ChatMessageUi(
            id = "local_${System.currentTimeMillis()}",
            sender = "Me",
            content = content,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )
        _messages.value = (_messages.value + myMessage)
    }

    private fun ChatMessage.toUi(): ChatMessageUi {
        val body = this.body as? TextMessageBody
        return ChatMessageUi(
            id = this.msgId,
            sender = this.from,
            content = body?.message ?: "",
            timestamp = this.msgTime,
            isFromMe = this.from == currentUserId
        )
    }
}
