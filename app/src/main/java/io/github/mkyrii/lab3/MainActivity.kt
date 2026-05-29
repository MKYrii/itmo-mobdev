package io.github.mkyrii.lab3

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import io.github.mkyrii.lab3.network.NetworkMonitor
import io.github.mkyrii.lab3.repository.ChatRepository
import io.github.mkyrii.lab3.storage.PreferencesManager
import io.github.mkyrii.lab3.ui.LoginFragment
import io.github.mkyrii.lab3.ui.ChatsFragment
import io.github.mkyrii.lab3.ui.MessagesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var networkMonitor: NetworkMonitor

    var selectedChat: String? = null
    private var isRestoringUi = false

    fun getRepository(): ChatRepository = repository
    fun getPreferencesManager(): PreferencesManager = preferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesManager = PreferencesManager(this)
        repository = ChatRepository(this, preferencesManager)
        networkMonitor = NetworkMonitor(this)

        selectedChat = savedInstanceState?.getString("selected_chat")

        setupWebSocketCallback()
        setupNetworkMonitor()
        if (savedInstanceState != null) {
            clearFragmentState()
        }
        restoreUi()

        supportFragmentManager.addOnBackStackChangedListener {
            if (isRestoringUi || isLandscape()) return@addOnBackStackChangedListener
            val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (current !is MessagesFragment) {
                selectedChat = null
                getChatsFragment()?.updateSelectedChat(null)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isLandscape()) {
                    if (selectedChat != null) {
                        closeChat()
                    } else {
                        finish()
                    }
                } else {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                    when (currentFragment) {
                        is MessagesFragment -> {
                            supportFragmentManager.popBackStack()
                            selectedChat = null
                            getChatsFragment()?.updateSelectedChat(null)
                        }
                        else -> {
                            finish()
                        }
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        if (::networkMonitor.isInitialized) {
            networkMonitor.stop()
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selected_chat", selectedChat)
    }

    private fun setupNetworkMonitor() {
        repository.setOnPendingFlushComplete {
            runOnUiThread { refreshVisibleScreens() }
        }
        networkMonitor.onNetworkAvailable = {
            runOnUiThread {
                repository.reconnectIfLoggedIn()
                refreshVisibleScreens()
            }
        }
        networkMonitor.start()
    }

    private fun refreshVisibleScreens() {
        getChatsFragment()?.refreshData()
        getCurrentMessagesFragment()?.refreshData()
    }

    private fun setupWebSocketCallback() {
        if (!repository.isLoggedIn()) return
        repository.setOnNewMessageCallback { message ->
            Log.d("MainActivity", "ПОЛУЧЕН КОЛБЭК, сообщение: ${message.data.Text?.text}")
            runOnUiThread {
                val messagesFragment = getCurrentMessagesFragment()
                Log.d("MainActivity", "messagesFragment = $messagesFragment")
                messagesFragment?.onNewMessageReceived(message)
            }
        }
    }

    private fun clearFragmentState() {
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        for (fragment in supportFragmentManager.fragments.toList()) {
            if (fragment.isAdded) {
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNowAllowingStateLoss()
            }
        }
    }

    private fun restoreUi() {
        isRestoringUi = true
        try {
            if (!repository.isLoggedIn()) {
                showLoginFragmentNow()
                return
            }
            val chatToOpen = selectedChat

            if (isLandscape()) {
                val transaction = supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerLeft, ChatsFragment())
                if (chatToOpen != null) {
                    transaction.replace(
                        R.id.fragmentContainerRight,
                        MessagesFragment.newInstance(chatToOpen)
                    )
                }
                transaction.commitNowAllowingStateLoss()
            } else if (chatToOpen != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ChatsFragment())
                    .commitNowAllowingStateLoss()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, MessagesFragment.newInstance(chatToOpen))
                    .addToBackStack(null)
                    .commit()
                supportFragmentManager.executePendingTransactions()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ChatsFragment())
                    .commitNowAllowingStateLoss()
            }

            if (chatToOpen != null) {
                getChatsFragment()?.updateSelectedChat(chatToOpen)
            }
        } finally {
            isRestoringUi = false
        }
    }

    private fun showLoginFragmentNow() {
        selectedChat = null
        val transaction = supportFragmentManager.beginTransaction()
        if (isLandscape()) {
            transaction.replace(R.id.fragmentContainerLeft, LoginFragment())
            supportFragmentManager.findFragmentById(R.id.fragmentContainerRight)?.let {
                transaction.remove(it)
            }
        } else {
            transaction.replace(R.id.fragmentContainer, LoginFragment())
        }
        transaction.commitNowAllowingStateLoss()
    }

    fun isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun showLoginFragment() {
        selectedChat = null
        val transaction = supportFragmentManager.beginTransaction()
        if (isLandscape()) {
            transaction.replace(R.id.fragmentContainerLeft, LoginFragment())
            supportFragmentManager.findFragmentById(R.id.fragmentContainerRight)?.let {
                transaction.remove(it)
            }
        } else {
            transaction.replace(R.id.fragmentContainer, LoginFragment())
        }
        transaction.commitNowAllowingStateLoss()
    }

    fun showChatsFragment() {
        val fragment = ChatsFragment()
        if (isLandscape()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerLeft, fragment)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }

    fun showMessagesFragment(chatName: String) {
        selectedChat = chatName
        val fragment = MessagesFragment.newInstance(chatName)

        getChatsFragment()?.updateSelectedChat(chatName)

        if (isLandscape()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerRight, fragment)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    fun closeChat() {
        if (isLandscape()) {
            selectedChat = null
            val rightFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerRight)
            if (rightFragment != null) {
                supportFragmentManager.beginTransaction()
                    .remove(rightFragment)
                    .commit()
            }
            getChatsFragment()?.updateSelectedChat(null)
        }
    }

    private fun getChatsFragment(): ChatsFragment? {
        val fragment = if (isLandscape()) {
            supportFragmentManager.findFragmentById(R.id.fragmentContainerLeft)
        } else {
            supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        }
        return if (fragment is ChatsFragment) fragment else null
    }

    private fun getCurrentMessagesFragment(): MessagesFragment? {
        val fragment = if (isLandscape()) {
            supportFragmentManager.findFragmentById(R.id.fragmentContainerRight)
        } else {
            supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        }
        return if (fragment is MessagesFragment) fragment else null
    }
}