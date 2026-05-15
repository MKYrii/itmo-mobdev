package io.github.mkyrii.lab3

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.mkyrii.lab3.repository.ChatRepository
import io.github.mkyrii.lab3.storage.PreferencesManager
import io.github.mkyrii.lab3.network.RetrofitClient
import io.github.mkyrii.lab3.ui.ChatsFragment
import io.github.mkyrii.lab3.ui.LoginFragment
import io.github.mkyrii.lab3.ui.ImageActivity
import io.github.mkyrii.lab3.ui.MessagesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var preferencesManager: PreferencesManager

    var selectedChat: String? = null

    fun getRepository(): ChatRepository = repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesManager = PreferencesManager(this)
        val retrofitClient = RetrofitClient(preferencesManager)
        repository = ChatRepository(retrofitClient.apiService, preferencesManager)

        selectedChat = savedInstanceState?.getString("selected_chat")

        if (savedInstanceState == null) {
            if (repository.isLoggedIn()) {
                showChatsFragment()
                if (isLandscape() && selectedChat != null) {
                    showMessagesFragment(selectedChat!!)
                }
            } else {
                showLoginFragment()
            }
        }

        if (repository.isLoggedIn()) {
            repository.initWebSocket(
                onNewMessage = { message ->
                    runOnUiThread {
                        val messagesFragment = getCurrentMessagesFragment()
                        messagesFragment?.onNewMessageReceived(message)
                    }
                },
                onUnauthorized = {
                    runOnUiThread {
                        handleUnauthorized()
                    }
                }
            )
        }

        // Обработка кнопки "назад" через OnBackPressedDispatcher
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
                        }
                        else -> {
                            finish()
                        }
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedChat?.let { outState.putString("selected_chat", it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.closeWebSocket()
    }

    fun isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun showLoginFragment() {
        val fragment = LoginFragment()
        if (isLandscape()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerLeft, fragment as Fragment)
                .commit()
            supportFragmentManager.beginTransaction()
                .remove(getRightFragment())
                .commitAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment as Fragment)
                .commit()
        }

        selectedChat = null
    }

    fun getPreferencesManager(): PreferencesManager = preferencesManager

    fun showChatsFragment() {
        val fragment = ChatsFragment()
        if (isLandscape()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerLeft, fragment as Fragment)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment as Fragment)
                .commit()
        }
    }

    fun showMessagesFragment(chatName: String) {
        selectedChat = chatName
        val fragment = MessagesFragment.newInstance(chatName)

        val chatsFragment = getChatsFragment()
        chatsFragment?.updateSelectedChat(chatName)

        if (isLandscape()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerRight, fragment as Fragment)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment as Fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    fun closeChat() {
        if (isLandscape()) {
            selectedChat = null
            supportFragmentManager.beginTransaction()
                .remove(getRightFragment())
                .commit()
            getChatsFragment()?.updateSelectedChat(null)
        }
    }

    private fun getRightFragment(): Fragment {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainerRight) ?: Fragment()
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

    private fun handleUnauthorized() {
        preferencesManager.clear()
        showLoginFragment()
        Toast.makeText(this, "Сессия истекла, войдите заново", Toast.LENGTH_LONG).show()
    }
}