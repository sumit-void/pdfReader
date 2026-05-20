package com.example.pdfreader.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfreader.R
import com.example.pdfreader.data.Book
import java.io.File

class BookAdapter(
    private var books: List<Book> = emptyList(),
    private val onBookClick: (Book) -> Unit,
    private val onBookLongClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    fun updateBooks(newBooks: List<Book>) {
        this.books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImage = itemView.findViewById<ImageView>(R.id.book_cover)
        private val titleText = itemView.findViewById<TextView>(R.id.book_title)
        private val authorText = itemView.findViewById<TextView>(R.id.book_author)
        private val progressBadge = itemView.findViewById<TextView>(R.id.book_progress_badge)
        private val placeholderCover = itemView.findViewById<View>(R.id.placeholder_cover)
        private val placeholderTitle = itemView.findViewById<TextView>(R.id.placeholder_title)

        fun bind(book: Book) {
            titleText.text = book.title
            authorText.text = book.author ?: "Unknown Author"
            
            // Set Progress Badge
            val progressPercent = if (book.totalPages > 0) {
                ((book.lastPageRead + 1).toFloat() / book.totalPages.toFloat() * 100).toInt()
            } else {
                0
            }
            val progressClamped = Math.min(100, Math.max(0, progressPercent))
            
            if (book.lastPageRead == 0 && progressClamped <= 1) {
                progressBadge.text = "Unread"
                progressBadge.setBackgroundResource(R.drawable.badge_unread)
            } else {
                progressBadge.text = "$progressClamped% read"
                progressBadge.setBackgroundResource(R.drawable.badge_in_progress)
            }

            // Set Cover Image or Placeholder
            val coverFile = File(book.coverPath)
            if (book.coverPath.isNotEmpty() && coverFile.exists()) {
                placeholderCover.visibility = View.GONE
                coverImage.visibility = View.VISIBLE
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                coverImage.setImageBitmap(bitmap)
            } else {
                coverImage.visibility = View.GONE
                placeholderCover.visibility = View.VISIBLE
                placeholderTitle.text = book.title
            }

            itemView.setOnClickListener { onBookClick(book) }
            itemView.setOnLongClickListener {
                onBookLongClick(book)
                true
            }
        }
    }
}
