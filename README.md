# Multi-Threaded Image Crawler & Discovery Engine
**A high-performance Java 8 web crawler with a modern asynchronous frontend.**

## 📖 Overview
This project is a full-stack discovery engine designed to traverse web domains and extract image metadata. It solves the challenge of efficient web scraping by using multi-threaded crawling logic while providing a real-time, responsive user interface.

Developed as a deep dive into Java concurrency and modern web integration, the application traverses target domains, parses image data using Jsoup, and presents discovery results through a responsive "Glassmorphism" web interface.

## 🚀 Key Technical Features
* **High-Concurrency Engine:** Orchestrates a `FixedThreadPool` and `Phaser` synchronization barrier to crawl multiple sub-pages simultaneously while preventing thread-leaks.
* **Intelligent Domain-Locking:** Implements strict filtering logic to ensure the crawler respects domain boundaries and prevents infinite recursive loops.
* **Thread-Safe Data Structures:** Utilizes a `Synchronized LinkedHashSet` to achieve $O(1)$ lookup performance for duplicate detection while preserving discovery order for deterministic results.
* **Modern Full-Stack Integration:** Features a sleek frontend built with **CSS Grid** and **AJAX (XHR)** for real-time status updates and dynamic gallery rendering.
* **Defensive Engineering:** Robust handling for HTTP status codes (400, 403, 500) and automated Maven testing workflows to ensure build integrity.

---

## 🛠️ Tech Stack
* **Backend:** Java 8, Jsoup (HTML Parsing), GSON (JSON Serialization)
* **Concurrency:** `ExecutorService`, `Phaser`, `Synchronized Collections`
* **Frontend:** JavaScript (ES6+), HTML5, CSS3 (Grid & Backdrop-filters)
* **Build Tool:** Maven

---

## 🏗️ System Architecture & Challenges

### The Synchronization Gap
One of the primary challenges during development was a `RejectedExecutionException` during high-volume crawls. This was solved by migrating to a `Phaser` synchronization barrier, ensuring the "party count" was registered immediately before task submission, effectively preventing the main thread from shutting down the executor prematurely.

### Deterministic Results in a Multi-threaded Environment
Standard `HashSets` yielded non-deterministic results in testing due to the random nature of thread completion. By implementing a **Synchronized LinkedHashSet**, the system achieved thread safety while maintaining the specific insertion order needed to pass automated integrity checks.

---

## 🚧 Known Issues & Future Roadmap
* **Image Validation:** Some crawled images are valid URLs but may be blocked by hotlinking protections or broken on the host side. A future enhancement involves a pre-render "HEAD" request to verify image visibility before displaying them in the UI.

---

## 📝 Development Journal

### Phase 1: Security & Core Logic
* **Vulnerability Patching:** Updated dependencies to address CVE-2021-37714 and CVE-2022-25647.
* **Environment Configuration:** Configured Jetty server compatibility for Java 8.
* **Milestone:** Completed basic single-page parsing and JSON serialization.

### Phase 2: Recursion & Efficiency
* **State Management:** Implemented `Collections.synchronizedSet` to safely track `visitedUrls` and `foundImages` across threads.
* **Recursive Depth:** Added depth-control logic and "Friendly Crawler" delays to ensure ethical scraping practices.

### Phase 3: Concurrency & Phaser Implementation
* **Concurrency Fix:** Resolved race conditions by integrating a `Phaser` synchronization block.
* **Error Resilience:** Implemented `try-finally` blocks to ensure proper thread deregistration regardless of I/O exceptions.
* **Testing:** Established automated unit tests to verify image discovery accuracy and collection integrity.

### Phase 4: UI/UX & Glassmorphism
* **Visual Overhaul:** Designed an interface using `backdrop-filter` for a frosted-glass effect and CSS Grid for a responsive image gallery.
* **Dynamic Feedback:** Developed the JavaScript driver to report real-time crawling status and server-side errors to the client.

### Phase 5: Optimization & Stress Testing
* **Edge Case Testing:** Verified the crawler against diverse architectures (Photography portfolios, Wikipedia, Jsoup.org).
* **Resiliency:** Optimized handling for 403 Forbidden errors and bot-protected directories.

---

## ⚙️ How to Run
1. **Requirements:** Ensure Maven 3.5+ and **Java 8** are installed.
2. **Build:** Run `mvn clean package` in the root directory.
3. **Launch:** Run `mvn jetty:run` and navigate to `http://localhost:8080`.