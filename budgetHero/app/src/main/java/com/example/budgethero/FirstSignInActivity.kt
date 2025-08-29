/**
 * Attribution:
 * - Firebase real-time data read/write patterns adapted from:
 *     Vikram Kodag, "Firebase real-time data read-write best practices - Android"
 *     https://medium.com/@kodagvikram/firebase-real-time-data-read-write-best-practices-android-67a06fa6420d
 *     Accessed: 2025-06-09
 * - Firebase Documentation, "Read and Write Data on Android"
 *     https://firebase.google.com/docs/database/android/read-and-write
 *     Accessed: 2025-06-09
 * - Subcollection handling in Firestore adapted from:
 *     StackOverflow, "How to add subcollection to a document in Firebase Cloud Firestore"
 *     https://stackoverflow.com/questions/47514419/how-to-add-subcollection-to-a-document-in-firebase-cloud-firestore
 *     Accessed: 2025-06-09
 **/

package com.example.budgethero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class FirstSignInActivity : AppCompatActivity() {

    private lateinit var incomeEditText: EditText
    private lateinit var expenseNameEditText: EditText
    private lateinit var expenseAmountEditText: EditText
    private lateinit var addExpenseButton: Button
    private lateinit var confirmButton: Button
    private lateinit var recyclerView: RecyclerView

    private val fixedExpenses = mutableListOf<FixedExpense>()
    private lateinit var adapter: FixedExpenseAdapter

    private val db = FirebaseFirestore.getInstance()
    private val uid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_sign_in)

        incomeEditText = findViewById(R.id.editTextIncome)
        expenseNameEditText = findViewById(R.id.editTextExpenseName)
        expenseAmountEditText = findViewById(R.id.editTextExpenseAmount)
        addExpenseButton = findViewById(R.id.buttonAddExpense)
        confirmButton = findViewById(R.id.buttonConfirm)
        recyclerView = findViewById(R.id.recyclerViewExpenses)

        adapter = FixedExpenseAdapter(fixedExpenses) { pos ->
            fixedExpenses.removeAt(pos)
            adapter.notifyItemRemoved(pos)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addExpenseButton.setOnClickListener {
            val name = expenseNameEditText.text.toString().trim()
            val amountText = expenseAmountEditText.text.toString().trim()
            if (name.isNotEmpty() && amountText.isNotEmpty()) {
                val amount = amountText.toDoubleOrNull()
                if (amount != null) {
                    fixedExpenses.add(FixedExpense(name, amount))
                    adapter.notifyItemInserted(fixedExpenses.size - 1)
                    expenseNameEditText.text.clear()
                    expenseAmountEditText.text.clear()
                } else {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter name and amount", Toast.LENGTH_SHORT).show()
            }
        }

        confirmButton.setOnClickListener {
            val income = incomeEditText.text.toString().toDoubleOrNull()
            if (income == null) {
                Toast.makeText(this, "Enter a valid monthly income", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 1. Save monthly income in user doc
            db.collection("users").document(uid)
                .set(mapOf("monthlyIncome" to income))
                .addOnSuccessListener {
                    // 2. Save fixed expenses as subcollection
                    val fixedExpenseRef = db.collection("users").document(uid).collection("fixedExpenses")
                    val batch = db.batch()
                    fixedExpenses.forEach { expense ->
                        val doc = fixedExpenseRef.document()
                        batch.set(doc, expense)
                    }
                    batch.commit().addOnSuccessListener {
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                        // Go to dashboard
                        startActivity(android.content.Intent(this, DashboardActivity::class.java))
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Could not save expenses", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Could not save income", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

class FixedExpenseAdapter(
    private val items: List<FixedExpense>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<FixedExpenseAdapter.FixedExpenseViewHolder>() {
    class FixedExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvExpenseName)
        val amount: TextView = view.findViewById(R.id.tvExpenseAmount)
        val deleteBtn: Button = view.findViewById(R.id.buttonDeleteExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FixedExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fixed_expense, parent, false)
        return FixedExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: FixedExpenseViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.amount.text = "R %.2f".format(item.amount)
        holder.deleteBtn.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount(): Int = items.size
}
