package com.example.pdfreader.ui

import android.os.Bundle
import android.text.TextPaint
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.pdfreader.R
import com.example.pdfreader.data.Book
import com.example.pdfreader.data.Bookmark
import com.example.pdfreader.data.Chapter
import com.example.pdfreader.data.SearchResult
import com.example.pdfreader.ui.view.BookPageTransformer
import com.example.pdfreader.viewmodel.ReaderViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReaderFragment : Fragment() {

    companion object {
        private const val ARG_BOOK_ID = "book_id"

        fun newInstance(bookId: Long): ReaderFragment {
            return ReaderFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BOOK_ID, bookId)
                }
            }
        }
    }

    private val viewModel: ReaderViewModel by viewModels()
    private var bookId: Long = -1

    // View bindings
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var appBar: View
    private lateinit var bottomBar: View
    private lateinit var toolbar: Toolbar
    private lateinit var txtPageProgress: TextView
    private lateinit var btnTimer: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnBookmark: ImageView

    // Search Panel Views
    private lateinit var searchPanel: View
    private lateinit var searchInput: EditText
    private lateinit var searchProgress: ProgressBar
    private lateinit var searchResultsRecycler: RecyclerView
    private lateinit var searchResultAdapter: SearchResultAdapter

    // Drawer Views
    private lateinit var drawerTabs: TabLayout
    private lateinit var tocRecycler: RecyclerView
    private lateinit var bookmarksRecycler: RecyclerView
    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var bookmarkAdapter: BookmarkAdapter

    // State parameters
    private var controlsVisible = true
    private var pageAdapter: ReaderPagerAdapter? = null
    
    // Auto Scroll Timer parameters
    private var autoScrollJob: Job? = null
    private var timerSettingSeconds = 0 // 0 = Off, 15, 30, 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookId = arguments?.getLong(ARG_BOOK_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupViewPager()
        setupDrawer()
        setupSearch()
        observeViewModel()

        if (bookId != -1L) {
            viewModel.loadBook(bookId)
        }
    }

    private fun initViews(view: View) {
        drawerLayout = view.findViewById(R.id.reader_drawer_layout)
        viewPager = view.findViewById(R.id.reader_viewpager)
        appBar = view.findViewById(R.id.reader_appbar)
        bottomBar = view.findViewById(R.id.reader_bottom_bar)
        toolbar = view.findViewById(R.id.reader_toolbar)
        txtPageProgress = view.findViewById(R.id.txt_page_progress)
        btnTimer = view.findViewById(R.id.btn_timer)
        seekBar = view.findViewById(R.id.reader_seekbar)
        btnBookmark = view.findViewById(R.id.btn_bookmark)

        searchPanel = view.findViewById(R.id.search_panel)
        searchInput = view.findViewById(R.id.search_input)
        searchProgress = view.findViewById(R.id.search_progress)
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler)

        drawerTabs = view.findViewById(R.id.drawer_tabs)
        tocRecycler = view.findViewById(R.id.drawer_toc_recycler)
        bookmarksRecycler = view.findViewById(R.id.drawer_bookmarks_recycler)

        // Viewport dimensions feed back loop
        viewPager.post {
            val width = viewPager.width
            val height = viewPager.height
            val testPaint = TextPaint().apply {
                density = resources.displayMetrics.density
            }
            viewModel.setViewportSize(width, height, testPaint)
        }

        // Seekbar paging listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewPager.currentItem = progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Timer controller
        btnTimer.setOnClickListener {
            cycleTimerSettings()
        }
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_drawer).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        view.findViewById<View>(R.id.btn_search).setOnClickListener {
            toggleSearchPanel()
        }

        btnBookmark.setOnClickListener {
            val currentPos = viewPager.currentItem
            val mappedPage = getActualPageIndex(currentPos)
            viewModel.toggleBookmark(mappedPage)
        }

        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            showSettingsBottomSheet()
        }
    }

    private fun setupViewPager() {
        // Apply our custom realistic 3D fold page transformer
        viewPager.setPageTransformer(BookPageTransformer())

        pageAdapter = ReaderPagerAdapter(
            onPageClick = { toggleControls() }
        )
        viewPager.adapter = pageAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setCurrentPage(position)
                seekBar.progress = position
                updateProgressLabel(position)
                updateBookmarkIcon(position)
                
                // Restart timer auto-scroll countdown on page change
                resetAutoScrollTimer()
            }
        })
    }

    private fun setupDrawer() {
        // Tabs listener
        drawerTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    tocRecycler.visibility = View.VISIBLE
                    bookmarksRecycler.visibility = View.GONE
                } else {
                    tocRecycler.visibility = View.GONE
                    bookmarksRecycler.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Chapters TOC Recycler setup
        tocRecycler.layoutManager = LinearLayoutManager(requireContext())
        chapterAdapter = ChapterAdapter { chapter ->
            navigateToChapter(chapter)
        }
        tocRecycler.adapter = chapterAdapter

        // Bookmarks Recycler setup
        bookmarksRecycler.layoutManager = LinearLayoutManager(requireContext())
        bookmarkAdapter = BookmarkAdapter(
            onBookmarkClick = { bookmark ->
                navigateToBookmark(bookmark)
            },
            onBookmarkDelete = { bookmark ->
                viewModel.toggleBookmark(bookmark.pageNumber)
            }
        )
        bookmarksRecycler.adapter = bookmarkAdapter
    }

    private fun setupSearch() {
        searchResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        searchResultAdapter = SearchResultAdapter { searchResult ->
            navigateToPage(searchResult.pageNumber)
            toggleSearchPanel()
        }
        searchResultsRecycler.adapter = searchResultAdapter

        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.performSearch(query)
                }
                true
            } else {
                false
            }
        }

        view.findViewById<View>(R.id.search_close).setOnClickListener {
            toggleSearchPanel()
        }
    }

    private fun observeViewModel() {
        viewModel.book.observe(viewLifecycleOwner) { book ->
            book?.let {
                toolbar.title = it.title
            }
        }

        viewModel.paginatedPages.observe(viewLifecycleOwner) { pages ->
            val pageCount = pages.size
            if (pageCount > 0) {
                seekBar.max = pageCount - 1
                pageAdapter?.updateData(
                    pages,
                    isLandscape(),
                    viewModel.theme.value ?: "sepia",
                    viewModel.fontFamily.value ?: "lora",
                    viewModel.fontSize.value ?: 18f,
                    viewModel.marginType.value ?: "standard",
                    viewModel.lineSpacing.value ?: 1.2f
                )

                // Restore page position
                val lastPage = viewModel.currentPage.value ?: 0
                if (lastPage < pageCount) {
                    viewPager.setCurrentItem(lastPage, false)
                    seekBar.progress = lastPage
                    updateProgressLabel(lastPage)
                }
            }
        }

        viewModel.bookmarks.observe(viewLifecycleOwner) { bookmarksList ->
            bookmarkAdapter.updateBookmarks(bookmarksList)
            updateBookmarkIcon(viewPager.currentItem)
        }

        viewModel.chapters.observe(viewLifecycleOwner) { tocChapters ->
            chapterAdapter.updateChapters(tocChapters)
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { searchList ->
            searchResultAdapter.updateData(searchList)
        }

        viewModel.isSearching.observe(viewLifecycleOwner) { searching ->
            searchProgress.visibility = if (searching) View.VISIBLE else View.GONE
        }
    }

    private fun isLandscape(): Boolean {
        return resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }

    private fun getActualPageIndex(viewPagerPosition: Int): Int {
        if (!isLandscape()) return viewPagerPosition
        if (viewPagerPosition == 0) return 0
        return 2 * viewPagerPosition - 1
    }

    private fun navigateToPage(pageIndex: Int) {
        val count = viewModel.paginatedPages.value?.size ?: 0
        if (count == 0) return
        
        val pos = if (isLandscape()) {
            if (pageIndex == 0) 0 else (pageIndex + 1) / 2
        } else {
            pageIndex
        }
        viewPager.currentItem = Math.min(pos, count - 1)
    }

    private fun navigateToChapter(chapter: Chapter) {
        drawerLayout.closeDrawer(GravityCompat.START)
        navigateToPage(chapter.pageIndex)
    }

    private fun navigateToBookmark(bookmark: Bookmark) {
        drawerLayout.closeDrawer(GravityCompat.START)
        navigateToPage(bookmark.pageNumber)
    }

    private fun updateProgressLabel(position: Int) {
        val pagesList = viewModel.paginatedPages.value ?: return
        val total = pagesList.size
        if (total == 0) return

        if (isLandscape()) {
            if (position == 0) {
                txtPageProgress.text = "Page 1 of $total"
            } else {
                val left = 2 * position
                val right = left + 1
                if (right <= total) {
                    txtPageProgress.text = "Pages $left-$right of $total"
                } else {
                    txtPageProgress.text = "Page $left of $total"
                }
            }
        } else {
            txtPageProgress.text = "Page ${position + 1} of $total"
        }
    }

    private fun updateBookmarkIcon(position: Int) {
        val actualPage = getActualPageIndex(position)
        val bookmarked = viewModel.bookmarks.value?.any { it.pageNumber == actualPage } ?: false
        if (bookmarked) {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }

    private fun toggleControls() {
        controlsVisible = !controlsVisible
        val translationYTop = if (controlsVisible) 0f else -appBar.height.toFloat()
        val translationYBottom = if (controlsVisible) 0f else bottomBar.height.toFloat()

        appBar.animate().translationY(translationYTop).setDuration(250).start()
        bottomBar.animate().translationY(translationYBottom).setDuration(250).start()
    }

    private fun toggleSearchPanel() {
        if (searchPanel.visibility == View.VISIBLE) {
            searchPanel.visibility = View.GONE
            viewModel.clearSearch()
            searchInput.setText("")
        } else {
            searchPanel.visibility = View.VISIBLE
            searchInput.requestFocus()
        }
    }

    // Settings sheet dialog binding controls
    private fun showSettingsBottomSheet() {
        val context = requireContext()
        val dialog = BottomSheetDialog(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        dialog.setContentView(dialogView)

        // Bind theme selectors
        val sepiaBtn = dialogView.findViewById<Button>(R.id.btn_theme_sepia)
        val lightBtn = dialogView.findViewById<Button>(R.id.btn_theme_light)
        val darkBtn = dialogView.findViewById<Button>(R.id.btn_theme_dark)

        sepiaBtn.setOnClickListener {
            viewModel.theme.value = "sepia"
            updatePreferences()
            dialog.dismiss()
        }
        lightBtn.setOnClickListener {
            viewModel.theme.value = "light"
            updatePreferences()
            dialog.dismiss()
        }
        darkBtn.setOnClickListener {
            viewModel.theme.value = "dark"
            updatePreferences()
            dialog.dismiss()
        }

        // Bind fonts
        val loraBtn = dialogView.findViewById<Button>(R.id.btn_font_lora)
        val playfairBtn = dialogView.findViewById<Button>(R.id.btn_font_playfair)
        val sansBtn = dialogView.findViewById<Button>(R.id.btn_font_sans)

        loraBtn.setOnClickListener {
            viewModel.fontFamily.value = "lora"
            updatePreferences()
        }
        playfairBtn.setOnClickListener {
            viewModel.fontFamily.value = "playfair"
            updatePreferences()
        }
        sansBtn.setOnClickListener {
            viewModel.fontFamily.value = "sans"
            updatePreferences()
        }

        // Size increment/decrement
        val txtFontSize = dialogView.findViewById<TextView>(R.id.txt_font_size)
        txtFontSize.text = viewModel.fontSize.value?.toInt().toString()

        dialogView.findViewById<Button>(R.id.btn_size_decrease).setOnClickListener {
            val current = viewModel.fontSize.value ?: 18f
            if (current > 12f) {
                viewModel.fontSize.value = current - 2f
                txtFontSize.text = viewModel.fontSize.value?.toInt().toString()
                updatePreferences()
            }
        }
        dialogView.findViewById<Button>(R.id.btn_size_increase).setOnClickListener {
            val current = viewModel.fontSize.value ?: 18f
            if (current < 36f) {
                viewModel.fontSize.value = current + 2f
                txtFontSize.text = viewModel.fontSize.value?.toInt().toString()
                updatePreferences()
            }
        }

        // Margins selection
        val marginNarrow = dialogView.findViewById<ImageButton>(R.id.btn_margin_narrow)
        val marginStandard = dialogView.findViewById<ImageButton>(R.id.btn_margin_standard)
        val marginWide = dialogView.findViewById<ImageButton>(R.id.btn_margin_wide)

        marginNarrow.setOnClickListener {
            viewModel.marginType.value = "narrow"
            updatePreferences()
        }
        marginStandard.setOnClickListener {
            viewModel.marginType.value = "standard"
            updatePreferences()
        }
        marginWide.setOnClickListener {
            viewModel.marginType.value = "wide"
            updatePreferences()
        }

        // Brightness Seekbar binding
        val brightnessSeek = dialogView.findViewById<SeekBar>(R.id.brightness_seekbar)
        brightnessSeek.max = 100
        val currBrightness = viewModel.brightness.value ?: 0.8f
        brightnessSeek.progress = (currBrightness * 100).toInt()

        brightnessSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                viewModel.brightness.value = value
                applyWindowBrightness(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialog.show()
    }

    private fun updatePreferences() {
        // Redraw page lists based on settings
        val pages = viewModel.paginatedPages.value ?: return
        pageAdapter?.updateData(
            pages,
            isLandscape(),
            viewModel.theme.value ?: "sepia",
            viewModel.fontFamily.value ?: "lora",
            viewModel.fontSize.value ?: 18f,
            viewModel.marginType.value ?: "standard",
            viewModel.lineSpacing.value ?: 1.2f
        )
        viewModel.updateReadingPreferences()
    }

    private fun applyWindowBrightness(value: Float) {
        val window = activity?.window ?: return
        val layoutParams = window.attributes
        layoutParams.screenBrightness = value
        window.attributes = layoutParams
    }

    // Auto-scroll / Reading timer controller
    private fun cycleTimerSettings() {
        timerSettingSeconds = when (timerSettingSeconds) {
            0 -> 15
            15 -> 30
            30 -> 60
            else -> 0
        }
        
        if (timerSettingSeconds == 0) {
            btnTimer.text = "Timer: Off"
            stopAutoScrollTimer()
            Toast.makeText(requireContext(), "Auto-scroll turned off", Toast.LENGTH_SHORT).show()
        } else {
            btnTimer.text = "Timer: ${timerSettingSeconds}s"
            startAutoScrollTimer()
            Toast.makeText(requireContext(), "Auto-scroll page every $timerSettingSeconds seconds", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAutoScrollTimer() {
        stopAutoScrollTimer()
        if (timerSettingSeconds == 0) return
        autoScrollJob = lifecycleScope.launch {
            while (true) {
                delay(timerSettingSeconds * 1000L)
                val current = viewPager.currentItem
                val count = viewPager.adapter?.itemCount ?: 0
                if (current < count - 1) {
                    viewPager.currentItem = current + 1
                } else {
                    cycleTimerSettings() // Turn off at end of book
                    break
                }
            }
        }
    }

    private fun stopAutoScrollTimer() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    private fun resetAutoScrollTimer() {
        if (autoScrollJob != null) {
            startAutoScrollTimer()
        }
    }

    override fun onDestroyView() {
        stopAutoScrollTimer()
        super.onDestroyView()
    }
}

// Inner Recycler Adapter for Text Search Results
class SearchResultAdapter(
    private var results: List<SearchResult> = emptyList(),
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchViewHolder>() {

    fun updateData(newResults: List<SearchResult>) {
        this.results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1 = itemView.findViewById<TextView>(android.R.id.text1)
        private val text2 = itemView.findViewById<TextView>(android.R.id.text2)

        fun bind(result: SearchResult) {
            text1.text = "Page ${result.pageNumber + 1}"
            text2.text = result.snippet.trim()
            
            // Set text styling
            text1.setTextColor(itemView.context.getColor(R.color.accent_gold))
            text1.setTypeface(null, android.graphics.Typeface.BOLD)
            text2.setTextColor(itemView.context.getColor(R.color.sepia_text))
            
            itemView.setOnClickListener { onItemClick(result) }
        }
    }
}
