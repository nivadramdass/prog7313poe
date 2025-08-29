package com.example.budgethero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onClick: (Transaction) -> Unit,
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textName)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textTotal: TextView = view.findViewById(R.id.textTotal)
        val textDate: TextView = view.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.textName.text = transaction.name
        holder.textCategory.text = transaction.category
        holder.textTotal.text = "R%.2f".format(transaction.total)
        // Format timestamp if available
        holder.textDate.text = transaction.timestamp?.toDate()?.toString() ?: ""

        holder.itemView.setOnClickListener { onClick(transaction) }
        holder.itemView.setOnLongClickListener {
            onLongClick(transaction)
            true
        }
    }

    fun updateData(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }
}
