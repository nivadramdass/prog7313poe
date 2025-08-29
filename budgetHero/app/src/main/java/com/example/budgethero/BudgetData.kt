package com.example.budgethero

data class FixedExpense(
    val name: String = "",
    val amount: Double = 0.0
)
data class Transaction(
    val name: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val total: Double = 0.0,
    val receiptImage: String? = null,
    val category: String = ""
)
data class Goal(
    val name: String = "",
    val min: Double = 0.0,
    val max: Double = 0.0
)
data class Category(
    val name: String = "",
    val colour: String = ""
)
data class UserProfile(
    val monthlyIncome: Double = 0.0,
    val fixedExpenses: List<FixedExpense> = emptyList()
)
