package com.example.pdfreader.ui

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfreader.R
import com.example.pdfreader.data.Book
import com.example.pdfreader.viewmodel.ImportState
import com.example.pdfreader.viewmodel.LibraryViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LibraryFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter
    private lateinit var emptyStateView: View
    private lateinit var loaderOverlay: View

    // PDF SAF Picker Launcher
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = queryFileName(uri) ?: "Imported Book"
            viewModel.importPdf(uri, displayName)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyStateView = view.findViewById(R.id.library_empty_state)
        loaderOverlay = view.findViewById(R.id.import_loader_overlay)

        // Setup Recycler view (Grid layout, 2 columns for portrait, 3 or 4 for landscape)
        val recyclerView = view.findViewById<RecyclerView>(R.id.library_recycler)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val spanCount = if (isLandscape) 4 else 2
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        
        bookAdapter = BookAdapter(
            onBookClick = { book -> openBook(book) },
            onBookLongClick = { book -> confirmDeleteBook(book) }
        )
        recyclerView.adapter = bookAdapter

        // Setup FAB
        val addFab = view.findViewById<FloatingActionButton>(R.id.library_add_fab)
        addFab.setOnClickListener {
            // Launch SAF file selector for PDFs only
            pdfPickerLauncher.launch("application/pdf")
        }

        // Observe view model data
        viewModel.books.observe(viewLifecycleOwner) { booksList ->
            bookAdapter.updateBooks(booksList)
            if (booksList.isEmpty()) {
                emptyStateView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.importState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ImportState.Loading -> {
                    loaderOverlay.visibility = View.VISIBLE
                }
                is ImportState.Success -> {
                    loaderOverlay.visibility = View.GONE
                    Toast.makeText(requireContext(), "Imported: ${state.book.title}", Toast.LENGTH_SHORT).show()
                    viewModel.resetImportState()
                }
                is ImportState.Error -> {
                    loaderOverlay.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetImportState()
                }
                else -> {
                    loaderOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun openBook(book: Book) {
        val readerFragment = ReaderFragment.newInstance(book.id)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, readerFragment)
            .addToBackStack("library")
            .commit()
    }

    private fun confirmDeleteBook(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Book")
            .setMessage("Are you sure you want to delete '${book.title}' from your shelf? This will remove all annotations and local copies.")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteBook(book)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Deleted ${book.title}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        // Strip .pdf extension if present for title display
        name?.let {
            if (it.endsWith(".pdf", ignoreCase = true)) {
                return it.substring(0, it.length - 4)
            }
        }
        return name
    }
}
