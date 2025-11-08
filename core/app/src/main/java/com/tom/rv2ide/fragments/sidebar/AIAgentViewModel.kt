package com.tom.rv2ide.fragments.sidebar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AIAgentViewModel : ViewModel() {

  data class ChatMessage(var text: String, val isUser: Boolean, var isStreaming: Boolean)

  private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
  val messages: LiveData<MutableList<ChatMessage>> = _messages

  fun addUserMessage(text: String) {
    val list = _messages.value ?: mutableListOf()
    list.add(ChatMessage(text, true, false))
    _messages.value = list
  }

  fun startAssistantStreaming(): Int {
    val list = _messages.value ?: mutableListOf()
    list.add(ChatMessage("", false, true))
    _messages.value = list
    return list.lastIndex
  }

  fun updateStreaming(index: Int, text: String) {
    val list = _messages.value ?: return
    if (index in list.indices) {
      list[index].text = text
      _messages.value = list
    }
  }

  fun finalizeStreaming(index: Int) {
    val list = _messages.value ?: return
    if (index in list.indices) {
      list[index].isStreaming = false
      _messages.value = list
    }
  }
}
