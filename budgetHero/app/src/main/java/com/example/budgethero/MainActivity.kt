package com.example.budgethero

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.activity.result.IntentSenderRequest


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val createAccountButton = findViewById<Button>(R.id.buttonCreateAccount)


        // One Tap Google Sign In setup
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        // Regular email/password login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handlePostLogin()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Email/password registration
        createAccountButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }


    }

    // Use the activity result API for Google sign-in
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                when {
                    idToken != null -> {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    handlePostLogin()
                                } else {
                                    Toast.makeText(this, "Google authentication failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                    else -> {
                        Toast.makeText(this, "No Google ID token received.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Google sign-in error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Checks if this is the first login and navigates accordingly
    private fun handlePostLogin() {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val userDoc = db.collection("users").document(uid)
            userDoc.get().addOnSuccessListener { document ->
                // Check if monthlyIncome field exists and is > 0
                val monthlyIncome = document.getDouble("monthlyIncome") ?: 0.0
                if (monthlyIncome > 0.0) {
                    // Income exists, skip first sign in screen
                    goToDashboard()
                } else {
                    // Ask for initial setup
                    goToFirstSignIn()
                }
            }.addOnFailureListener {
                // Treat as new user if we can't read the doc
                goToFirstSignIn()
            }
        }
    }


    private fun goToFirstSignIn() {
        startActivity(Intent(this, FirstSignInActivity::class.java))
        finish()
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
