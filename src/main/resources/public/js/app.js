// On page load, check with the server if the user is logged in
window.onload = async function() {
    try {
        const response = await fetch('/spotify/auth/status');  // request login status
        const data = await response.json();

        if (data.spotifyLoggedIn) {
            document.getElementById('spotifyButton').setAttribute("disabled", "disabled");
            document.getElementById('spotifyButton').innerText = 'Spotify Login Successful';
        } else{
            document.getElementById('spotifyButton').removeAttribute("disabled");
            document.getElementById('spotifyButton').innerText = 'Login with Spotify';
        }
    } catch (error) {
        console.error('Error checking login status:', error);
    }
}