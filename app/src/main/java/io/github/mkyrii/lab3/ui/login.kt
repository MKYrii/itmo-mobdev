package io.github.mkyrii.lab3.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import io.github.mkyrii.lab3.MainActivity
import io.github.mkyrii.lab3.R

class LoginFragment : Fragment() {

    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etLogin = view.findViewById(R.id.etLogin)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        progressBar = view.findViewById(R.id.progressBar)

        val repository = (requireActivity() as MainActivity).getRepository()
        val (savedName, savedPassword) = repository.getSavedCredentials()

        if (!savedName.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            etLogin.setText(savedName)
            etPassword.setText(savedPassword)
        }

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                showError("Заполните оба поля")
                return@setOnClickListener
            }

            performLogin(login, password)
        }
    }

    private fun performLogin(login: String, password: String) {
        setLoading(true)
        val repository = (requireActivity() as MainActivity).getRepository()

        repository.login(login, password,
            onSuccess = {
                activity?.runOnUiThread {
                    if (!isAdded || view == null) return@runOnUiThread
                    setLoading(false)
                    val mainActivity = requireActivity() as MainActivity
                    val containerId = if (mainActivity.isLandscape()) {
                        R.id.fragmentContainerLeft
                    } else {
                        R.id.fragmentContainer
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(containerId, ChatsFragment())
                        .commit()
                }
            },
            onError = { errorMsg ->
                activity?.runOnUiThread {
                    if (!isAdded || view == null) return@runOnUiThread
                    setLoading(false)
                    showError(errorMsg)
                }
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}