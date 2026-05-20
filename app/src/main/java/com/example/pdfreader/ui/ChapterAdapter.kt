package com.example.pdfreader.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfreader.R
import com.example.pdfreader.data.Chapter

class ChapterAdapter(
    private var chapters: List<Chapter> = emptyList(),
    private val onChapterClick: (Chapter) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    fun updateChapters(newChapters: List<Chapter>) {
        this.chapters = newChapters
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(chapters[position])
    }

    override fun getItemCount(): Int = chapters.size

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText = itemView.findViewById<TextView>(R.id.chapter_title)
        private val pageText = itemView.findViewById<TextView>(R.id.chapter_page)

        fun bind(chapter: Chapter) {
            titleText.text = chapter.title.trim()
            pageText.text = "p. ${chapter.pageIndex + 1}"
            
            itemView.setOnClickListener { onChapterClick(chapter) }
        }
    }
}
