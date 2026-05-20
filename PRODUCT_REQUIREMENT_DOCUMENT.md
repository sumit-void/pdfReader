# Product Requirement Document (PRD) - Paperback Book Reader

## 1. Overview & Concept
**Paperback** is a premium, distraction-free Android application that transforms dry, static PDF documents into beautiful, warm, and highly readable reflowed books. Instead of forcing users to pinch and pan across fixed-layout PDF pages, Paperback extracts the text and structural content from any user-imported PDF, dynamically paginates it according to the user's custom reading comfort settings, and renders it with a realistic 3D book-swipe/curl page turn effect. The visual style is inspired by classic paperbacks, using soft cream tones, deep shadows, and elegant serif typography.

---

## 2. Key Features

### 2.1. Onboarding & Dummy Authentication
- **User Login Screen**: A simple, elegant login/onboarding screen to establish user context. A dummy implementation for now, with options for email login or quick bypass.
- **Persistent Session State**: Once "logged in", the user stays authenticated and is routed directly to the Library on subsequent launches.

### 2.2. Library Management
- **Empty State**: When no books have been imported, a clean, inviting empty state guides the user with a prompt and a prominent "+" floating button.
- **PDF Importing**: Import any PDF from internal storage using the Android Storage Access Framework (SAF).
- **Persistent Local Caching**: PDFs are copied to the app's internal storage to guarantee offline access and immunity to external file movement/deletion.
- **Grid Layout**: A visual shelf/grid of imported books displaying:
  - Dynamically generated book covers (using the first page).
  - Book title and author (extracted from metadata).
  - Last-read progress badge (e.g., "45% read").
  - Date added.

### 2.3. The Paperback Reflow Engine
- **Text & Structure Extraction**: Parses the PDF's text and document outline (Table of Contents) in the background.
- **Dynamic Pagination**: Measures and segments the text flow into screen-sized pages dynamically based on:
  - Screen dimensions (width/height).
  - Font family and text size.
  - Selected margins and line heights.
  - Device orientation (portrait vs. landscape).
- **Orientation Behavior**:
  - **Portrait**: Renders one reflowed page at a time.
  - **Landscape (Dual-Page Spread)**: Renders two reflowed pages side-by-side with an ambient vertical spine shadow in the crease.

### 2.4. Immersive Reader View
- **Distraction-Free UI**: Tapping the center of the screen toggles the visibility of reading controls (top toolbar, bottom progress bar, settings menu, and navigation drawer).
- **3D Page Flip**: Swipe gesture or page clicks trigger a realistic 3D folding page-turn animation simulating a physical paper page.
- **Table of Contents (TOC) Drawer**: A navigation drawer showing the document's outline. Tapping an item jumps the reader to that chapter.
- **Bookmarks Manager**:
  - Add bookmarks with custom page-preview thumbnails.
  - Access saved bookmarks via the navigation drawer.
- **Full-Text Search**: Search for keywords inside the book, returning page numbers and snippet lists, with instant navigation on click.
- **Resume Reading**: Automatically saves and restores the last-read position per book.

### 2.5. Reading Comfort & Settings (Bottom Sheet)
- **Typography Settings**: Switch between premium serif fonts (Lora, Playfair Display) and standard sans-serif.
- **Font Size & Margin Control**: Slider and button step-adjusters for text size, page margins, and line heights.
- **Themes**:
  - **Sepia (Cream)**: Warm #F5ECD7 paper tone background with soft dark text.
  - **Dark Charcoal**: Matte #1C1C1E background with warm off-white text.
  - **Light**: Balanced high-contrast white theme.
- **Brightness Slider**: In-app brightness adjustment overlay.
- **Auto-Scroll / Reading Timer**: Hands-free scrolling or reading interval alarms.

---

## 3. UI/UX Design Goals
- **Warm & Paperback-Toned**: Core theme utilizes HSL sepia colors, avoiding harsh stark whites and blues.
- **Realistic Shadows & Depth**: Book pages feature subtle drop shadows to create a layered physical page depth.
- **Micro-Animations**: Smooth transitions when toggling controls, sliding menus, or flipping pages.
- **Serif Typography**: Primary emphasis on premium typography to elevate readability.

---

## 4. Technical Constraints & Tech Stack
- **OS Version**: Android 8.0+ (Min SDK 26).
- **Language**: Kotlin.
- **Architecture**: MVVM + Repository Pattern.
- **Local Database**: Room DB (for books metadata, bookmarks, last read status, and reading history).
- **PDF Handling**: Apache PDFBox for Android (for outline/text extraction & search).
- **Git Repository**: Iterative commits published component-by-component to the public repository.
