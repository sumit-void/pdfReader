package com.example.pdfreader

import android.app.Application
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader

class ReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox resource loader
        PDFBoxResourceLoader.init(applicationContext)
    }
}
