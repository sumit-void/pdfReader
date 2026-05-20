package com.example.pdfreader.ui

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfreader.R
import com.example.pdfreader.data.Bookmark
import java.io.File
import java.util.Calendar

class BookmarkAdapter(
    private var bookmarks: List<Bookmark> = emptyList(),
    private val onBookmarkClick: (Bookmark) -> Unit,
    private val onBookmarkDelete: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    fun updateBookmarks(newBookmarks: List<Bookmark>) {
        this.bookmarks = newBookmarks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(bookmarks[position])
    }

    override fun getItemCount(): Int = bookmarks.size

    inner class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val previewImage = itemView.findViewById<ImageView>(R.id.bookmark_preview)
        private val pageTitle = itemView.findViewById<TextView>(R.id.bookmark_page_title)
        private val dateText = itemView.findViewById<TextView>(R.id.bookmark_date)
        private val deleteBtn = itemView.findViewById<View>(R.id.bookmark_delete_btn)

        fun bind(bookmark: Bookmark) {
            pageTitle.text = "Page ${bookmark.pageNumber + 1}"
            
            // Format timestamp
            val cal = Calendar.getInstance().apply { timeInMillis = bookmark.timestamp }
            dateText.text = DateFormat.format("MMM dd, yyyy h:mm a", cal).toString()

            // Load visual preview
            if (!bookmark.previewPath.isNullOrEmpty()) {
                val file = File(bookmark.previewPath)
                if (file.exists()) {
                    previewImage.visibility = View.VISIBLE
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    previewImage.setImageBitmap(bitmap)
                } else {
                    previewImage.visibility = View.GONE
                }
            } else {
                previewImage.visibility = View.GONE
            }

            itemView.setOnClickListener { onBookmarkClick(bookmark) }
            deleteBtn.setOnClickListener { onBookmarkDelete(bookmark) }
        }
    }
}
