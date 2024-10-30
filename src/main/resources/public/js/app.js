// On page load, check with the server if the user is logged in
window.onload = async function() {
    try {
        const response = await fetch('/spotify/auth/status');  // request login status
        const data = await response.json();

        if (data.spotifyLoggedIn) {
            await handleSpotifyLoginSuccessful()
        } else{
            handleSpotifyLoginRequired()
        }
    } catch (error) {
        console.error('Error checking login status:', error);
    }
}

// Handle successful Spotify login
async function handleSpotifyLoginSuccessful(){
    document.getElementById('spotifyButton').setAttribute("disabled", "disabled");
    document.getElementById('spotifyButton').innerText = 'Spotify Login Successful';

    // Fetch and display playlists
    await fetchPlaylists();
}

// Handle Spotify login requirement
function handleSpotifyLoginRequired() {
    document.getElementById('spotifyButton').removeAttribute("disabled");
    document.getElementById('spotifyButton').innerText = 'Login with Spotify';
}

// Fetch playlists from the server
async function fetchPlaylists() {
    console.log("fetching playlist")
    try {
        const response = await fetch('/playlists');
        console.log("Response status:", response.status);

        if (response.status === 401) {
            // If the response is 401 Unauthorized, it means the token might have expired
            console.log("Token expired. Refreshing token and retrying...");
            await refreshAccessTokenAndRetry(fetchPlaylists);
            return;
        }

        const data = await response.json();
        console.log("Parsed data:", data);

        const playlistSection = document.getElementById('playlistSection');
        const playlistList = document.getElementById('playlistList');
        const submitButton = document.getElementById('transferButton');

        // Clear any previous playlist items
        playlistList.innerHTML = '';

        // Check if playlists are available
        if (!data.playlists || data.playlists.length === 0) {
            const message = document.createElement('p');
            message.innerText = 'No playlists found.';
            playlistList.appendChild(message);
            submitButton.style.display = 'none';
        } else {
            // Display playlists in the list
            data.playlists.forEach(playlist => {
                const li = document.createElement('li');
                li.classList.add('playlist-item');

                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.classList.add('playlist-checkbox')
                checkbox.dataset.playlistId = playlist.id

                const label = document.createElement('label');
                label.innerText = playlist.name;

                li.appendChild(checkbox);
                li.appendChild(label);

                // Add click event listener to display the Spotify embed player
                li.addEventListener('click', () => {
                    displaySpotifyEmbed(playlist.id);
                });

                playlistList.appendChild(li);
            });
            // Show the submit button
            submitButton.style.display = 'block';
        }
        // Show the playlist section
        playlistSection.style.display = 'block';

        // Add event listeners for "Select All" and "Submit" buttons
        setupSelectAllCheckbox();
        setupTransferButton();
    } catch (error) {
        console.error('Error fetching playlists:', error);
    }
}

// Function to display the Spotify embed player for a clicked playlist
function displaySpotifyEmbed(playlistId) {
    console.log("Displaying embed for playlist ID:", playlistId);

    const embedContainer = document.getElementById('embedContainer');
    const spotifyEmbed = document.getElementById('spotifyEmbed');

    // Clear any existing embed iframe
    spotifyEmbed.innerHTML = '';

    // Create a new iframe with the embed code
    const iframe = document.createElement('iframe');
    iframe.classList.add('playlist-embed');
    iframe.style.borderRadius = "12px";
    iframe.src = `https://open.spotify.com/embed/playlist/${playlistId}?utm_source=generator`;
    iframe.width = "100%";
    iframe.height = "352";
    iframe.frameBorder = "0";
    iframe.allow = "autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture";
    iframe.loading = "lazy";

    // Append the iframe directly under the clicked <li> element
    spotifyEmbed.appendChild(iframe);

    // Show the embed container
    embedContainer.style.display = 'block';
}

// Select All checkbox functionality
function setupSelectAllCheckbox(){
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const checkboxes = document.querySelectorAll('.playlist-checkbox');

    selectAllCheckbox.addEventListener('change', () =>{
        const isChecked = selectAllCheckbox.checked;
        checkboxes.forEach(checkbox => {
            checkbox.checked = isChecked;
        });
    });
}

// transfer button
function setupTransferButton() {
    const submitButton = document.getElementById('transferButton');

    submitButton.addEventListener('click', () => {
        const selectedPlaylists = [];
        document.querySelectorAll('.playlist-checkbox:checked').forEach(checkbox => {
            selectedPlaylists.push(checkbox.dataset.playlistId);
        });

        if (selectedPlaylists.length === 0) {
            alert('Please select at least one playlist to submit.');
            return;
        }

        console.log("Selected Playlists:", selectedPlaylists);

        //TODO: send the selected playlists and it's info to ytMicroService here
    });
}


// Helper function to refresh the access token and retry an action
async function refreshAccessTokenAndRetry(action) {
    try {
        const response = await fetch('/spotify/refresh-token', {
            method: 'POST',
        });

        if (response.ok) {
            console.log("Access token successfully refreshed.");
            await action(); // Retry the original action
        } else {
            console.error("Error refreshing access token. User may need to reauthenticate.");
            handleSpotifyLoginRequired();
        }
    } catch (error) {
        console.error('Error refreshing access token:', error);
        handleSpotifyLoginRequired();
    }
}