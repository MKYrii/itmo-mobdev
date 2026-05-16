package io.github.mkyrii.lab3

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.mkyrii.lab3.repository.ChatRepository
import io.github.mkyrii.lab3.storage.PreferencesManager
import io.github.mkyrii.lab3.ui.LoginFragment
import io.github.mkyrii.lab3.ui.ChatsFragment
import io.github.mkyrii.lab3.ui.MessagesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var preferencesManager: PreferencesManager

    var selectedChat: String? = null

    fun getRepository(): ChatRepository = repository
    fun getPreferencesManager(): PreferencesManager = preferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesManager = PreferencesManager(this)
        repository = ChatRepository(preferencesManager)

        selectedChat = savedInstanceState?.getString("selected_chat")

        if (repository.isLoggedIn()) {
            val username = preferencesManager.savedName ?: ""
            repository.setOnNewMessageCallback { message ->
                Log.d("MainActivity", "ПОЛУЧЕН КОЛБЭК, сообщение: ${message.data.Text?.text}")
                runOnUiThread {
                    val messagesFragment = getCurrentMessagesFragment()
                    Log.d("MainActivity", "messagesFragment = $messagesFragment")
                    messagesFragment?.onNewMessageReceived(message)
                }
            }
        }

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

    fun isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun showLoginFragment() {
        val fragment = LoginFragment()
        if (isLandscape()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerLeft, fragment)
                .commit()
            supportFragmentManager.beginTransaction()
                .remove(getRightFragment())
                .commitAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
        selectedChat = null
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
}