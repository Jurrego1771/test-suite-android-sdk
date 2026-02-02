package com.example.sdkqa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying test cases in a RecyclerView.
 */
class TestCaseAdapter(
    private val testCases: List<TestCase>,
    private val onItemClick: (TestCase) -> Unit
) : RecyclerView.Adapter<TestCaseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardTestCase)
        val categoryIndicator: View = view.findViewById(R.id.categoryIndicator)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test_case, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val testCase = testCases[position]

        holder.tvCategory.text = testCase.category.displayName
        holder.tvTitle.text = testCase.title

        // Set category indicator color based on type
        val indicatorColor = when (testCase.category) {
            TestCase.Category.AUDIO -> holder.itemView.context.getColor(R.color.accent_audio)
            TestCase.Category.VIDEO -> holder.itemView.context.getColor(R.color.accent_video)
        }
        holder.categoryIndicator.setBackgroundColor(indicatorColor)

        holder.card.setOnClickListener {
            onItemClick(testCase)
        }
    }

    override fun getItemCount(): Int = testCases.size
}
