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
 * - Bar chart and pie chart implementation using MPAndroidChart adapted from:
 *     Malcolm Maima, "(Kotlin) Implementing a Barchart and Piechart using MPAndroidChart"
 *     https://malcolmmaima.medium.com/kotlin-implementing-a-barchart-and-piechart-using-mpandroidchart-8c7643c4ba75
 *     Accessed: 2025-06-09
 */



package com.example.budgethero

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

class DashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var legendLayout: LinearLayout
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvBalance: TextView
    private lateinit var goalsLayout: LinearLayout
    private lateinit var spinner: Spinner

    private var expenseListener: ListenerRegistration? = null
    private var goalsListener: ListenerRegistration? = null
    private var categories: Map<String, String> = emptyMap() // category name -> colour hex

    private var selectedPeriod: Pair<Long, String> = getPeriods()[0] // Default: This month

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)



        // View references
        pieChart = findViewById(R.id.pieChartExpenses)
        legendLayout = findViewById(R.id.legendSection)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvBalance = findViewById(R.id.tvBalance)
        goalsLayout = findViewById(R.id.goalsListLayout)
        barChart = findViewById(R.id.barChartTopCategories)
        spinner = findViewById(R.id.spinnerSummaryPeriod)

        // Set spinner options (periods)
        val periodLabels = getPeriods().map { it.second }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periodLabels)
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedPeriod = getPeriods()[position]
                fetchCategoriesThenListen()
                listenToTopCategoriesBarChart()
            }
        }

        // Button and navigation listeners
        findViewById<ImageButton>(R.id.buttonProfile).setOnClickListener {
            startActivity(Intent(this, ProfileSettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.buttonAddTransaction).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
        findViewById<ImageButton>(R.id.buttonAddCategory).setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAddGoal).setOnClickListener {
            startActivity(Intent(this, AddGoalActivity::class.java))
        }

        findViewById<Button>(R.id.buttonViewAllExpenses).setOnClickListener {
            startActivity(Intent(this, StatementsActivity::class.java))
        }
        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_dashboard -> {} // Already here
                R.id.menu_statements -> startActivity(Intent(this, StatementsActivity::class.java))
                R.id.menu_reports -> startActivity(Intent(this, ReportsActivity::class.java))
                R.id.menu_awards -> Toast.makeText(this, "Awards coming soon!", Toast.LENGTH_SHORT).show()
                R.id.menu_logout -> finishAffinity()
            }
            true
        }

        // Start loading dashboard data (calls are inside spinner as well)
        fetchCategoriesThenListen()
        listenToTopCategoriesBarChart()



    }

    override fun onDestroy() {
        super.onDestroy()
        expenseListener?.remove()
        goalsListener?.remove()
    }

    // 1. Period helpers
    companion object {
        fun getPeriods(): List<Pair<Long, String>> {
            // Returns (startMillis, label). You can add more periods if needed
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            // This month
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            // This year
            calendar.set(java.util.Calendar.MONTH, 0)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val yearStart = calendar.timeInMillis
            // All time (pick 1970 as default)
            val allStart = 0L
            return listOf(
                Pair(monthStart, "This month"),
                Pair(yearStart, "This year"),
                Pair(allStart, "All time")
            )
        }
    }

    // Step 1: Load categories with colors, then attach listeners that need them
    private fun fetchCategoriesThenListen() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("categories")
            .addSnapshotListener { docs, error ->
                if (error != null || docs == null) return@addSnapshotListener
                categories = docs.mapNotNull {
                    val cat = it.toObject(Category::class.java)
                    if (cat.name.isNotEmpty() && cat.colour.isNotEmpty()) cat.name to cat.colour else null
                }.toMap()
                listenToExpensesAndBalance()
            }
    }

    // Step 2: Listen for expenses, update donut + legend + balance, then call goals
    private fun listenToExpensesAndBalance() {
        val uid = auth.currentUser?.uid ?: return

        // Period filter
        val startMillis = selectedPeriod.first
        val startTimestamp = Timestamp(java.util.Date(startMillis))

        expenseListener?.remove()
        expenseListener = db.collection("users").document(uid).collection("transactions")
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .addSnapshotListener { docs, error ->
                if (error != null || docs == null) return@addSnapshotListener

                val categorySums = mutableMapOf<String, Double>()
                var totalExpenses = 0.0
                var allTransactions = 0.0
                var allIncome = 0.0

                for (doc in docs) {
                    val txn = doc.toObject(Transaction::class.java)
                    allTransactions += txn.total
                    if (txn.total < 0) {
                        val cat = txn.category.ifEmpty { "Other" }
                        categorySums[cat] = (categorySums[cat] ?: 0.0) + -txn.total
                        totalExpenses += -txn.total
                    } else {
                        allIncome += txn.total
                    }
                }

                // Get profile for monthly income and fixed expenses
                db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                    val profile = userDoc.toObject(UserProfile::class.java)
                    val monthlyIncome = profile?.monthlyIncome ?: 0.0
                    val fixedExpenses = profile?.fixedExpenses ?: emptyList()
                    val fixedTotal = fixedExpenses.sumOf { it.amount }
                    // Net savings = monthlyIncome - fixedExpenses - all variable expenses (totalExpenses) + sum of positive transactions for period
                    val netBalance = monthlyIncome - fixedTotal - totalExpenses + allIncome

                    updatePieChart(categorySums)
                    updateLegend(categorySums)
                    tvTotalExpenses.text = "Total Expenses: R%.2f".format(totalExpenses)
                    tvBalance.text = "Balance: R%.2f".format(netBalance)

                    listenToGoals(netBalance)
                }
            }
    }

    // Step 3: Listen for goals and update their progress (with a 1D line chart)
    private fun listenToGoals(balance: Double) {
        val uid = auth.currentUser?.uid ?: return
        goalsListener?.remove()
        goalsListener = db.collection("users").document(uid).collection("goals")
            .addSnapshotListener { docs, error ->
                if (error != null || docs == null) return@addSnapshotListener

                goalsLayout.removeAllViews()
                for (doc in docs) {
                    val goal = doc.toObject(Goal::class.java)
                    val statusColor: Int
                    val statusText: String

                    when {
                        balance < goal.min -> {
                            statusColor = Color.RED
                            statusText = "Below Goal"
                        }
                        balance > goal.max -> {
                            statusColor = Color.GREEN
                            statusText = "Above Goal"
                        }
                        else -> {
                            statusColor = Color.BLUE
                            statusText = "Within Goal"
                        }
                    }

                    val chart = LineChart(this)
                    chart.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 180
                    )

                    val entries = listOf(
                        Entry(0f, goal.min.toFloat()),
                        Entry(1f, balance.toFloat()),
                        Entry(2f, goal.max.toFloat())
                    )
                    val valueLabels = listOf(
                        "Min: %.2f".format(goal.min),
                        "Current: %.2f".format(balance),
                        "Max: %.2f".format(goal.max)
                    )

                    val dataSet = LineDataSet(entries, "").apply {
                        setDrawCircles(true)
                        setCircleColors(listOf(Color.RED, statusColor, Color.GREEN))
                        color = statusColor // Line color matches status
                        setDrawValues(true)
                        valueTextSize = 14f
                        valueTextColor = statusColor
                        lineWidth = 5f
                        circleRadius = 8f
                        valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getPointLabel(entry: Entry?): String {
                                return when (entry?.x) {
                                    0f -> "Min\n%.0f".format(goal.min)
                                    1f -> "Now\n%.0f".format(balance)
                                    2f -> "Max\n%.0f".format(goal.max)
                                    else -> "%.0f".format(entry?.y ?: 0f)
                                }
                            }
                        }
                    }
                    val lineData = LineData(dataSet)
                    chart.data = lineData
                    chart.axisLeft.isEnabled = false
                    chart.axisRight.isEnabled = false
                    chart.xAxis.isEnabled = false
                    chart.legend.isEnabled = false
                    chart.description.isEnabled = false
                    chart.setExtraOffsets(0f, 20f, 0f, 20f)
                    chart.invalidate()

                    // Label for the chart
                    val label = TextView(this)
                    label.text = "${goal.name} (Min: ${goal.min}, Max: ${goal.max}, Current: %.2f, $statusText)".format(balance)
                    label.setPadding(0, 16, 0, 0)
                    label.setTextColor(statusColor)
                    label.textSize = 16f

                    goalsLayout.addView(label)
                    goalsLayout.addView(chart)
                }
            }
    }



    // Step 4: Listen for bar chart updates (top 3 categories)
    private fun listenToTopCategoriesBarChart() {
        val uid = auth.currentUser?.uid ?: return
        val startMillis = selectedPeriod.first
        val startTimestamp = Timestamp(java.util.Date(startMillis))

        db.collection("users").document(uid).collection("transactions")
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .addSnapshotListener { docs, error ->
                if (error != null || docs == null) return@addSnapshotListener

                val freq = mutableMapOf<String, Int>()
                for (doc in docs) {
                    val txn = doc.toObject(Transaction::class.java)
                    if (txn.total < 0) {
                        freq[txn.category] = (freq[txn.category] ?: 0) + 1
                    }
                }
                val top = freq.entries.sortedByDescending { it.value }.take(3)
                val entries = top.mapIndexed { idx, (cat, count) ->
                    BarEntry(idx.toFloat(), count.toFloat())
                }
                val dataSet = BarDataSet(entries, "Top Categories")
                dataSet.colors = top.map { getColorForCategory(it.key) }
                dataSet.valueTextColor = Color.BLACK
                dataSet.valueTextSize = 14f
                dataSet.barShadowColor = Color.TRANSPARENT
                dataSet.barBorderWidth = 0.5f

                // Slim bars
                val barData = BarData(dataSet)
                barData.barWidth = 0.2f // Half default width

                barChart.data = barData
                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(top.map { it.key })
                barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                barChart.xAxis.granularity = 1f
                barChart.xAxis.setDrawGridLines(false)
                barChart.axisLeft.isEnabled = false
                barChart.axisRight.isEnabled = false
                barChart.legend.isEnabled = false
                barChart.description.isEnabled = false
                barChart.invalidate()
            }
    }

    // Donut chart: Use Firestore category color if possible, else fallback color
    private fun updatePieChart(categorySums: Map<String, Double>) {
        if (categorySums.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No expenses yet"
            pieChart.invalidate()
            return
        }
        val entries = categorySums.map { (cat, sum) -> PieEntry(sum.toFloat(), cat) }
        val dataSet = PieDataSet(entries, "Expenses")
        dataSet.colors = entries.map { getColorForCategory(it.label ?: "Other") }
        // Remove value overlays, but keep category label
        dataSet.setDrawValues(false)
        dataSet.valueTextColor = Color.TRANSPARENT // Not shown
        dataSet.valueTextSize = 0f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = ""
        pieChart.setEntryLabelColor(Color.BLACK)
        // Hide legend in chart itself, as you display your own
        pieChart.legend.isEnabled = false
        pieChart.invalidate()
    }

    private fun updateLegend(categorySums: Map<String, Double>) {
        legendLayout.removeAllViews()
        for ((cat, sum) in categorySums) {
            val tv = TextView(this)
            tv.text = "$cat: R%.2f".format(sum)
            tv.setTextColor(getColorForCategory(cat))
            tv.setPadding(4, 4, 4, 4)
            legendLayout.addView(tv)
        }
    }

    // Fetch color from loaded categories map, fallback to hash color if missing
    private fun getColorForCategory(cat: String): Int {
        val hex = categories[cat]
        return try {
            if (hex != null && hex.startsWith("#")) Color.parseColor(hex)
            else if (hex != null) Color.parseColor("#$hex")
            else fallbackColor(cat)
        } catch (e: Exception) {
            fallbackColor(cat)
        }
    }

    // Fallback: pastel color based on hash
    private fun fallbackColor(cat: String): Int {
        val palette = listOf("#35BBCA", "#F8D90F", "#D3DD18", "#FE7A15", "#0191B4")
        return Color.parseColor(palette[abs(cat.hashCode()) % palette.size])
    }
}
