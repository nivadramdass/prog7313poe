/**
 * Attribution:
 * Spinner implementation and simplification in Android adapted from:
 *   - Nelson Leme, "Simplifying Using Spinners in Android"
 *     URL: https://medium.com/geekculture/simplifying-using-spinners-in-android-ad14f8f1213d
 *     Accessed on: 2025-06-09
 */





package com.example.budgethero

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

sealed class StatementItem {
    data class StatementTransaction(val id: String, val transaction: Transaction) : StatementItem()
    data class StatementFixedExpense(val id: String, val fixedExpense: FixedExpense) : StatementItem()
    object Header : StatementItem()
}

class StatementsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var statementAdapter: StatementAdapter
    private lateinit var summaryTextView: TextView

    private lateinit var spinnerPeriod: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerSort: Spinner

    private var allTransactions: List<Pair<String, Transaction>> = emptyList()
    private var allFixedExpenses: List<Pair<String, FixedExpense>> = emptyList()
    private var categories: List<String> = listOf("All")
    private var monthlyIncome: Double = 0.0

    private var filterListenersSet = false // <--- Only set filter listeners once

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statements)

        summaryTextView = findViewById(R.id.textSummary)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStatement)
        recyclerView.layoutManager = LinearLayoutManager(this)
        statementAdapter = StatementAdapter { id, item ->
            if (item is StatementItem.StatementTransaction) {
                val intent = Intent(this, EditTransactionActivity::class.java)
                intent.putExtra("TRANSACTION_ID", id)
                startActivity(intent)
            }
        }
        recyclerView.adapter = statementAdapter

        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerType = findViewById(R.id.spinnerType)
        spinnerSort = findViewById(R.id.spinnerSort)

        // Static spinners
        spinnerPeriod.adapter = ArrayAdapter.createFromResource(this, R.array.period_options, android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = ArrayAdapter.createFromResource(this, R.array.type_options, android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = ArrayAdapter.createFromResource(this, R.array.sort_options, android.R.layout.simple_spinner_dropdown_item)

        loadData()
    }

    // Always reload data when coming back to this activity
    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setFilterListeners() {
        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) = filterAndDisplay()
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinnerPeriod.onItemSelectedListener = filterListener
        spinnerCategory.onItemSelectedListener = filterListener
        spinnerType.onItemSelectedListener = filterListener
        spinnerSort.onItemSelectedListener = filterListener
    }

    private fun loadData() {
        if (uid.isEmpty()) return

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            monthlyIncome = userDoc.getDouble("monthlyIncome") ?: 0.0

            // Categories (for filtering)
            db.collection("users").document(uid).collection("categories").get().addOnSuccessListener { catSnap ->
                categories = listOf("All") + catSnap.documents.mapNotNull { it.toObject(Category::class.java)?.name }
                spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
                spinnerCategory.setSelection(0)

                // Attach listeners after data is loaded (only once!)
                if (!filterListenersSet) {
                    setFilterListeners()
                    filterListenersSet = true
                }

                // Fixed Expenses
                db.collection("users").document(uid).collection("fixedExpenses").get().addOnSuccessListener { fixedExpSnap ->
                    allFixedExpenses = fixedExpSnap.documents.mapNotNull { doc ->
                        val obj = doc.toObject(FixedExpense::class.java)
                        if (obj != null) doc.id to obj else null
                    }

                    db.collection("users").document(uid).collection("transactions").get().addOnSuccessListener { txnSnap ->
                        allTransactions = txnSnap.documents.mapNotNull { doc ->
                            val obj = doc.toObject(Transaction::class.java)
                            if (obj != null) doc.id to obj else null
                        }
                        filterAndDisplay()
                    }
                }

            }
        }
    }

    private fun filterAndDisplay() {
        // Defensive: All spinners must be ready!
        if (spinnerCategory.adapter == null || spinnerCategory.adapter.count == 0) return

        val period = spinnerPeriod.selectedItemPosition
        val category = spinnerCategory.selectedItem?.toString() ?: "All"
        val type = spinnerType.selectedItemPosition
        val sort = spinnerSort.selectedItemPosition

        val now = Calendar.getInstance()
        // Filter transactions
        val filteredTxns = allTransactions.filter { (_, txn) ->
            // Period
            val date = txn.timestamp?.toDate() ?: Date(0)
            val periodOk = when (period) {
                1 -> isSameDay(date, now.time)
                2 -> isThisWeek(date, now)
                3 -> isThisMonth(date, now)
                4 -> isThisYear(date, now)
                else -> true
            }
            // Category
            val catOk = category == "All" || txn.category == category
            // Type
            val typeOk = when (type) {
                1 -> txn.total > 0
                2 -> txn.total < 0
                else -> true
            }
            periodOk && catOk && typeOk
        }

        // Sort
        val sortedTxns = when (sort) {
            0 -> filteredTxns.sortedByDescending { it.second.timestamp }
            1 -> filteredTxns.sortedBy { it.second.timestamp }
            2 -> filteredTxns.sortedByDescending { Math.abs(it.second.total) }
            3 -> filteredTxns.sortedBy { Math.abs(it.second.total) }
            else -> filteredTxns
        }

        // Totals for summary
        val totalFixedExpenses = allFixedExpenses.sumOf { it.second.amount }
        val totalIncome = sortedTxns.sumOf { if (it.second.total > 0) it.second.total else 0.0 }
        val totalExpenses = sortedTxns.sumOf { if (it.second.total < 0) -it.second.total else 0.0 }
        val net = monthlyIncome + totalIncome - (totalExpenses + totalFixedExpenses)

        summaryTextView.text =
            "Monthly Income: R%.2f\n".format(monthlyIncome) +
                    "Fixed Expenses: R%.2f\n".format(totalFixedExpenses) +
                    "Income (txns): R%.2f\n".format(totalIncome) +
                    "Expenses (txns): R%.2f\n".format(totalExpenses) +
                    "Net: R%.2f".format(net)

        // Build list
        val items = mutableListOf<StatementItem>()
        items.add(StatementItem.Header)
        allFixedExpenses.forEach { items.add(StatementItem.StatementFixedExpense(it.first, it.second)) }
        sortedTxns.forEach { items.add(StatementItem.StatementTransaction(it.first, it.second)) }
        statementAdapter.updateData(items)
    }

    // Date helpers
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
}

// --- Adapter ---

class StatementAdapter(
    private val onClick: (String, StatementItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<StatementItem>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is StatementItem.Header -> 0
        is StatementItem.StatementFixedExpense -> 1
        is StatementItem.StatementTransaction -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            1 -> FixedExpenseViewHolder(LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false))
            2 -> TransactionViewHolder(LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false))
            else -> HeaderViewHolder(TextView(parent.context))
        }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is StatementItem.Header -> (holder as HeaderViewHolder).textView.text = "Statement"
            is StatementItem.StatementFixedExpense -> (holder as FixedExpenseViewHolder).bind(item)
            is StatementItem.StatementTransaction -> (holder as TransactionViewHolder).bind(item, onClick)
        }
    }

    fun updateData(newItems: List<StatementItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    inner class FixedExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(android.R.id.text1)
        private val subtitle: TextView = view.findViewById(android.R.id.text2)
        fun bind(item: StatementItem.StatementFixedExpense) {
            title.text = "[Fixed Expense] ${item.fixedExpense.name}"
            subtitle.text = "R%.2f / month".format(item.fixedExpense.amount)
        }
    }
    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(android.R.id.text1)
        private val subtitle: TextView = view.findViewById(android.R.id.text2)
        fun bind(item: StatementItem.StatementTransaction, onClick: (String, StatementItem) -> Unit) {
            val t = item.transaction
            title.text = "${if (t.total < 0) "Expense" else "Income"}: ${t.name} (${t.category})"
            subtitle.text = "R%.2f • %s".format(t.total, t.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "")
            itemView.setOnClickListener { onClick(item.id, item) }
        }
    }
}
