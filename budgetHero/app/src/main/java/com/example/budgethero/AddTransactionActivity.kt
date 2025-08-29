/**
 * Attribution:
 * Glide image loading implementation adapted from:
 *   - Eran Gross, "Introduction to Glide: The Image Loading Library for Android"
 *     URL: https://medium.com/@eran_6323/introduction-to-glide-the-image-loading-library-for-android-a3b9b0fc39a7
 *     Accessed on: 2025-06-09
 * Hiding a button programmatically in Android adapted from:
 *   - StackOverflow, "How to hide a button programmatically?"
 *     URL: https://stackoverflow.com/questions/70528244/how-to-make-the-items-of-my-firebase-recycler-adapter-onclick-with-kotlin-androi
 *     Accessed on: 2025-06-09
 */


package com.example.budgethero

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var editName: EditText
    private lateinit var editTotal: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var switchIncomeExpense: Switch
    private lateinit var buttonSelectTimestamp: Button
    private lateinit var textSelectedTimestamp: TextView
    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonUploadPhoto: Button
    private lateinit var imageReceipt: ImageView
    private lateinit var buttonSave: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var categoryList: List<String> = listOf()
    private var selectedTimestamp: Timestamp = Timestamp.now()
    private var receiptBitmap: Bitmap? = null
    private var receiptImageUri: Uri? = null

    private val selectPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageReceipt.setImageURI(uri)
            receiptImageUri = uri
            receiptBitmap = null
        }
    }
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            imageReceipt.setImageBitmap(bitmap)
            receiptBitmap = bitmap
            receiptImageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        editName = findViewById(R.id.editName)
        editTotal = findViewById(R.id.editTotal)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        switchIncomeExpense = findViewById(R.id.switchIncomeExpense)
        buttonSelectTimestamp = findViewById(R.id.buttonSelectTimestamp)
        textSelectedTimestamp = findViewById(R.id.textSelectedTimestamp)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        buttonUploadPhoto = findViewById(R.id.buttonUploadPhoto)
        imageReceipt = findViewById(R.id.imageReceipt)
        buttonSave = findViewById(R.id.buttonSave)

        loadCategories()
        buttonSelectTimestamp.setOnClickListener { pickDateTime() }
        buttonTakePhoto.setOnClickListener { takePhotoLauncher.launch(null) }
        buttonUploadPhoto.setOnClickListener { selectPhotoLauncher.launch("image/*") }
        buttonSave.setOnClickListener { saveTransaction() }
    }

    private fun loadCategories() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                categoryList = documents.mapNotNull { it.getString("name") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
            }
    }

    private fun pickDateTime() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                selectedTimestamp = Timestamp(calendar.time)
                textSelectedTimestamp.text = calendar.time.toString()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveTransaction() {
        val userId = auth.currentUser?.uid ?: return
        val name = editName.text.toString().trim()
        val totalInput = editTotal.text.toString().toDoubleOrNull() ?: 0.0
        val isIncome = switchIncomeExpense.isChecked
        val total = if (isIncome) totalInput else -totalInput
        val category = if (categoryList.isNotEmpty()) categoryList[spinnerCategory.selectedItemPosition] else ""

        if (name.isBlank() || category.isBlank()) {
            Toast.makeText(this, "Enter all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (receiptBitmap != null || receiptImageUri != null) {
            uploadReceiptImage { url ->
                addTransactionToFirestore(userId, name, selectedTimestamp, total, url, category)
            }
        } else {
            addTransactionToFirestore(userId, name, selectedTimestamp, total, null, category)
        }
    }

    private fun uploadReceiptImage(onResult: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("users/$userId/receipts/${UUID.randomUUID()}.jpg")

        if (receiptBitmap != null) {
            val baos = ByteArrayOutputStream()
            receiptBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            ref.putBytes(baos.toByteArray())
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { uri -> onResult(uri.toString()) }
                .addOnFailureListener { onResult(null) }
        } else if (receiptImageUri != null) {
            ref.putFile(receiptImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { uri -> onResult(uri.toString()) }
                .addOnFailureListener { onResult(null) }
        }
    }

    private fun addTransactionToFirestore(
        userId: String,
        name: String,
        timestamp: Timestamp,
        total: Double,
        receiptImage: String?,
        category: String
    ) {
        val transaction = Transaction(
            name = name,
            timestamp = timestamp,
            total = total,
            receiptImage = receiptImage,
            category = category
        )

        db.collection("users").document(userId)
            .collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction added!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add transaction.", Toast.LENGTH_SHORT).show()
            }
    }
}
