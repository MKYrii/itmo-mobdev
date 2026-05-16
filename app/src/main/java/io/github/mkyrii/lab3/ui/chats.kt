package io.github.mkyrii.lab3.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.mkyrii.lab3.MainActivity
import io.github.mkyrii.lab3.R
import io.github.mkyrii.lab3.repository.ChatRepository

class ChatsFragment : Fragment() {

    private lateinit var repository: ChatRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogout: Button
    private lateinit var adapter: ChatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = (requireActivity() as MainActivity).getRepository()

        recyclerView = view.findViewById(R.id.rvChats)
        progressBar = view.findViewById(R.id.progressBar)
        btnLogout = view.findViewById(R.id.btnLogout)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ChatsAdapter(
            onChatClick = { chatName ->
                (requireActivity() as MainActivity).showMessagesFragment(chatName)
            }
        )
        recyclerView.adapter = adapter

        btnLogout.setOnClickListener {
            logout()
        }

        loadChats()
    }

    fun updateSelectedChat(chat: String?) {
        if (!::adapter.isInitialized) return
        adapter.updateSelectedChat(chat)
    }

    private fun loadChats() {
        if (!isAdded || view == null) return
        progressBar.visibility = View.VISIBLE
        repository.getChannels(
            onSuccess = { channels ->
                runOnUiThreadIfActive {
                    progressBar.visibility = View.GONE
                    adapter.submitList(channels)
                }
            },
            onError = { errorMsg ->
                runOnUiThreadIfActive {
                    progressBar.visibility = View.GONE
                    if (errorMsg.contains("401") || errorMsg.contains("Не авторизован")) {
                        goToLogin()
                    } else {
                        showError(errorMsg)
                    }
                }
            }
        )
    }

    private fun runOnUiThreadIfActive(block: () -> Unit) {
        activity?.runOnUiThread {
            if (isAdded && view != null) {
                block()
            }
        }
    }

    private fun logout() {
        repository.logout(
            onSuccess = {
                runOnUiThreadIfActive {
                    repository.closeWebSocket()
                    val prefs = (requireActivity() as MainActivity).getPreferencesManager()
                    prefs.clear()
                    requireActivity().finish()
                    startActivity(requireActivity().intent)
                }
            },
            onError = {
                runOnUiThreadIfActive {
                    val prefs = (requireActivity() as MainActivity).getPreferencesManager()
                    prefs.clear()
                    requireActivity().finish()
                    startActivity(requireActivity().intent)
                }
            }
        )
    }

    private fun goToLogin() {
        // Очищаем весь стек фрагментов
        parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        // Заменяем текущий фрагмент на экран входа
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, LoginFragment())
            .commit()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                if (message.contains("401") || message.contains("Не авторизован")) {
                    goToLogin()
                }
            }
            .show()
    }

    // Адаптер для списка чатов
    private class ChatsAdapter(
        private val onChatClick: (String) -> Unit
    ) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

        private var items: List<String> = emptyList()
        private var selectedChat: String? = null

        fun submitList(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }

        fun updateSelectedChat(chat: String?) {
            selectedChat = chat
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val chat = items[position]
            holder.bind(chat, onChatClick, selectedChat == chat)
        }

        override fun getItemCount(): Int = items.size

        class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvChatName: TextView = itemView.findViewById(R.id.tvChatName)

            fun bind(chatName: String, onChatClick: (String) -> Unit, isSelected: Boolean) {
                tvChatName.text = chatName
                itemView.setOnClickListener { onChatClick(chatName) }
                itemView.setBackgroundColor(
                    if (isSelected) android.graphics.Color.LTGRAY
                    else android.graphics.Color.TRANSPARENT
                )
            }
        }
    }
}