# 📖 How Paperback Works — A Beginner's Guide
## Written by Sumit Kumar

---

### What is this app?

Paperback is a beautiful, full-featured PDF reader for Android built with Kotlin and Jetpack Compose. Think of it like a personal digital bookshelf on your phone — you import PDF files, and the app lets you read them with realistic page-curl animations, customizable themes (Light, Dark, Sepia, AMOLED), bookmarks, highlights, text-to-speech read-aloud, AI-powered page summaries via Gemini, reading streak tracking, and even biometric app-lock security. It's designed to make reading PDFs feel as natural and enjoyable as reading a real paper book.

---

### The Big Picture — How the pieces fit together

```
┌─────────────────────────────────────────────────────────────────┐
│                         📱 USER INTERFACE                       │
│  (Jetpack Compose Screens: LibraryScreen, ReaderScreen, etc.)   │
└──────────────────────────────┬──────────────────────────────────┘
                               │ observes StateFlow
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        🧠 VIEWMODEL                             │
│   (LibraryViewModel, ReaderViewModel, SettingsViewModel, etc.)  │
│   Holds UI state, handles user actions, talks to UseCases       │
└──────────────────────────────┬──────────────────────────────────┘
                               │ calls suspend functions
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        📦 USE CASES                             │
│   (ImportPdfUseCase, ToggleBookmarkUseCase, GetBooksUseCase…)   │
│   One job per class. Clean, testable business logic.            │
└──────────────────────────────┬──────────────────────────────────┘
                               │ delegates to
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       🗄️ REPOSITORY                             │
│  (BookRepositoryImpl, BookmarkRepositoryImpl, etc.)             │
│  Single source of truth. Talks to Room DB, files, and APIs.    │
└─────────┬───────────────────┬───────────────────┬──────────────┘
          │                   │                   │
          ▼                   ▼                   ▼
   ┌────────────┐     ┌─────────────┐     ┌─────────────┐
   │  🗃️ Room   │     │ 📄 PDF File │     │ ☁️ Gemini   │
   │  Database  │     │  System     │     │  AI API     │
   │  (SQLite)  │     │ (PdfRenderer│     │ (Summaries) │
   │            │     │  + PDFBox)  │     │             │
   └────────────┘     └─────────────┘     └─────────────┘
```

Here's what each layer does, in plain English:

**UI Layer (Screens)** — This is what the user actually sees and touches. Each screen is a `@Composable` function written in Jetpack Compose. For example, `LibraryScreen` shows your book grid, and `ReaderScreen` displays the PDF pages with controls. The UI *never* talks to the database directly — it just watches the ViewModel's state and redraws itself whenever the state changes.

**ViewModel Layer** — The ViewModel is the "brain" behind each screen. It holds all the data the screen needs (current page number, is this page bookmarked?, list of books, etc.) inside a `StateFlow`. When the user taps a button, the screen calls a function on the ViewModel (like `toggleBookmark()`), and the ViewModel updates the state. ViewModels survive screen rotations, so you don't lose your place in a book if you turn your phone sideways.

**UseCase Layer** — Each UseCase is a tiny, focused class that does *one thing*. `ImportPdfUseCase` imports a PDF. `ToggleBookmarkUseCase` toggles a bookmark. That's it. This makes the code easy to test and easy to understand. The ViewModel calls the UseCase, and the UseCase calls the Repository.

**Repository Layer** — Repositories are the "middle managers" that know *where* data lives. `BookRepositoryImpl` knows how to copy a PDF file into app storage, extract its metadata with PDFBox, generate a cover thumbnail with `PdfRenderer`, and save the book record to Room. The UseCase doesn't care about *how* this happens — it just calls `bookRepository.importPdf(uri)` and gets back a `Result<Book>`.

**Data Sources** — At the very bottom sit the actual storage systems: Room (a SQLite database wrapped in Kotlin-friendly APIs) for structured data like books, bookmarks, highlights, and reading sessions; the file system for PDF files and cover images; DataStore for user preferences like theme and brightness; and the Gemini API for AI-powered page summaries.

---

### Screen by Screen — What each screen does and which files control it

| Screen | Main File | ViewModel | What it does in plain English |
|---|---|---|---|
| **Splash** | `SplashScreen.kt` | `SplashViewModel.kt` | Shows the Paperback logo with a bouncy scale animation on a warm cream background for 1.5 seconds. Checks if there's a last-opened book so it can jump straight to the reader. |
| **Library** | `LibraryScreen.kt` | `LibraryViewModel.kt` | Your home screen! Shows all imported books in a grid or list view with cover thumbnails, progress badges, and a search bar. Has a "Daily Goal" streak card at the top. Tap a book to open it. Long-press to delete. Tap the **+** FAB to import a new PDF. |
| **Reader** | `ReaderScreen.kt` | `ReaderViewModel.kt` | The heart of the app. Displays PDF pages using a 3D page-curl effect (`CurlPageEffect`). Has a top bar with bookmark, TTS, TOC, and settings buttons. Has a bottom bar with a page slider. Supports pinch-to-zoom, theme overlays (Dark/Sepia/AMOLED), and a Gemini AI "Summarize" FAB. |
| **Bookmarks** | `BookmarksScreen.kt` | *(uses ReaderViewModel via nav args)* | Lists all bookmarks for a specific book. Tap one to jump directly to that page in the reader. |
| **Highlights** | `HighlightsScreen.kt` | *(uses nav args)* | Shows all text highlights for a book. Tap one to jump to that page, or tap the share button to open the Quote Card Editor. |
| **Quote Card Editor** | `QuoteCardEditorScreen.kt` | *(uses nav args)* | Lets you design a beautiful shareable quote card from a highlighted passage. Think Instagram-story-style quote images. |
| **Table of Contents** | `TableOfContentsScreen.kt` | *(uses nav args)* | Displays the PDF's table of contents (extracted from the PDF structure). Tap a chapter to jump to it. |
| **Settings** | `SettingsScreen.kt` | `SettingsViewModel.kt` | Lets you change theme (Light/Dark/Sepia/AMOLED), page-turn style (Curl/Slide/Fade/Scroll), reading direction (LTR/RTL), brightness, screen-awake toggle, biometric app-lock, and screenshot blocking. |
| **Reading Stats** | `ReadingStatsScreen.kt` | *(uses ReadingStatsRepository)* | Shows your reading analytics: total time read, pages read, books finished, reading streak history, and daily goal progress. |

**Navigation**: All routes are defined as a sealed class in `Screen.kt`. The `PaperbackNavGraph.kt` sets up the `NavHost` with smooth fade+slide transitions between screens. The Reader screen uses a deep link pattern (`paperback://reader/{bookId}`) so the home-screen widget can open a book directly.

---

### How a PDF gets imported — Step by step

Let's follow the journey of a PDF from the moment you tap the **+** button to when it appears in your library:

1. **User taps the "+" FAB** on `LibraryScreen`. This triggers Android's file picker via `ActivityResultContracts.OpenDocument()` with a MIME filter of `application/pdf`.

2. **User selects a PDF file.** Android returns a `Uri` pointing to the chosen file.

3. **`LibraryViewModel.importPdf(uri)`** is called. The ViewModel sets `isImporting = true` in the UI state (you'll see a spinner) and calls `ImportPdfUseCase(uri)`.

4. **`ImportPdfUseCase.invoke(uri)`** — this is a one-liner! It just delegates to `bookRepository.importPdf(uri)`. The UseCase exists to keep the ViewModel thin and to make the import logic independently testable.

5. **`BookRepositoryImpl.importPdf(uri)`** — this is where the heavy lifting happens, all on the IO dispatcher:
   - **Copy the file**: Creates a `books/` directory in internal storage, generates a unique filename (`book_<timestamp>.pdf`), and copies the file from the Uri using `contentResolver.openInputStream()`.
   - **Extract metadata**: Opens the copied PDF with **PDFBox** (`PDDocument.load()`) to read the title, author, and page count from the PDF's metadata. If PDFBox fails (some PDFs are tricky), it falls back to Android's `PdfRenderer` to at least get the page count.
   - **Generate cover thumbnail**: Opens page 0 with `PdfRenderer`, renders it at 2× scale into a `Bitmap`, saves it as a PNG to a `covers/` directory, and returns the file path.
   - **Check for duplicates**: Queries the Room database to see if a book with this file path already exists.
   - **Save to database**: Creates a `BookEntity` with all the metadata and calls `bookDao.insertBook()`. Room returns the auto-generated ID.
   - **Return `Result.success(book)`**.

6. **Back in `LibraryViewModel`**: The `Result` is unfolded. On success, it sets `importedBookId` in the state. A `LaunchedEffect` in `LibraryScreen` watches for this and automatically navigates to the Reader screen to open the newly imported book.

7. **The book appears in the library grid** because `LibraryViewModel` is already collecting `getBooksUseCase()` which flows from `bookDao.getAllBooks()` — so when the new row is inserted, Room's `Flow` emits the updated list, and the grid recomposes with the new book card.

---

### How a page is displayed — Step by step

When you're reading a book and flip to a new page, here's what happens under the hood:

1. **`ReaderScreen` renders a `CurlPageEffect` composable.** This is a custom composable in `CurlPageEffect.kt` that handles the 3D page-turning animation. It manages the page swipe gestures and calls `onPageChanged(newPage)` when a page flip completes.

2. **`ReaderViewModel.goToPage(page)`** is called. This:
   - Validates the page number (clamps it between 0 and `totalPages - 1`)
   - Stops any active TTS playback
   - Updates the UI state with the new `currentPage`
   - Emits the page number to `progressSaveFlow` (a debounced `MutableSharedFlow` that saves progress to the database every 500ms, but only if the user has moved ≥3 pages since the last save — smart batching to avoid excessive database writes!)
   - Calls `renderPage(page)`

3. **`ReaderViewModel.renderPage(page)`** calls `pdfRenderCache.renderPage(pageIndex, screenWidth, theme)`.

4. **`PdfRenderCache.renderPage()`** — this is a singleton with a `Mutex` for thread safety:
   - **Check the LruCache** (up to 10 bitmaps, keyed by `filePath_pageIndex_screenWidth_themeName`). If the bitmap is already cached and not recycled, return it immediately. This makes flipping back to recently viewed pages instant.
   - **Open the page**: `currentRenderer.openPage(pageIndex)` using Android's `PdfRenderer`.
   - **Calculate dimensions**: Width = `screenWidth × 2` (2× scale for crisp rendering). Height is calculated proportionally from the PDF page's aspect ratio.
   - **Create a Bitmap**: `Bitmap.createBitmap(width, height, ARGB_8888)`.
   - **Render**: `page.render(bitmap, null, null, RENDER_MODE_FOR_DISPLAY)`. This is where Android's built-in PDF renderer rasterizes the vector PDF page into pixels.
   - **Apply theme filter**: If the theme is Dark, Sepia, or AMOLED, `PageColorFilter.getColorMatrix(theme)` returns a `ColorMatrix` that inverts colors, adds a warm tint, etc. A new bitmap is created, drawn through a `Paint` with a `ColorMatrixColorFilter`, and the original is recycled.
   - **Cache the result** and return it.

5. **Back in the composable**, the bitmap is converted to an `ImageBitmap` via `.asImageBitmap()` and displayed inside a Compose `Image` composable. The `CurlPageEffect` wraps this in its 3D curl animation.

6. **Theme overlay**: On top of the rendered page, `ReaderContent` draws semi-transparent overlay boxes with animated cross-fade when the theme changes (e.g., switching from Light to Sepia smoothly fades the tint color).

---

### How bookmarks work — Step by step

Let's trace what happens when you tap the bookmark icon while reading:

1. **User taps the bookmark icon** in the Reader's top bar. The `IconButton`'s `onClick` triggers `onToggleBookmark()`, which calls `ReaderViewModel.toggleBookmark()`. There's also a satisfying spring-bounce scale animation on the icon (`bookmarkScale` animates 1.0 → 1.3 → 1.0).

2. **`ReaderViewModel.toggleBookmark()`** launches a coroutine and calls `bookmarkRepository.toggleBookmark(bookId, currentPage)`.

3. **`BookmarkRepositoryImpl.toggleBookmark(bookId, page)`** checks if a bookmark already exists for this page:
   - Calls `bookmarkDao.getBookmarkForPage(bookId, page)`.
   - If a bookmark exists → **deletes it** via `bookmarkDao.deleteBookmark(existing)` and returns `false`.
   - If no bookmark exists → **inserts one** via `bookmarkDao.insertBookmark(BookmarkEntity(...))` and returns `true`.

4. **The UI updates automatically!** Here's the magic: back when the page was loaded, `ReaderViewModel.observeBookmark(page)` started collecting `bookmarkRepository.isPageBookmarked(bookId, page)`. This returns a `Flow<Boolean>` from Room. When the bookmark row is inserted or deleted, Room automatically emits a new value through this Flow. The ViewModel updates `isBookmarked` in the `StateFlow`, and Compose recomposes — the icon switches between `Icons.Filled.Bookmark` (filled, colored) and `Icons.Filled.BookmarkBorder` (outline).

5. **No manual refresh needed.** This is the power of reactive programming: the data flows from Room → Repository → ViewModel → UI, and every change propagates automatically.

---

### Key concepts explained simply

#### What is MVVM? 🍳 *The Cooking Analogy*

Imagine you're running a restaurant:

- **The Model** is your pantry and recipe book. It stores all the ingredients (data) and the instructions for preparing dishes (business logic). In Paperback, this is the `domain/` and `data/` packages — Room entities, repositories, use cases.

- **The ViewModel** is the head chef. Customers (the UI) never go into the kitchen. Instead, they tell the chef what they want ("I'd like to bookmark this page"), and the chef handles it, updates the order board, and puts the finished plate on the counter. In Paperback, `ReaderViewModel` is the chef for the reader screen. It holds a `StateFlow<ReaderUiState>` (the order board) that the UI watches.

- **The View** is the waiter and the dining room. It presents the food beautifully to the customer and carries their requests back to the chef. In Paperback, `ReaderScreen.kt` is the View — it *only* knows how to display things and forward taps. It never touches the database or files directly.

**Why bother?** Because you can fire the waiter and hire a new one (completely redesign the UI) without changing a single recipe. You can also test the chef's logic without needing an actual dining room (unit tests without Android emulators).

---

#### What is Hilt? 🏭 *The Vending Machine Analogy*

Imagine every class in your app needs tools to do its job. `ReaderViewModel` needs a `BookRepository`, a `BookmarkRepository`, a `PdfRenderCache`, and `UserPreferences`. Without Hilt, the ViewModel would have to build each of those itself — and each of *those* needs a `BookDao`, which needs a `PaperbackDatabase`, which needs a `Context`… it's like building the entire factory just to get a candy bar.

**Hilt is a smart vending machine.** You press the button labeled "ReaderViewModel" and it pops out a fully assembled ViewModel with all its dependencies already plugged in. You just annotate your class with `@HiltViewModel` and list what it needs in the `@Inject constructor(...)`, and Hilt figures out how to build everything.

Here's how it works in Paperback:

- `DatabaseModule.kt` tells Hilt: "When someone asks for a `BookDao`, build a `PaperbackDatabase` first (using this recipe), then call `.bookDao()` on it."
- `RepositoryModule.kt` tells Hilt: "When someone asks for a `BookRepository` (the interface), give them a `BookRepositoryImpl` (the concrete class)."
- When `ReaderViewModel` is created, Hilt reads its constructor, sees it needs a `BookRepository`, looks up the recipe, builds a `BookRepositoryImpl` (which needs a `BookDao`, which needs a database…), and hands the fully wired ViewModel to the screen.

**You never write `ReaderViewModel(BookRepositoryImpl(bookDao), ...)`.** Hilt does it for you. Magic! ✨

---

#### What is Room? 📓 *The Notebook Analogy*

Room is like a super-organized notebook where you store data in tables (think spreadsheet pages):

- **Entities** are the table structures. `BookEntity` defines columns: `id`, `filePath`, `title`, `author`, `pageCount`, `coverPath`, `currentPage`, etc. It's like drawing the column headers on a page of your notebook.

- **DAOs** (Data Access Objects) are the instructions for reading and writing. `BookDao` has methods like `insertBook()`, `getBookById()`, `searchBooks(query)`, `updateReadingProgress()`. It's like writing instructions on a sticky note: "To find a book, look in the title column for this word."

- **The Database** (`PaperbackDatabase`) is the notebook itself. It lists all the tables (entities) and provides the DAOs. Paperback's database includes tables for: books, bookmarks, highlights, reading sessions, streaks, and goals.

**The really cool part**: When you call `bookDao.getAllBooks()`, Room doesn't just return a `List<Book>` — it returns a `Flow<List<Book>>`. This means whenever *any* book is inserted, updated, or deleted, Room automatically pushes the updated list through the Flow. Your UI always shows the latest data without you manually refreshing.

In Paperback, the database is also encrypted using **SQLCipher** (`SupportFactory` + a key from `SecurityKeyManager`) so your reading data is secure even if someone gets access to your device's files.

---

#### What is StateFlow? 📡 *The Live Scoreboard Analogy*

Imagine a basketball game scoreboard in a stadium:

- The scoreboard always shows the **current score** (it has a "value" at any moment).
- When the score changes, **everyone in the stadium sees the update instantly** — you don't have to keep asking "what's the score now?"
- New spectators who arrive mid-game **immediately see the current score** — they don't have to wait for the next point.

That's exactly how `StateFlow` works in Paperback:

```kotlin
// In ReaderViewModel:
private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
```

- `_uiState` is the scoreboard operator (only the ViewModel can change the score).
- `uiState` is the public scoreboard (the screen can read it, but not change it).
- When the ViewModel does `_uiState.value = ReaderUiState.Success(...)`, the screen automatically recomposes.

In the composable:
```kotlin
val uiState by viewModel.uiState.collectAsState()
```
This line is like a spectator sitting down and watching the scoreboard. Whenever the value changes, Compose redraws only the parts of the screen that depend on the changed data.

---

#### What are Coroutines? 🍽️ *The Restaurant Order Analogy*

Imagine you're a waiter at a busy restaurant:

**Without coroutines (blocking):** A customer orders soup. You walk to the kitchen, stand there watching the chef cook the soup for 10 minutes, then bring it back. Meanwhile, all your other tables are ignored. Terrible service!

**With coroutines (non-blocking):** A customer orders soup. You hand the order to the kitchen (`launch` a coroutine) and immediately go serve your other tables. When the soup is ready, the kitchen dings a bell (`suspend` function returns), and you pick it up and deliver it. While the soup was cooking, you handled five other tables!

In Paperback, this happens everywhere:

```kotlin
// In ReaderViewModel:
fun toggleBookmark() {
    viewModelScope.launch {  // "Hand the order to the kitchen"
        // This runs without freezing the screen
        bookmarkRepository.toggleBookmark(bookId, page)
    }
    // The UI continues to respond immediately
}
```

The `suspend` keyword means "this function might take a while — I'll pause here and let other things run." `withContext(Dispatchers.IO)` means "run this in the IO kitchen, not the main/UI kitchen." If you did heavy file operations on the main thread, the app would freeze — coroutines prevent that.

Key terms:
- `viewModelScope.launch { ... }` — Start a background task tied to the ViewModel's lifecycle (auto-cancelled when the screen closes).
- `suspend fun` — A function that can pause without blocking the thread.
- `Dispatchers.IO` — Use threads optimized for disk/network operations.
- `Dispatchers.Main` — The UI thread where Compose runs.
- `Flow` — An asynchronous stream of values (like a conveyor belt of data).

---

### 🚀 Advanced Features & Premium Architecture (New!)

To elevate Paperback to a state-of-the-art flagship application, we have integrated a series of advanced features spanning local AI capabilities, offline databases, hardware-backed cryptography, dynamic UI/UX, and extreme optimizations.

#### 1. Full-Text Search (FTS) Background Indexing
* **Database Upgrades (`MIGRATION_2_3`)**: Replaced standard text columns with a high-performance SQLite virtual `FTS5` table schema mapped reactively to Room using `@Fts4` (for Room 2.6.x KSP compiler compatibility).
* **Incremental Background Worker**: Leverages Android `WorkManager` via `FtsIndexingWorker` to extract text from imported books page-by-page while the device is charging and idle.
* **Sync Triggers**: Automated database triggers synchronize the search index instantly when book pages are inserted, updated, or deleted, keeping search query latency near-zero!

#### 2. Hybrid "Chat with Book" — Offline Local AI (RAG)
* **API Resiliency**: Integrates Google Gemini `1.5-flash` for book summaries and Q&A.
* **Dual-Mode Fallback**: If the Gemini API key is missing/invalid, or the device is offline/rate-limited, the system seamlessly transitions to our custom **On-Device local AI fallback engine** inside [GeminiRepositoryImpl.kt](file:///e:/app/pdfReader/app/src/main/java/com/example/pdfreader/data/repository/GeminiRepositoryImpl.kt).
* **Smart Key & Status Auditing**: Checks `BuildConfig.GEMINI_API_KEY` for placeholder keys (like `your_api_key_here`, starts with `AIzaSyB2vzooL9KOh`, or contains `Bavery`) and intercepts any online execution exceptions. If any audit fails or network connection drops, it activates the fallback immediately without interface freezes or app crashes.
* **High-Fidelity Context Synthesis**:
  - **Local Page Summarizer**: Parses page text into distinct sentence clusters, identifies capital-case key concepts, extracts highlighted narrative blocks, and builds a comprehensive markdown summary.
  - **Local Book Chat (RAG)**: Tokenizes user questions (ignoring typical stopwords), computes a keyword relevance score against every sentence on the page, and retrieves/ranks matching passages to synthesize a direct, custom markdown answer.
  - **Local Chapter Parser**: Uses structured regex matching (e.g., matching lines starting with `Chapter`, `Section`, `Part`, etc.) and end-line numerals to dynamically construct and format a valid JSON Table of Contents when offline.
* **Natural Streaming Experience**: Feeds response word buffers into Jetpack Compose streams using asynchronous coroutine flows with a `delay(40)` typing cadence, replicating a highly premium, fluid cloud LLM token generation completely offline!

#### 3. Responsive Serif Typography Reflow & Auto-Cropping
* **Auto-Cropping**: Runs an edge boundary scan in `PdfRenderCache.kt` to auto-detect blank white margins, dynamically zooming the view bounds to maximize readable text size on smaller displays.
* **Reflow Layout**: Translates static PDF pages into fully reflowed Compose typography layouts. Readers can dynamically adjust font sizes, letter spacing, line height, and choose premium serif fonts like **Lora** and **Merriweather** in real-time.

#### 4. Hardware-Backed StrongBox Cryptography & Heap Protection
* **StrongBox Fallbacks**: Requests dedicated hardware enclaves (StrongBox Keymaster) inside `SecurityKeyManager.kt` to shield application master keys. Gracefully falls back to standard TEE enclaves to prevent emulator crashes.
* **Zero-Byte Memory Sweeping**: For advanced RAM dump protection, all active pages, bitmap pools, and bytes are explicitly overwritten with `0x00` in memory whenever the app goes to the background or enters the Stopped lifecycle state.
* **Play Integrity Attestation**: Leverages `PlayIntegrityUtil` to verify binary genuineness and device trust on start, mitigating reverse engineering and malicious side-loading.

#### 5. Premium Theme Engine, E-Ink Comfort & Tactile Haptics
* **E-Ink Comfort Preset**: A fully optimized reading mode that strips away heavy page animations, switches transitions to zero-ghosting instant page-turns, and disables system color gradients to maximize battery life and readability on e-paper screens.
* **Dynamic Wallpaper Styling**: Full Material You dynamic styling that harmonizes primary accents with system wallpapers in both Light and Dark modes.
* **Tactile Haptics**: Micro-haptics inside `CurlPageEffect.kt` compose minor friction-clicks using the Android `Vibrator` to simulate the sensation of real paper pages rubbing together.

---

### How to run this project (for absolute beginners)

#### Step 1: Install Android Studio

1. Go to [developer.android.com/studio](https://developer.android.com/studio)
2. Download Android Studio (the latest stable version)
3. Run the installer and follow the setup wizard
4. When asked about SDK components, make sure **Android SDK** and **Android Virtual Device** are checked
5. Wait for the initial setup to complete (it downloads several GB of tools)

#### Step 2: Clone the repository

Open a terminal (or Git Bash on Windows) and run:

```bash
git clone https://github.com/sumit-void/pdfReader.git
```

Or download the ZIP from GitHub and extract it.

#### Step 3: Open the project in Android Studio

1. Open Android Studio
2. Click **"Open"** (not "New Project")
3. Navigate to the cloned folder and select it
4. Click **OK**
5. Wait for Gradle sync to complete (the bar at the bottom will say "Gradle sync finished" — this can take 5-10 minutes the first time)

#### Step 4: Set up the Gemini API key

The app uses Google's Gemini AI for page summaries. You need an API key:

1. Go to [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
2. Create a free API key
3. In the project, open (or create) `local.properties` in the root folder
4. Add this line:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```

#### Step 5: Run the app

1. Connect a physical Android device via USB (with Developer Options and USB Debugging enabled), **OR** create an emulator:
   - Click **Device Manager** (📱 icon on the right sidebar)
   - Click **Create Device**
   - Pick a phone (e.g., Pixel 7) → Next
   - Download a system image (API 34 recommended) → Next → Finish
2. Select your device from the dropdown at the top
3. Click the green **▶ Run** button (or press `Shift + F10`)
4. Wait for the build to complete and the app to install

#### Common errors and how to fix them

| Error | Fix |
|---|---|
| `SDK location not found` | Open `local.properties` and make sure `sdk.dir` points to your Android SDK path (e.g., `C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk`) |
| `Gradle sync failed: Could not resolve...` | Check your internet connection. Go to **File → Settings → Build → Gradle** and make sure "Use Gradle wrapper" is selected. Try **File → Invalidate Caches → Restart**. |
| `Execution failed for task ':app:kaptDebugKotlin'` | This is usually a Hilt annotation processing issue. Try **Build → Clean Project** then **Build → Rebuild Project**. |
| `Unsupported class file major version` | Your JDK version doesn't match. Go to **File → Settings → Build → Gradle → Gradle JDK** and select JDK 17. |
| App crashes on launch with `SQLiteException` | If you changed database entities without updating the version, uninstall the app from the device and reinstall, or add a migration in `PaperbackDatabase.kt`. |
| `BiometricPrompt` crashes on emulator | The emulator may not have biometrics set up. Go to emulator **Settings → Security → Fingerprint** and enroll a fingerprint, or disable app-lock in Settings. |

---

### How to add your own feature (a simple example)

Let's walk through adding a **"Notes"** feature that lets users attach text notes to any page of a book. We'll follow the same MVVM + Clean Architecture pattern that the rest of the app uses.

#### Step 1: Create the Domain Model

Create `domain/model/Note.kt`:

```kotlin
package com.example.pdfreader.domain.model

data class Note(
    val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val text: String,
    val dateCreated: Long = System.currentTimeMillis()
)
```

#### Step 2: Create the Room Entity and DAO

Create `data/local/entity/NoteEntity.kt`:

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val text: String,
    val dateCreated: Long = System.currentTimeMillis()
)
```

Create `data/local/dao/NoteDao.kt`:

```kotlin
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY pageNumber")
    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId AND pageNumber = :page")
    fun getNotesForPage(bookId: Long, page: Int): Flow<List<NoteEntity>>

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
```

#### Step 3: Register in the Database

Open `PaperbackDatabase.kt` and add `NoteEntity::class` to the `@Database` entities list, bump the version number, add `abstract fun noteDao(): NoteDao`, and create a migration.

#### Step 4: Create the Repository

Create the interface at `domain/repository/NoteRepository.kt`:

```kotlin
interface NoteRepository {
    fun getNotesForBook(bookId: Long): Flow<List<Note>>
    fun getNotesForPage(bookId: Long, page: Int): Flow<List<Note>>
    suspend fun addNote(bookId: Long, page: Int, text: String): Note
    suspend fun deleteNote(note: Note)
}
```

Create the implementation at `data/repository/NoteRepositoryImpl.kt`:

```kotlin
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {
    // Map NoteEntity ↔ Note, delegate to DAO
}
```

#### Step 5: Wire it up with Hilt

In `DatabaseModule.kt`, add:
```kotlin
@Provides
fun provideNoteDao(database: PaperbackDatabase): NoteDao = database.noteDao()
```

In `RepositoryModule.kt`, add:
```kotlin
@Binds @Singleton
abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
```

#### Step 6: Create a UseCase

Create `domain/usecase/AddNoteUseCase.kt`:

```kotlin
class AddNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(bookId: Long, page: Int, text: String): Note {
        return noteRepository.addNote(bookId, page, text)
    }
}
```

#### Step 7: Add to the ViewModel

In `ReaderViewModel`, inject `NoteRepository`, add note-related state to `ReaderUiState.Success`, and add functions like `addNote(text: String)` and `loadNotesForCurrentPage()`.

#### Step 8: Build the UI

Add a "Add Note" button to the Reader's control bar, and a bottom sheet or dialog where the user can type and save their note. Use `collectAsState()` to reactively display the notes list.

**That's the pattern!** Model → Entity/DAO → Repository → UseCase → ViewModel → UI. Every feature in Paperback follows this same flow. Once you understand it, you can add anything.

---

### About the developer

**Sumit Kumar** — Android developer passionate about building beautiful, production-quality mobile apps with modern architecture patterns.

- 🔗 LinkedIn: [linkedin.com/in/sumittkumar911](https://www.linkedin.com/in/sumittkumar911/)
- 🐙 GitHub: [github.com/sumit-void](https://github.com/sumit-void)

---

> 💡 **Tip for beginners**: The best way to learn this codebase is to pick *one flow* (like "what happens when I tap a bookmark?") and trace it through every layer. Use Android Studio's `Ctrl+Click` (or `Cmd+Click` on Mac) to jump from function call to definition. Read the code like a story, not a textbook. You'll be surprised how quickly it makes sense!

Happy reading, and happy coding! 📚✨
