# Spotify Java Service - README

## Overview

This project is a Java-based service that integrates with the Spotify API, providing functionality for users to login with Spotify, view their playlists, and transfer them to another platform (e.g., YouTube Music). It leverages Javalin as the web framework, and utilizes the Spotify Web API for interacting with Spotify data.

### Main Components

- **SpotifyService**: Handles the communication with the Spotify API, including login, refreshing tokens, and fetching playlists or songs from playlists.
- **SpotifyController**: Exposes RESTful endpoints to manage user interactions, such as logging in with Spotify, listing playlists, and transferring selected playlists.
- **Frontend Integration**: Includes HTML and JavaScript to manage user interaction, allowing users to log in to Spotify, view their playlists, and select them for transfer.

## Features

1. **User Authentication with Spotify**
   - Users can log in with their Spotify account using the OAuth flow. The access token and refresh token are managed by the `SpotifyService` class.

2. **Playlist Management**
   - Users can view their playlists after logging in. The playlists are displayed on the frontend with an option to select them for further actions.

3. **Transfer Playlists**
   - Users can select playlists and transfer them to another service (e.g., YouTube Music). The tracks are fetched for each playlist and sent to a microservice handling YouTube integration.

### Prerequisites

- **Java 11 or higher**
- **Maven** for dependency management
- **Spotify Developer Account**: You need to create a Spotify app to get a client ID and client secret.

### Available Endpoints

- **GET /login/spotify**: Redirects to Spotify for user authentication.
- **GET /playlists**: Returns the user's playlists after logging in.
- **POST /transfer-playlists**: Accepts a list of playlist IDs and initiates the process of transferring them.

## Frontend

- The frontend consists of an HTML file (`index.html`) with JavaScript (`app.js`) to manage user interaction.
- Users can log in with Spotify, view playlists, and select them for transfer.
- An embedded Spotify player iframe is dynamically added when a playlist is clicked, providing a preview of the selected playlist.



