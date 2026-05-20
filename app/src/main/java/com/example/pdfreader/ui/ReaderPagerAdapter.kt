package com.example.pdfreader.ui

import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfreader.R

class ReaderPagerAdapter(
    private var pages: List<String> = emptyList(),
    private var isLandscape: Boolean = false,
    private var themeMode: String = "sepia", // sepia, dark, light
    private var fontType: String = "lora", // lora, playfair, sans
    private var sizeSp: Float = 18f,
    private var marginType: String = "standard", // narrow, standard, wide
    private var lineSpacingMultiplier: Float = 1.2f,
    private val onPageClick: () -> Unit = {}
) : RecyclerView.Adapter<ReaderPagerAdapter.PageViewHolder>() {

    fun updateData(
        newPages: List<String>,
        landscape: Boolean,
        theme: String,
        font: String,
        size: Float,
        margin: String,
        spacing: Float
    ) {
        this.pages = newPages
        this.isLandscape = landscape
        this.themeMode = theme
        this.fontType = font
        this.sizeSp = size
        this.marginType = margin
        this.lineSpacingMultiplier = spacing
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (pages.isEmpty()) return 0
        return if (isLandscape) {
            pages.size / 2 + 1
        } else {
            pages.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reader_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootLayout = itemView.findViewById<ViewGroup>(R.id.page_root)
        private val leftCard = itemView.findViewById<CardView>(R.id.left_page_card)
        private val rightCard = itemView.findViewById<CardView>(R.id.right_page_card)
        private val leftText = itemView.findViewById<TextView>(R.id.left_page_text)
        private val rightText = itemView.findViewById<TextView>(R.id.right_page_text)
        private val leftPageNum = itemView.findViewById<TextView>(R.id.left_page_number)
        private val rightPageNum = itemView.findViewById<TextView>(R.id.right_page_number)
        private val spineCrease = itemView.findViewById<View>(R.id.spine_crease)

        init {
            itemView.setOnClickListener { onPageClick() }
            leftCard.setOnClickListener { onPageClick() }
            rightCard.setOnClickListener { onPageClick() }
        }

        fun bind(position: Int) {
            val context = itemView.context

            // Apply global page container theme (Sepia, Dark, Light)
            val colors = getThemeColors(themeMode, context)
            rootLayout.setBackgroundColor(colors.bgColor)
            
            // Set up layout structures for portrait vs landscape
            if (isLandscape) {
                // Show dual page spread
                rightCard.visibility = View.VISIBLE
                spineCrease.visibility = View.VISIBLE
                
                // Adjust weights
                val leftParams = leftCard.layoutParams as LinearLayout.LayoutParams
                leftParams.width = 0
                leftParams.weight = 1f
                leftCard.layoutParams = leftParams

                val rightParams = rightCard.layoutParams as LinearLayout.LayoutParams
                rightParams.width = 0
                rightParams.weight = 1f
                rightCard.layoutParams = rightParams

                // Bind text: Position 0 is blank page on left, page 0 on right
                if (position == 0) {
                    leftCard.visibility = View.INVISIBLE // blank page
                    bindPageText(rightText, rightPageNum, 0, colors)
                } else {
                    leftCard.visibility = View.VISIBLE
                    val leftPageIndex = 2 * position - 1
                    val rightPageIndex = 2 * position
                    
                    bindPageText(leftText, leftPageNum, leftPageIndex, colors)
                    
                    if (rightPageIndex < pages.size) {
                        rightCard.visibility = View.VISIBLE
                        bindPageText(rightText, rightPageNum, rightPageIndex, colors)
                    } else {
                        rightCard.visibility = View.INVISIBLE // blank trailing page
                    }
                }
            } else {
                // Show single page (portrait)
                rightCard.visibility = View.GONE
                spineCrease.visibility = View.GONE
                leftCard.visibility = View.VISIBLE

                val leftParams = leftCard.layoutParams as LinearLayout.LayoutParams
                leftParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                leftParams.weight = 0f
                leftCard.layoutParams = leftParams

                bindPageText(leftText, leftPageNum, position, colors)
            }
        }

        private fun bindPageText(
            textView: TextView,
            pageNumberView: TextView,
            pageIndex: Int,
            colors: ThemeColors
        ) {
            if (pageIndex < 0 || pageIndex >= pages.size) {
                textView.text = ""
                pageNumberView.text = ""
                return
            }
            
            val context = textView.context

            // Apply content colors
            textView.setTextColor(colors.textColor)
            pageNumberView.setTextColor(colors.pageNumColor)
            (textView.parent as? CardView)?.setCardBackgroundColor(colors.pageBgColor)

            // Bind content
            textView.text = pages[pageIndex]
            pageNumberView.text = (pageIndex + 1).toString()

            // Apply typeface
            val typeface = getTypeface(fontType, context)
            textView.typeface = typeface
            pageNumberView.typeface = typeface

            // Apply font size
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)

            // Apply line spacing
            textView.setLineSpacing(0f, lineSpacingMultiplier)

            // Apply dynamic margins padding
            val paddingDp = getMarginPadding(marginType)
            val paddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                paddingDp.toFloat(),
                context.resources.displayMetrics
            ).toInt()
            textView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }
    }

    private fun getMarginPadding(type: String): Int {
        return when (type) {
            "narrow" -> 16
            "wide" -> 36
            else -> 24 // standard
        }
    }

    private fun getTypeface(font: String, context: android.content.Context): Typeface? {
        return when (font) {
            "playfair" -> ResourcesCompat.getFont(context, R.font.playfair_display)
            "sans" -> Typeface.SANS_SERIF
            else -> ResourcesCompat.getFont(context, R.font.lora) // lora
        }
    }

    data class ThemeColors(
        val bgColor: Int,
        val pageBgColor: Int,
        val textColor: Int,
        val pageNumColor: Int
    )

    private fun getThemeColors(mode: String, context: android.content.Context): ThemeColors {
        return when (mode) {
            "dark" -> ThemeColors(
                bgColor = context.getColor(R.color.dark_window_bg),
                pageBgColor = context.getColor(R.color.dark_page_bg),
                textColor = context.getColor(R.color.dark_text),
                pageNumColor = context.getColor(R.color.dark_text_secondary)
            )
            "light" -> ThemeColors(
                bgColor = context.getColor(R.color.light_window_bg),
                pageBgColor = context.getColor(R.color.light_page_bg),
                textColor = context.getColor(R.color.light_text),
                pageNumColor = context.getColor(R.color.light_text_secondary)
            )
            else -> ThemeColors( // sepia
                bgColor = context.getColor(R.color.sepia_window_bg),
                pageBgColor = context.getColor(R.color.sepia_page_bg),
                textColor = context.getColor(R.color.sepia_text),
                pageNumColor = context.getColor(R.color.sepia_text_secondary)
            )
        }
    }
}
