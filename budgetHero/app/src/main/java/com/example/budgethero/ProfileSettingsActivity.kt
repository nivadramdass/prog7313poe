package com.example.budgethero

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var buttonUpdateEmail: Button
    private lateinit var buttonUpdatePassword: Button
    private lateinit var darkModeSwitch: Switch
    private lateinit var expenseNameEditText: EditText
    private lateinit var expenseAmountEditText: EditText
    private lateinit var buttonAddExpense: Button
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: FirestoreFixedExpenseAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    private lateinit var incomeEditText: EditText
    private lateinit var buttonUpdateIncome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        buttonUpdateEmail = findViewById(R.id.buttonUpdateEmail)
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword)
        darkModeSwitch = findViewById(R.id.switchDarkMode)
        expenseNameEditText = findViewById(R.id.editTextExpenseName)
        expenseAmountEditText = findViewById(R.id.editTextExpenseAmount)
        buttonAddExpense = findViewById(R.id.buttonAddExpense)
        recyclerView = findViewById(R.id.recyclerViewFixedExpenses)
        incomeEditText = findViewById(R.id.editTextMonthlyIncome)
        buttonUpdateIncome = findViewById(R.id.buttonUpdateIncome)

        // -- DARK MODE TOGGLE --
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        darkModeSwitch.isChecked = nightMode == AppCompatDelegate.MODE_NIGHT_YES
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Email & Password update
        buttonUpdateEmail.setOnClickListener {
            val newEmail = emailEditText.text.toString().trim()
            if (newEmail.isNotEmpty()) {
                auth.currentUser?.updateEmail(newEmail)
                    ?.addOnSuccessListener { Toast.makeText(this, "Email updated!", Toast.LENGTH_SHORT).show() }
                    ?.addOnFailureListener { Toast.makeText(this, "Failed to update email.", Toast.LENGTH_SHORT).show() }
            }
        }
        buttonUpdatePassword.setOnClickListener {
            val newPass = passwordEditText.text.toString()
            if (newPass.length >= 6) {
                auth.currentUser?.updatePassword(newPass)
                    ?.addOnSuccessListener { Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show() }
                    ?.addOnFailureListener { Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show() }
            } else {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show()
            }
        }

        // Recycler setup
        adapter = FirestoreFixedExpenseAdapter { id -> deleteExpense(id) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        loadExpenses()

        // Add new fixed expense
        buttonAddExpense.setOnClickListener {
            val name = expenseNameEditText.text.toString().trim()
            val amountText = expenseAmountEditText.text.toString().trim()
            val amount = amountText.toDoubleOrNull()
            if (name.isNotEmpty() && amount != null) {
                db.collection("users").document(uid).collection("fixedExpenses")
                    .add(FixedExpense(name, amount))
                    .addOnSuccessListener {
                        expenseNameEditText.text.clear()
                        expenseAmountEditText.text.clear()
                        loadExpenses()
                    }
                    .addOnFailureListener { Toast.makeText(this, "Failed to add expense", Toast.LENGTH_SHORT).show() }
            } else {
                Toast.makeText(this, "Enter valid name and amount", Toast.LENGTH_SHORT).show()
            }
        }

        buttonUpdateIncome.setOnClickListener {
            val incomeValue = incomeEditText.text.toString().toDoubleOrNull()
            if (incomeValue != null && uid.isNotEmpty()) {
                db.collection("users").document(uid)
                    .update("monthlyIncome", incomeValue)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Monthly income updated!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update income", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val income = doc.getDouble("monthlyIncome")
            if (income != null) {
                incomeEditText.setText("%.2f".format(income))
            }
        }
    }

    private fun loadExpenses() {
        db.collection("users").document(uid).collection("fixedExpenses")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.documents.mapNotNull { doc ->
                    val obj = doc.toObject(FixedExpense::class.java)
                    if (obj != null) doc.id to obj else null
                }
                adapter.updateData(list)
            }
    }

    private fun deleteExpense(id: String) {
        db.collection("users").document(uid).collection("fixedExpenses")
            .document(id).delete()
            .addOnSuccessListener { loadExpenses() }
            .addOnFailureListener { Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show() }
    }
}

// Firestore-aware Adapter (use only for ProfileSettingsActivity)
class FirestoreFixedExpenseAdapter(
    private var items: List<Pair<String, FixedExpense>> = emptyList(),
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<FirestoreFixedExpenseAdapter.FixedExpenseViewHolder>() {

    inner class FixedExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvExpenseName: TextView = view.findViewById(R.id.tvExpenseName)
        val tvExpenseAmount: TextView = view.findViewById(R.id.tvExpenseAmount)
        val buttonDelete: Button = view.findViewById(R.id.buttonDeleteExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FixedExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fixed_expense, parent, false)
        return FixedExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: FixedExpenseViewHolder, position: Int) {
        val (id, expense) = items[position]
        holder.tvExpenseName.text = expense.name
        holder.tvExpenseAmount.text = "R %.2f".format(expense.amount)
        holder.buttonDelete.setOnClickListener { onDelete(id) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<String, FixedExpense>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
