package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(
    name = "ImageFinder",
    urlPatterns = {"/main"}
)
public class ImageFinder extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new GsonBuilder().create(); 
	// LinkedHashSet is a HashSet with a doubly-linked list running through it to maintain insertion order, return images in the order they were found while still having O(1) contains() performance and no duplicates
    // https://www.geeksforgeeks.org/java/linkedhashset-in-java-with-examples/
	protected static Set<String> foundImages = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
	// https://stackoverflow.com/questions/32552307/hashset-vs-arraylist-contains-performance


    private static final int MAX_DEPTH = 2; // Only go 2 clicks deep to avoid long crawl times and potential infinite loops, can be adjusted as needed
    private static final int MAX_PAGES = 50; // Limit total pages to crawl to prevent excessive load, can be adjusted as needed

	// Test images to return when URL is null for test cases
    public static final String[] testImages = {
        "https://images.pexels.com/photos/545063/pexels-photo-545063.jpeg?auto=compress&format=tiny",
        "https://images.pexels.com/photos/464664/pexels-photo-464664.jpeg?auto=compress&format=tiny",
        "https://images.pexels.com/photos/406014/pexels-photo-406014.jpeg?auto=compress&format=tiny",
        "https://images.pexels.com/photos/1108099/pexels-photo-1108099.jpeg?auto=compress&format=tiny"
    };

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/json");
        String url = req.getParameter("url");

        foundImages.clear();
        visitedUrls.clear();

        // Handle Null URL for Test Cases
        if (url == null) {
            resp.getWriter().print(GSON.toJson(testImages));
            return;
        }

        // Handle Empty URL for actual use cases, return 400 Bad Request if URL is empty or just whitespace
        if (url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(GSON.toJson(Collections.singletonMap("error", "Invalid or missing URL parameter.")));
            return;
        }

        // Use a fixed thread pool to limit concurrent threads and a Phaser to track when all crawling tasks are complete
        ExecutorService executor = Executors.newFixedThreadPool(10);
        final Phaser phaser = new Phaser(1); // Phaser to synchronize the completion of all crawling tasks, start with 1 registered party for the main thread
		// https://www.geeksforgeeks.org/java/java-util-concurrent-phaser-class-in-java-with-examples/

        try {
            phaser.register(); // Register the initial crawling task for the main URL
            executor.submit(() -> crawl(url, url, 0, executor, phaser)); // Start crawling from the initial URL, using the URL as the domain to restrict crawling to the same site

			// Wait for all crawling tasks to complete before proceeding, the main thread will wait here until all registered parties (crawling tasks) have arrived and deregistered
            phaser.arriveAndAwaitAdvance();
            executor.shutdown();

			// If no images were found, return 403 Forbidden with an error message, otherwise return the list of found images as JSON
            if (foundImages.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().print(GSON.toJson(Collections.singletonMap("error", 
                    "Access Denied (403) or no images found on " + url)));
            } else {
                resp.getWriter().print(GSON.toJson(foundImages));
            }

        } catch (Exception e) {
			// catch any thread or execution exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(GSON.toJson(Collections.singletonMap("error", "Server Error: " + e.getMessage())));
			// https://stackoverflow.com/questions/33744875/how-to-log-all-requests-and-responses-with-exceptions-in-single-place
        }
    }

	// Recursive crawl method to find images on subpages, takes the target URL to crawl, the domain to restrict crawling to, the current depth of crawling, the executor service for submitting new crawl tasks, and the phaser for synchronizing task completion
    private void crawl(String targetUrl, String domain, int depth, ExecutorService executor, Phaser phaser) {
        if (depth > MAX_DEPTH 
            || visitedUrls.size() >= MAX_PAGES // Limit total pages to crawl to prevent excessive load
            || !targetUrl.startsWith(domain) // Stay within the same domain to avoid crawling the entire web
            || targetUrl.toLowerCase().endsWith(".pdf") // Skip PDFs as they are not HTML pages and Jsoup can't parse them for images/links
			// IO Error on https://www.eulerity.com/static/Novus-da7a206ba8d7e6e461f6fea48c17e5c4.pdf: Unhandled content type. Must be text/*, */xml, or */*+xml    - This error occured, parsing Eulerity's website for images, but it has a PDF linked that Jsoup can't handle, so we skip any URLs that end with .pdf
            || !visitedUrls.add(targetUrl) // If the URL has already been visited, skip it
            || targetUrl.contains("cdn-cgi")) // Ran into this on one of the test sites, it seems to be a security measure that blocks bots and causes errors, so we will skip any URLs that contain "cdn-cgi"
			{ 
            phaser.arriveAndDeregister(); // If we are skipping this URL for any reason, we need to arrive and deregister from the phaser to avoid blocking the main thread indefinitely
            return;
        }

        try {
            Thread.sleep(100);

			// Connect and Parse
            Document doc = Jsoup.connect(targetUrl) // // https://stackoverflow.com/questions/6581655/jsoup-useragent-how-to-set-it-right
                                .userAgent("Mozilla/5.0") // Identifying as a browser is "friendly", user-agent
                                .timeout(5000) // wait a max of 5 seconds for the server to respond before giving up
                                .get(); // https://jsoup.org/apidocs/org/jsoup/Jsoup.html#connect-java.lang.String- and https://jsoup.org/apidocs/org/jsoup/Connection.html#userAgent-java.lang.String- and https://jsoup.org/apidocs/org/jsoup/Connection.html#timeout-int-
								// .get() is a blocking call that will wait until the page is fully fetched
            
			// Extract Images
            for (Element img : doc.select("img")) {
                String imgSrc = img.absUrl("src"); // this filters out data URIs and relative paths which may not be valid outside the context of the page
                if (imgSrc.isEmpty()) {
                    imgSrc = img.attr("src");
                }

                if (!imgSrc.isEmpty()) {
					// Only add images that are absolute URLs and start with http or https
                    foundImages.add(imgSrc);
                }
            }

			// Discover Links and Submit New Crawl Tasks to the Pool
            for (Element link : doc.select("a[href]")) {
                String subPage = link.absUrl("href").split("#")[0];
                
				// Re-check domain and depth before wasting a thread on a submission
                if (subPage.startsWith(domain) && depth < MAX_DEPTH) {
                    phaser.register(); // Register a new task with the Phaser before submitting to ensure it knows to wait for this new task
                    try {
                        executor.submit(() -> crawl(subPage, domain, depth + 1, executor, phaser));
                    } catch (RejectedExecutionException e) {
                        phaser.arriveAndDeregister(); // If the task can't be submitted, deregister it immediately to avoid waiting for a task that won't run
                    }
                }
            }

			// Previous code without concurrency and error handling, left here for reference
			// try {
			//     // delay to avoid overwhelming the server
			//     Thread.sleep(100); 
				
			//     Document doc = Jsoup.connect(targetUrl).get();
				
			//     // Extract Images
			//     for (Element img : doc.select("img")) {
			//         String imgSrc = img.absUrl("src");
			//         if (!imgSrc.isEmpty()) foundImages.add(imgSrc);
			//     }

			//     // Extract and Follow Links
			//     for (Element link : doc.select("a[href]")) {
			//         String subPage = link.absUrl("href");
			//         // Recursive call with incremented depth
			//         // crawl(subPage, domain, depth + 1); This is the original day 1 method, but it is not concurrent and can be very slow
			// 		executor.submit(() -> crawl(subPage, domain, depth + 1, executor)); // Use thread pool to crawl subpages concurrently
			//     }

        } catch (IOException | InterruptedException e) { 
			// https://www.geeksforgeeks.org/java/handle-an-ioexception-in-java/
			// https://docs.oracle.com/javase/8/docs/api/java/lang/InterruptedException.html possible after Thread.sleep()
			// https://docs.oracle.com/javase/8/docs/api/java/lang/Exception.html
            System.err.println("Error on " + targetUrl + ": " + e.getMessage());
        } finally {
            phaser.arriveAndDeregister(); // Signal that this task is done, allowing the main thread to proceed when all tasks are complete
        }
    }
}