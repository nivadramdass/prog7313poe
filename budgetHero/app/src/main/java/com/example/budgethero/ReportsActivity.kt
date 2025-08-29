package com.example.budgethero

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity: AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var ledgerAdapter: LedgerAdapter
    private lateinit var spinnerPeriod: Spinner
    private lateinit var buttonSharePdf: Button

    private var allTxns: List<Pair<String, Transaction>> = emptyList()
    private var allFixedExpenses: List<Pair<String, FixedExpense>> = emptyList()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private var currentLedger: List<String> = emptyList()
    private var currentPeriodLabel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        buttonSharePdf = findViewById(R.id.buttonSharePdf)

        val periods = arrayOf("All Time", "Today", "This Week", "This Month", "This Year")
        spinnerPeriod.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periods)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLedger)
        recyclerView.layoutManager = LinearLayoutManager(this)
        ledgerAdapter = LedgerAdapter()
        recyclerView.adapter = ledgerAdapter

        spinnerPeriod.setSelection(0)
        spinnerPeriod.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                currentPeriodLabel = periods[position]
                filterAndShowLedger()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        buttonSharePdf.setOnClickListener {
            generateAndSharePdf()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        if (uid.isEmpty()) return
        db.collection("users").document(uid).collection("fixedExpenses").get().addOnSuccessListener { fixedExpSnap ->
            allFixedExpenses = fixedExpSnap.documents.mapNotNull { doc ->
                val obj = doc.toObject(FixedExpense::class.java)
                if (obj != null) doc.id to obj else null
            }
            db.collection("users").document(uid).collection("transactions").get().addOnSuccessListener { txnSnap ->
                allTxns = txnSnap.documents.mapNotNull { doc ->
                    val obj = doc.toObject(Transaction::class.java)
                    if (obj != null) doc.id to obj else null
                }
                filterAndShowLedger()
            }
        }
    }

    private fun filterAndShowLedger() {
        val period = spinnerPeriod.selectedItemPosition
        val now = Calendar.getInstance()

        val filteredTxns = allTxns.filter { (_, txn) ->
            val date = txn.timestamp?.toDate() ?: Date(0)
            when (period) {
                1 -> isSameDay(date, now.time)
                2 -> isThisWeek(date, now)
                3 -> isThisMonth(date, now)
                4 -> isThisYear(date, now)
                else -> true
            }
        }
        val ledgerRows = mutableListOf<String>()
        ledgerRows.add("DATE | TYPE | NAME | CATEGORY | AMOUNT")
        allFixedExpenses.forEach { (_, f) ->
            ledgerRows.add("- | Fixed Expense | ${f.name} | - | R%.2f".format(f.amount))
        }
        filteredTxns.sortedBy { it.second.timestamp }.forEach { (_, t) ->
            val type = if (t.total < 0) "Expense" else "Income"
            ledgerRows.add("${t.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "-"} | $type | ${t.name} | ${t.category} | R%.2f".format(t.total))
        }
        currentLedger = ledgerRows
        ledgerAdapter.updateData(ledgerRows)
    }

    // Helper functions (reuse your previous isSameDay/isThisWeek/etc.)
    private fun isSameDay(date: Date, ref: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { time = ref }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    private fun isThisWeek(date: Date, now: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { time = date }
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
    }
    private fun isThisMonth(date: Date, now: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { time = date }
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
    private fun isThisYear(date: Date, now: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { time = date }
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }

    // PDF export & share
    private fun generateAndSharePdf() {
        val pdfDoc = PdfDocument()
        val paint = Paint().apply { color = Color.BLACK; textSize = 16f }
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val margin = 40
        var y = 60
        canvas.drawText("Ledger Report - $currentPeriodLabel", margin.toFloat(), y.toFloat(), paint)
        y += 30
        for (line in currentLedger) {
            if (y > 800) break // stop if off the page
            canvas.drawText(line, margin.toFloat(), y.toFloat(), paint)
            y += 24
        }
        pdfDoc.finishPage(page)

        // Save PDF to cache
        val file = File(cacheDir, "report.pdf")
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()

        // Share intent
        val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share Report PDF"))
    }
}

// Simple RecyclerView Adapter for ledger view
class LedgerAdapter : RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder>() {
    private val items = mutableListOf<String>()
    fun updateData(data: List<String>) {
        items.clear(); items.addAll(data); notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LedgerViewHolder =
        LedgerViewHolder(TextView(parent.context).apply { setPadding(12, 12, 12, 12) })
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: LedgerViewHolder, pos: Int) {
        holder.textView.text = items[pos]
        holder.textView.setBackgroundColor(if (pos == 0) Color.LTGRAY else Color.TRANSPARENT)
    }
    inner class LedgerViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
