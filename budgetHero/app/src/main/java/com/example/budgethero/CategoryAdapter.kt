/**
 * Attribution(for onBindViewHolder(...)):
 * RecyclerView item onClick implementation adapted from:
 *   - StackOverflow, "how to make the items of my Firebase Recycler adapter onClick with kotlin android"
 *     URL: https://stackoverflow.com/questions/70528244/how-to-make-the-items-of-my-firebase-recycler-adapter-onclick-with-kotlin-android
 *     Accessed on: 2025-06-09
 */


package com.example.budgethero

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<Pair<String, Category>>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewColour: View = view.findViewById(R.id.viewColour)
        val textCategoryName: TextView = view.findViewById(R.id.textCategoryName)
        val buttonDeleteCategory: Button = view.findViewById(R.id.buttonDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }


    /**
     * Attribution:
     * Logic adapted from StackOverflow answer:
     * "how to make the items of my Firebase Recycler adapter onClick with kotlin android"
     * https://stackoverflow.com/questions/70528244/how-to-make-the-items-of-my-firebase-recycler-adapter-onclick-with-kotlin-android
     * Accessed on: 2025-06-09
     */

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val (id, category) = categories[position]
        holder.textCategoryName.text = category.name
        try {
            holder.viewColour.setBackgroundColor(Color.parseColor(category.colour))
        } catch (_: Exception) {
            // fallback in case of invalid colour
            holder.viewColour.setBackgroundColor(Color.GRAY)
        }
        holder.buttonDeleteCategory.setOnClickListener { onDelete(id) }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<Pair<String, Category>>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
