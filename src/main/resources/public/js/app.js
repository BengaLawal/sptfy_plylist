// On page load, check with the server if the user is logged in
window.onload = async function() {
    try {
        const response = await fetch('/spotify/auth/status');  // request login status
        const data = await response.json();

        if (data.spotifyLoggedIn) {
            document.getElementById('spotifyButton').setAttribute("disabled", "disabled");
            document.getElementById('spotifyButton').innerText = 'Spotify Login Successful';

            // Fetch and display playlists
            await fetchPlaylists();
        } else{
            document.getElementById('spotifyButton').removeAttribute("disabled");
            document.getElementById('spotifyButton').innerText = 'Login with Spotify';
        }
    } catch (error) {
        console.error('Error checking login status:', error);
    }
}

// Fetch playlists from the server
async function fetchPlaylists() {
    console.log("fetching playlist")
    try {
        const response = await fetch('/playlists');
        console.log("Response status:", response.status);

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
                li.innerText = playlist;
                playlistList.appendChild(li);
            });
        }

        // Show the playlist section
        playlistSection.style.display = 'block';
    } catch (error) {
        console.error('Error fetching playlists:', error);
    }
}