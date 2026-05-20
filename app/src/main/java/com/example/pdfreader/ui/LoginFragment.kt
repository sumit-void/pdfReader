package com.example.pdfreader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pdfreader.R

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameInput = view.findViewById<EditText>(R.id.login_username)
        val passwordInput = view.findViewById<EditText>(R.id.login_password)
        val loginBtn = view.findViewById<Button>(R.id.login_btn)
        val skipBtn = view.findViewById<TextView>(R.id.login_skip)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter dummy credentials", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                navigateToLibrary()
            }
        }

        skipBtn.setOnClickListener {
            navigateToLibrary()
        }
    }

    private fun navigateToLibrary() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, LibraryFragment())
            .commit()
    }
}
