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

        return refreshRequest.executeAsync()
                .thenAccept(credentials -> {
                    spotifyApi.setAccessToken(credentials.getAccessToken());

                    // Calculate and save the new expiration time
                    accessTokenExpirationTime = System.currentTimeMillis() + (credentials.getExpiresIn() * 1000L);

                    // Optionally update refresh token if provided
                    if (credentials.getRefreshToken() != null) {
                        spotifyApi.setRefreshToken(credentials.getRefreshToken());
                        logger.info("Refresh Token has been updated.");
                    }

                    logger.info("New Access Token: {}", credentials.getAccessToken());
                    logger.info("Token expires in: {} seconds", credentials.getExpiresIn());
                }).exceptionally(e -> {
                    logger.error("Error refreshing access token: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to refresh access token", e);
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
            logger.warn("Access token has expired. Attempting to refresh...");
            try {
                // Refresh the token
                refreshAccessTokenAsync();
                logger.info("Access token refreshed successfully.");
            } catch (Exception e) {
                logger.error("Failed to refresh access token: {}", e.getMessage(), e);
                return false;
            }
        }
        logger.info("User is logged in with a valid access token.");
        return true;
    }

    // ensure access token is active; otherwise, refresh access token
    private CompletionStage<Void> ensureAccessTokenIsValid() {
        if (isLoggedIn()) {
            return CompletableFuture.completedFuture(null);
        } else {
            logger.info("Access token is expired or user is not logged in. Refreshing access token...");
            return refreshAccessTokenAsync();
        }
    }

    // method to get the current user ID
    public CompletableFuture<String> getUserIdAsync() {
        return ensureAccessTokenIsValid()
                .thenCompose(unused -> requestUserId())
                .toCompletableFuture();
    }

    // Helper function to get user id
    private CompletableFuture<String> requestUserId() {
        logger.info("Fetching current user's Spotify ID");
        GetCurrentUsersProfileRequest request = spotifyApi.getCurrentUsersProfile().build();
        return request.executeAsync()
                .thenApply(User::getId)
                .exceptionally(e -> {
                    logger.error("Error fetching user ID: {}", e.getMessage(), e);
                    return null;
                });
    }

    // Get user's playlists by fetching all pages, with automatic refresh if token is expired
    public CompletionStage<List<String>> fetchUserPlaylistsWithPagination() {
        return ensureAccessTokenIsValid()
                .thenCompose(unused -> retrieveAllUserPlaylists());
    }

    // Retrieves all playlists for the current user by fetching all pages.
    private CompletionStage<List<String>> retrieveAllUserPlaylists() {
        return getUserIdAsync()
                .thenCompose(userId -> {
                    if (userId == null) {
                        logger.error("User ID could not be fetched.");
                        return CompletableFuture.completedFuture(List.of());
                    }
                    return retrievePlaylistsWithPagination(userId, 0, List.of());
                })
                .exceptionally(e -> {
                    logger.error("Error fetching playlists: {}", e.getMessage(), e);
                    return List.of();
                });
    }

    // Helper method to fetch all playlists with pagination
    private CompletionStage<List<String>> retrievePlaylistsWithPagination(String userId, int offset, List<String> accumulatedPlaylists) {
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
                        return retrievePlaylistsWithPagination(userId, offset + 50, updatedAccumulatedPlaylists);
                    } else {
                        // No more playlists to fetch, return the accumulated list
                        return CompletableFuture.completedFuture(updatedAccumulatedPlaylists);
                    }
                });
    }

    // Method to get liked songs, ensures token is valid before proceeding
    public CompletableFuture<Paging<SavedTrack>> getLikedSongs() {
        return ensureAccessTokenIsValid()
                .thenCompose(unused -> {
                    logger.info("Fetching user's liked songs");
                    GetUsersSavedTracksRequest request = spotifyApi.getUsersSavedTracks()
                            .limit(50)
                            .offset(0)
                            .build();
                    return request.executeAsync()
                            .exceptionally(e -> {
                                logger.error("Error fetching liked songs: {}", e.getMessage(), e);
                                return null;
                            });
                }).toCompletableFuture();
    }
}
