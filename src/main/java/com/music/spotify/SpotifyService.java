package com.music.spotify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.data.library.GetUsersSavedTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpotifyService {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);
    private final SpotifyApi spotifyApi;
    private String authorizationCode;
    private String refreshToken;
    private long accessTokenExpirationTime;

    public SpotifyService(String clientId, String clientSecret, URI redirectUri) {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
    }

    public CompletableFuture<URI> getAuthorizationUriAsync(String state) {
        logger.info("Generating Spotify authorization URI with state: {}", state);
        return spotifyApi.authorizationCodeUri()
                .state(state)
                .scope("user-read-email, user-read-private, playlist-read-private, playlist-read-collaborative, user-library-read")
                .show_dialog(true)
                .build()
                .executeAsync();
    }

    public void exchangeAuthorizationCodeAsync(String code) {
        logger.info("Exchanging authorization code for access token");
        this.authorizationCode = code;
        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authorizationCode).build();

        authorizationCodeRequest.executeAsync().thenAccept(credentials -> {
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            // Calculate and save the token expiration time
            accessTokenExpirationTime = System.currentTimeMillis() + (credentials.getExpiresIn() * 1000L);

            logger.info("Access Token: {}", credentials.getAccessToken());
            logger.info("Refresh Token: {}", credentials.getRefreshToken());
            logger.info("Token expires in: {} seconds", credentials.getExpiresIn());
        }).exceptionally(e -> {
            logger.error("Error exchanging authorization code: {}", e.getMessage(), e);
            return null;
        });
    }

    public CompletableFuture<Void> refreshAccessTokenAsync() {
        logger.info("Refreshing Spotify access token");
        AuthorizationCodeRefreshRequest refreshRequest = spotifyApi.authorizationCodeRefresh().build();

        return refreshRequest.executeAsync().thenAccept(credentials -> {
            spotifyApi.setAccessToken(credentials.getAccessToken());
            logger.info("New Access Token: {}", credentials.getAccessToken());
            logger.info("Token expires in: {} seconds", credentials.getExpiresIn());
        }).exceptionally(e -> {
            logger.error("Error refreshing access token: {}", e.getMessage(), e);
            return null;
        });
    }

    // Check if user is logged in by verifying if the access token exists and is valid
    public boolean isLoggedIn() {
        String accessToken = spotifyApi.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token found. User is not logged in.");
            return false;
        }
        // Check if the token has expired
        long currentTime = System.currentTimeMillis();
        if (currentTime >= accessTokenExpirationTime) {
            logger.warn("Access token has expired.");
            return false;
        }
        logger.info("User is logged in with a valid access token.");
        return true;
    }

    // method to get the current user ID
    public CompletableFuture<String> getUserIdAsync() {
        logger.info("Fetching current user's Spotify ID");
        GetCurrentUsersProfileRequest request = spotifyApi.getCurrentUsersProfile().build();
        return request.executeAsync()
                .thenApply(User::getId)
                .exceptionally(e -> {
                    logger.error("Error fetching user ID: {}", e.getMessage(), e);
                    return null;
                });
    }

    // get list of user playlist - loop through pages
    public CompletionStage<List<String>> getListOfUsersPlaylistsAsync() {
        logger.info("Fetching user's playlists");
        return getUserIdAsync()
                .thenCompose(userId -> {
                    if (userId == null) {
                        logger.error("User ID could not be fetched.");
                        return CompletableFuture.completedFuture(List.of());
                    }
                    return fetchAllPlaylists(userId, 0, List.of());
                })
                .exceptionally(e -> {
                    logger.error("Error fetching playlists: {}", e.getMessage(), e);
                    return List.of();
                });
    }

    // Helper method to fetch all playlists with pagination
    private CompletionStage<List<String>> fetchAllPlaylists(String userId, int offset, List<String> accumulatedPlaylists) {
        GetListOfUsersPlaylistsRequest request = spotifyApi.getListOfUsersPlaylists(userId)
                .limit(50)
                .offset(offset)
                .build();

        return request.executeAsync()
                .thenCompose(playlistPaging -> {
                    List<String> currentPlaylists = Stream.of(playlistPaging.getItems())
                            .map(PlaylistSimplified::getName)
                            .toList();

                    List<String> updatedAccumulatedPlaylists = Stream.concat(accumulatedPlaylists.stream(), currentPlaylists.stream())
                            .collect(Collectors.toList());

                    // If there are more playlists to fetch, continue with the next page
                    if (playlistPaging.getTotal() > offset + playlistPaging.getItems().length) {
                        return fetchAllPlaylists(userId, offset + 50, updatedAccumulatedPlaylists);
                    } else {
                        // No more playlists to fetch, return the accumulated list
                        return CompletableFuture.completedFuture(updatedAccumulatedPlaylists);
                    }
                });
    }

    // method to get liked songs
    public CompletableFuture<Paging<SavedTrack>> getLikedSongs() {
        logger.info("Fetching user's liked songs");
        GetUsersSavedTracksRequest request = spotifyApi.getUsersSavedTracks()
          .limit(50)
          .offset(0)
//          .market(CountryCode.SE)
                .build();
        return request.executeAsync().exceptionally(e -> {
            logger.error("Error fetching liked songs: {}", e.getMessage(), e);
            return null;
        });
    }

}
