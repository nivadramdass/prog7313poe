/**
 * Attribution:
 * Pattern for Firebase real-time data read/write in Android adapted from:
 *   - Vikram Kodag, "Firebase real-time data read-write best practices - Android"
 *     URL: https://medium.com/@kodagvikram/firebase-real-time-data-read-write-best-practices-android-67a06fa6420d
 *     Accessed on: 2025-06-09
 *   - Firebase Documentation, "Read and Write Data on Android"
 *     URL: https://firebase.google.com/docs/database/android/read-and-write
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

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var editCategoryName: EditText
    private lateinit var editColour: EditText
    private lateinit var buttonSaveCategory: Button
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var categoryData: List<Pair<String, Category>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        editCategoryName = findViewById(R.id.editCategoryName)
        editColour = findViewById(R.id.editColour)
        buttonSaveCategory = findViewById(R.id.buttonSaveCategory)
        recyclerCategories = findViewById(R.id.recyclerCategories)

        recyclerCategories.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(categoryData) { id ->
            deleteCategory(id)
        }
        recyclerCategories.adapter = categoryAdapter

        buttonSaveCategory.setOnClickListener { saveCategory() }

        loadCategories()
    }

    private fun saveCategory() {
        val userId = auth.currentUser?.uid ?: return
        val name = editCategoryName.text.toString().trim()
        val colour = editColour.text.toString().trim()

        if (name.isBlank() || colour.isBlank()) {
            Toast.makeText(this, "Enter all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val category = Category(name, colour)
        db.collection("users").document(userId)
            .collection("categories")
            .add(category)
            .addOnSuccessListener {
                Toast.makeText(this, "Category added!", Toast.LENGTH_SHORT).show()
                editCategoryName.text.clear()
                editColour.text.clear()
                loadCategories()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add category.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCategories() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                categoryData = documents.map { it.id to (it.toObject(Category::class.java)) }
                categoryAdapter.updateData(categoryData)
            }
    }

    private fun deleteCategory(categoryId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("categories")
            .document(categoryId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Category deleted.", Toast.LENGTH_SHORT).show()
                loadCategories()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show()
            }
    }
}
