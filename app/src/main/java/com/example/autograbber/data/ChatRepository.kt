package com.example.autograbber.data

import android.content.Context
import android.util.Log
import io.agora.chat.ChatClient
import io.agora.chat.ChatOptions
import io.agora.chat.ChatMessage
import io.agora.chat.ChatMessage.ChatType
import io.agora.CallBack
import io.agora.MessageListener
import io.agora.ValueCallBack
import io.agora.chat.ChatRoom
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepository(private val context: Context) {

    // Using provided Agora credentials
    private val appKey = "41200000795#200050874"
    private val appId = "21f0c15dedf447f09c5c6ed099de3cbb"

    fun init() {
        if (ChatClient.getInstance().isSdkInited) return

        val options = ChatOptions()
        // Ensure AppKey is set through the setter
        options.appKey = appKey.trim()
        
        // Some newer SDK versions allow/require AppId
        try {
            val setAppIdMethod = options.javaClass.getMethod("setAppId", String::class.java)
            setAppIdMethod.invoke(options, appId.trim())
            Log.d("ChatRepository", "Set AppId: $appId")
        } catch (e: Exception) {
            // Method might not exist in this specific SDK build
        }

        options.autoLogin = true 
        ChatClient.getInstance().init(context, options)
        
        // Enable detailed logging to help troubleshoot connection
        ChatClient.getInstance().setDebugMode(true)
        Log.d("ChatRepository", "Agora Chat SDK Initialized with Key: $appKey")
    }

    suspend fun login(userId: String, token: String): Pair<Boolean, Int> = suspendCoroutine { continuation ->
        ChatClient.getInstance().loginWithToken(userId, token, object : CallBack {
            override fun onSuccess() {
                Log.d("ChatRepository", "Chat Login Successful: $userId")
                continuation.resume(Pair(true, 0))
            }

            override fun onError(code: Int, error: String?) {
                Log.e("ChatRepository", "Chat Login Failed: $code - $error")
                continuation.resume(Pair(false, code))
            }
        })
    }

    fun logout() {
        ChatClient.getInstance().logout(true)
    }

    suspend fun joinChatroom(chatroomId: String): Boolean = suspendCoroutine { continuation ->
        ChatClient.getInstance().chatroomManager().joinChatRoom(chatroomId, object : ValueCallBack<ChatRoom> {
            override fun onSuccess(value: ChatRoom?) {
                Log.d("ChatRepository", "Joined Chatroom: $chatroomId")
                continuation.resume(true)
            }

            override fun onError(code: Int, error: String?) {
                Log.e("ChatRepository", "Failed to Join Chatroom: $code - $error")
                continuation.resume(false)
            }
        })
    }

    fun sendMessage(content: String, toUser: String, isChatroom: Boolean = false) {
        val message = ChatMessage.createTextSendMessage(content, toUser)
        message.chatType = if (isChatroom) ChatType.ChatRoom else ChatType.Chat
        
        message.setMessageStatusCallback(object : CallBack {
            override fun onSuccess() {
                Log.d("ChatRepository", "Message sent successfully")
            }
            override fun onError(code: Int, error: String?) {
                Log.e("ChatRepository", "Message failed to send: $code - $error")
            }
        })

        ChatClient.getInstance().chatManager().sendMessage(message)
    }

    fun observeMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : MessageListener {
            override fun onMessageReceived(messages: List<ChatMessage>) {
                trySend(messages)
            }
        }
        ChatClient.getInstance().chatManager().addMessageListener(listener)
        awaitClose {
            ChatClient.getInstance().chatManager().removeMessageListener(listener)
        }
    }
}
