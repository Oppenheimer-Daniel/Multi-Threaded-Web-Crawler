const urlInput = document.getElementById('url-input');
const submitBtn = document.getElementById('submit-btn');
const statusMsg = document.getElementById('status-message');
const imageList = document.getElementById('image-list');

// Safety Check: Verify all elements exist on page load
if (!urlInput || !submitBtn || !imageList) {
    console.error("Missing HTML elements! Check your IDs in index.html");
}

// Helper function to make API calls and handle responses
const makeApiCall = (url, method, payload, callback) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url);
    
    // Set the content type for form data. This matches what the server expects when using URLSearchParams.
    // https://stackoverflow.com/questions/19694503/ajax-setrequestheader-content-type-application-x-www-form-urlencoded-and-ch
    xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    
    // Handle the response from the server
    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            statusMsg.classList.remove('error'); // Reset error state on new response

            if (xhr.status === 200) { // Success
                callback(JSON.parse(xhr.responseText));
            } else { // Error handling: Try to extract error message from response, otherwise show generic error
                let errorDetails = "An unknown error occurred.";
                try { // Attempt to parse error message from server response
                    const response = JSON.parse(xhr.responseText);
                    errorDetails = response.error || `Error ${xhr.status}`;
                } catch (e) {
                    errorDetails = `Server Error: ${xhr.status}`;
                }
                statusMsg.innerText = "❌ " + errorDetails;
                statusMsg.classList.add('error');
            }
        }
    };
    xhr.send(payload); // Send the encoded string
};

// Function to display error messages in the UI
const displayError = (msg) => {
    statusMsg.innerText = "⚠️ " + msg;
    statusMsg.classList.add('error');
    imageList.innerHTML = ''; 
};

// Function to update the image list in the UI with the response from the server
const updateList = (response) => {
    console.log("Images received from Java:", response); // Log the actual array
    imageList.innerHTML = '';
    
    // Check if response is valid and contains images before trying to display them
    if (!response || response.length === 0) {
        statusMsg.innerText = "No images found.";
        return;
    }

    // Iterate over the array of image URLs and create list items with images for each URL
    response.forEach(url => {
        const li = document.createElement('li');
        const img = document.createElement('img');
        img.src = url;
        img.alt = "Crawled image";
        li.appendChild(img);
        imageList.appendChild(li);
    });

    // Update the status message to show how many images were found
    statusMsg.innerText = `Success! Found ${response.length} images.`;
};

// https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener
// Add click event listener to the submit button to trigger the crawling process when clicked
submitBtn.addEventListener("click", (e) => {
    e.preventDefault(); // Prevent default form submission behavior
    
    // Trim the input value to remove extra whitespace and check if it's empty before making the API call
    const urlValue = urlInput ? urlInput.value.trim() : "";
    console.log("Button clicked. URL Value:", urlValue);

    if (!urlValue) { // If the input is empty, display an error message and return early to avoid making an API call
        statusMsg.innerText = "❌ Please enter a URL!";
        statusMsg.classList.add('error');
        return;
    }

    // Update the status message to indicate that crawling has started and reset any previous error state or image list before making the API call
    statusMsg.innerText = "Crawling " + urlValue + "...";
    statusMsg.classList.remove('error'); // Reset error state
    imageList.innerHTML = '';

    // Switch to URLSearchParams for standard form encoding
    const params = new URLSearchParams();
    params.append("url", urlValue);

    // Make the API call to the server with the encoded parameters and handle the response with the updateList callback function
    makeApiCall('/main', 'POST', params.toString(), updateList);
});