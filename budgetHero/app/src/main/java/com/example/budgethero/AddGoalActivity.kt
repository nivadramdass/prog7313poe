/**
 * Attribution:
 * Subcollection handling in Firestore adapted from:
 *   - StackOverflow, "How to add subcollection to a document in Firebase Cloud Firestore"
 *     URL: https://stackoverflow.com/questions/47514419/how-to-add-subcollection-to-a-document-in-firebase-cloud-firestore
 *     Accessed on: 2025-06-09
 */


package com.example.budgethero

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddGoalActivity : AppCompatActivity() {

    private lateinit var editGoalName: EditText
    private lateinit var editGoalMin: EditText
    private lateinit var editGoalMax: EditText
    private lateinit var buttonSaveGoal: Button
    private lateinit var recyclerGoals: RecyclerView
    private lateinit var goalAdapter: GoalAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var goalData: List<Pair<String, Goal>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_goal)

        editGoalName = findViewById(R.id.editGoalName)
        editGoalMin = findViewById(R.id.editGoalMin)
        editGoalMax = findViewById(R.id.editGoalMax)
        buttonSaveGoal = findViewById(R.id.buttonSaveGoal)
        recyclerGoals = findViewById(R.id.recyclerGoals)

        recyclerGoals.layoutManager = LinearLayoutManager(this)
        goalAdapter = GoalAdapter(goalData) { id ->
            deleteGoal(id)
        }
        recyclerGoals.adapter = goalAdapter

        buttonSaveGoal.setOnClickListener { saveGoal() }

        loadGoals()
    }

    private fun saveGoal() {
        val userId = auth.currentUser?.uid ?: return
        val name = editGoalName.text.toString().trim()
        val minText = editGoalMin.text.toString().trim()
        val maxText = editGoalMax.text.toString().trim()

        if (name.isBlank() || minText.isBlank() || maxText.isBlank()) {
            Toast.makeText(this, "Enter all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val min = minText.toDoubleOrNull()
        val max = maxText.toDoubleOrNull()
        if (min == null || max == null) {
            Toast.makeText(this, "Invalid numbers.", Toast.LENGTH_SHORT).show()
            return
        }

        val goal = Goal(name, min, max)
        db.collection("users").document(userId)
            .collection("goals")
            .add(goal)
            .addOnSuccessListener {
                Toast.makeText(this, "Goal added!", Toast.LENGTH_SHORT).show()
                editGoalName.text.clear()
                editGoalMin.text.clear()
                editGoalMax.text.clear()
                loadGoals()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add goal.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGoals() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("goals")
            .get()
            .addOnSuccessListener { documents ->
                goalData = documents.map { it.id to (it.toObject(Goal::class.java)) }
                goalAdapter.updateData(goalData)
            }
    }

    private fun deleteGoal(goalId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("goals")
            .document(goalId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Goal deleted.", Toast.LENGTH_SHORT).show()
                loadGoals()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show()
            }
    }
}
