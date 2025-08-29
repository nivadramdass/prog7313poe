package com.example.budgethero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalAdapter(
    private var goals: List<Pair<String, Goal>>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textGoalName: TextView = view.findViewById(R.id.textGoalName)
        val textGoalRange: TextView = view.findViewById(R.id.textGoalRange)
        val buttonDeleteGoal: Button = view.findViewById(R.id.buttonDeleteGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val (id, goal) = goals[position]
        holder.textGoalName.text = goal.name
        holder.textGoalRange.text = "R${goal.min} - R${goal.max}"
        holder.buttonDeleteGoal.setOnClickListener { onDelete(id) }
    }

    override fun getItemCount() = goals.size

    fun updateData(newGoals: List<Pair<String, Goal>>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}
