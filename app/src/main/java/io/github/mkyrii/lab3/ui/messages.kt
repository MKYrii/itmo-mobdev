package io.github.mkyrii.lab3.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.mkyrii.lab3.MainActivity
import io.github.mkyrii.lab3.Message
import io.github.mkyrii.lab3.R
import io.github.mkyrii.lab3.repository.ChatRepository
import io.github.mkyrii.lab3.util.MessageMerge

class MessagesFragment : Fragment() {

    private lateinit var repository: ChatRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: Button
    private lateinit var tvOffline: TextView

    private var channelName: String = ""
    private var messages = mutableListOf<Message>()
    private var lastKnownId: Int = 0
    private var isLoading = false
    private var hasMore = true

    private var typingHandler: android.os.Handler? = null
    private val TYPING_DELAY = 1000L

    companion object {
        private const val CHANNEL_KEY = "channel_name"
        fun newInstance(channelName: String): MessagesFragment {
            val fragment = MessagesFragment()
            val args = Bundle()
            args.putString(CHANNEL_KEY, channelName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("channel_name", channelName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = (requireActivity() as MainActivity).getRepository()
        channelName = arguments?.getString(CHANNEL_KEY)
            ?: savedInstanceState?.getString("channel_name")
            ?: "1@channel"

        recyclerView = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        progressBar = view.findViewById(R.id.progressBar)
        btnBack = view.findViewById(R.id.btnBack)
        tvOffline = view.findViewById(R.id.tvOffline)

        setupRecyclerView()
        setupTypingListener()
        setupScrollListener()
        loadMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }

        btnBack.setOnClickListener {
            val activity = requireActivity() as MainActivity
            if (activity.isLandscape()) {
                activity.closeChat()
            } else {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupTypingListener() {
        etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                repository.sendStartTyping(channelName)
                startTypingTimer()
            }
        }

        etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                repository.sendStartTyping(channelName)
                startTypingTimer()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun startTypingTimer() {
        typingHandler?.removeCallbacksAndMessages(null)
        typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
        typingHandler?.postDelayed({
            repository.sendEndTyping()
        }, TYPING_DELAY)
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(
            onImageClick = { imageUrl ->
                val intent = Intent(requireContext(), ImageActivity::class.java)
                intent.putExtra("image_url", imageUrl)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupScrollListener() {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMore && firstVisibleItemPosition <= 2 && totalItemCount > 0) {
                    loadMoreMessages()
                }
            }
        })
    }

    fun refreshData() {
        if (isAdded && view != null) {
            loadMessages()
        }
    }

    private fun loadMessages() {
        if (isLoading || !isAdded || view == null) return
        isLoading = true
        updateOfflineBanner()
        runOnUiThreadIfActive { progressBar.visibility = View.VISIBLE }

        repository.getMessages(
            channelName = channelName,
            limit = 20,
            lastKnownId = 9999999,
            reverse = false,
            onSuccess = { newMessages ->
                runOnUiThreadIfActive {
                    applyMessages(newMessages, scrollToEnd = true)
                    isLoading = false
                    progressBar.visibility = View.GONE
                    hasMore = newMessages.size >= 20
                    updateOfflineBanner()
                }
            },
            onError = { errorMsg ->
                runOnUiThreadIfActive {
                    progressBar.visibility = View.GONE
                    isLoading = false
                    updateOfflineBanner()
                    handleError(errorMsg)
                }
            }
        )
    }

    private fun applyMessages(newMessages: List<Message>, scrollToEnd: Boolean) {
        messages.clear()
        messages.addAll(MessageMerge.sortChronologically(newMessages))
        updateLastKnownId()
        adapter.submitList(messages.toList())
        if (scrollToEnd) {
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        recyclerView.post {
            if (messages.isNotEmpty()) {
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun loadMoreMessages() {
        if (isLoading || !hasMore) return
        isLoading = true

        repository.getMessages(
            channelName = channelName,
            limit = 20,
            lastKnownId = lastKnownId,
            reverse = false,
            onSuccess = { olderMessages ->
                runOnUiThreadIfActive {
                    if (olderMessages.isNotEmpty()) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisible = layoutManager.findFirstVisibleItemPosition()
                        val topOffset = layoutManager.findViewByPosition(firstVisible)?.top ?: 0
                        val oldSize = messages.size
                        val currentMessages = messages.toList()

                        messages.clear()
                        messages.addAll(
                            MessageMerge.sortChronologically(olderMessages + currentMessages)
                        )
                        val addedCount = messages.size - oldSize
                        updateLastKnownId()
                        adapter.submitList(messages.toList())
                        if (addedCount > 0 && firstVisible >= 0) {
                            layoutManager.scrollToPositionWithOffset(
                                firstVisible + addedCount,
                                topOffset
                            )
                        }
                        hasMore = olderMessages.size >= 20
                    } else {
                        hasMore = false
                    }
                    isLoading = false
                    updateOfflineBanner()
                }
            },
            onError = { errorMsg ->
                runOnUiThreadIfActive {
                    isLoading = false
                    handleError(errorMsg)
                }
            }
        )
    }

    private fun updateLastKnownId() {
        if (messages.isNotEmpty()) {
            lastKnownId = messages.first().id.toIntOrNull() ?: 0
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        etMessage.text.clear()
        progressBar.visibility = View.VISIBLE

        val username = (activity as? MainActivity)?.getRepository()
            ?.getSavedCredentials()?.first ?: ""

        repository.sendMessage(
            from = username,
            to = channelName,
            text = text,
            onSuccess = {
                runOnUiThreadIfActive {
                    progressBar.visibility = View.GONE
                    lastKnownId = 0
                    loadMessages()
                }
            },
            onQueued = { queuedMessage ->
                runOnUiThreadIfActive {
                    progressBar.visibility = View.GONE
                    updateOfflineBanner()
                    messages.add(queuedMessage)
                    messages.sortWith(compareBy<Message> { it.time }.thenBy { it.id })
                    adapter.submitList(messages.toList())
                    scrollToBottom()
                    Toast.makeText(requireContext(), R.string.message_queued, Toast.LENGTH_SHORT).show()
                }
            },
            onError = { errorMsg ->
                runOnUiThreadIfActive {
                    progressBar.visibility = View.GONE
                    showError(errorMsg)
                }
            }
        )
    }

    fun onNewMessageReceived(message: Message) {
        Log.d("MessagesFragment", "onNewMessageReceived: канал=${message.to}, наш канал=$channelName, текст=${message.data.Text?.text}")
        runOnUiThreadIfActive {
            if (message.to == channelName || message.from == channelName) {
                addMessageIfNotDuplicate(message)
            }
        }
    }

    private fun addMessageIfNotDuplicate(message: Message) {
        if (messages.any { it.id == message.id }) return
        messages.clear()
        messages.addAll(MessageMerge.sortChronologically(messages + message))
        updateLastKnownId()
        adapter.submitList(messages.toList())
        scrollToBottom()
    }

    private fun updateOfflineBanner() {
        if (!::tvOffline.isInitialized) return
        tvOffline.visibility = if (repository.isOnline()) View.GONE else View.VISIBLE
    }

    private fun runOnUiThreadIfActive(block: () -> Unit) {
        activity?.runOnUiThread {
            if (isAdded && view != null) {
                block()
            }
        }
    }

    // Переопредели onDestroy
    override fun onDestroyView() {
        super.onDestroyView()
        typingHandler?.removeCallbacksAndMessages(null)
        repository.sendEndTyping()
    }

    private fun handleError(errorMsg: String) {
        progressBar.visibility = View.GONE
        isLoading = false
        if (errorMsg.contains("401") || errorMsg.contains("Не авторизован")) {
            goToLogin()
        } else {
            showError(errorMsg)
        }
    }

    private fun goToLogin() {
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, LoginFragment())
            .commit()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Адаптер для сообщений
    private class MessagesAdapter(
        private val onImageClick: (String) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<Message> = emptyList()

        companion object {
            private const val TYPE_TEXT = 0
            private const val TYPE_IMAGE = 1
        }

        fun submitList(list: List<Message>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (items[position].data.Text != null) TYPE_TEXT else TYPE_IMAGE
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_TEXT -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_message_text, parent, false)
                    TextMessageViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_message_image, parent, false)
                    ImageMessageViewHolder(view, onImageClick)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = items[position]
            when (holder) {
                is TextMessageViewHolder -> holder.bind(message)
                is ImageMessageViewHolder -> holder.bind(message)
            }
        }

        override fun getItemCount(): Int = items.size

        class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvFrom: TextView = itemView.findViewById(R.id.tvFrom)
            private val tvText: TextView = itemView.findViewById(R.id.tvText)
            private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

            fun bind(message: Message) {
                tvFrom.text = message.from
                tvText.text = message.data.Text?.text ?: ""
                tvTime.text = android.text.format.DateFormat.format("HH:mm:ss", message.time * 1000)
            }
        }

        class ImageMessageViewHolder(
            itemView: View,
            private val onImageClick: (String) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
            private val tvFrom: TextView = itemView.findViewById(R.id.tvFrom)
            private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

            fun bind(message: Message) {
                tvFrom.text = message.from
                tvTime.text = android.text.format.DateFormat.format("HH:mm:ss", message.time * 1000)

                val imagePath = message.data.Image?.link ?: ""
                val thumbUrl = "https://faerytea.name/thumb/$imagePath"

                Glide.with(itemView.context)
                    .load(thumbUrl)
                    .into(ivImage)

                ivImage.setOnClickListener {
                    val fullUrl = "https://faerytea.name/img/$imagePath"
                    onImageClick(fullUrl)
                }
            }
        }
    }
}