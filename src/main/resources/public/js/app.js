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

        // Clear any previous playlist items
        playlistList.innerHTML = '';

        // Check if playlists are available
        if (!data.playlists || data.playlists.length === 0) {
            const message = document.createElement('p');
            message.innerText = 'No playlists found.';
            playlistList.appendChild(message);
        } else {
            // Display playlists in the list
            data.playlists.forEach(playlist => {
                const li = document.createElement('li');
                li.innerText = playlist.name;
                li.dataset.playlistId = playlist.id;  // Store the playlist ID in a data attribute
                li.classList.add('playlist-item');

                // Add click event listener to display the Spotify embed player
                li.addEventListener('click', () => {
                    // Remove existing iframes from all other playlist items
                    document.querySelectorAll('.playlist-embed').forEach(iframe => iframe.remove());

                    displaySpotifyEmbed(playlist.id, li);
                });

                playlistList.appendChild(li);
            });
        }

        // Show the playlist section
        playlistSection.style.display = 'block';
    } catch (error) {
        console.error('Error fetching playlists:', error);
    }
}

// Function to display the Spotify embed player for a clicked playlist
function displaySpotifyEmbed(playlistId, parentElement) {
    console.log("Displaying embed for playlist ID:", playlistId);

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
    parentElement.appendChild(iframe);
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