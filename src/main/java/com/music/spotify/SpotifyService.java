package com.music.spotify;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class SpotifyService {
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
        return spotifyApi.authorizationCodeUri()
                .state(state)
                .scope("user-read-email, user-read-private, playlist-read-private, playlist-read-collaborative")
                .show_dialog(true)
                .build()
                .executeAsync();
    }

    public void exchangeAuthorizationCodeAsync(String code) {
        this.authorizationCode = code;
        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authorizationCode).build();

        authorizationCodeRequest.executeAsync().thenAccept(credentials -> {
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            // Calculate and save the token expiration time
            accessTokenExpirationTime = System.currentTimeMillis() + (credentials.getExpiresIn() * 1000L);

            System.out.println("Access Token: " + credentials.getAccessToken());
            System.out.println("Refresh Token: " + credentials.getRefreshToken());
            System.out.println("Expires in: " + credentials.getExpiresIn());
        }).exceptionally(e -> {
            System.out.println("Error: " + e.getMessage());
            return null;
        });
    }

    public CompletableFuture<Void> refreshAccessTokenAsync() {
        AuthorizationCodeRefreshRequest refreshRequest = spotifyApi.authorizationCodeRefresh().build();

        return refreshRequest.executeAsync().thenAccept(credentials -> {
            spotifyApi.setAccessToken(credentials.getAccessToken());
            System.out.println("New Access Token: " + credentials.getAccessToken());
            System.out.println("Expires in: " + credentials.getExpiresIn());
        }).exceptionally(e -> {
            System.out.println("Error: " + e.getMessage());
            return null;
        });
    }

    // Check if user is logged in by verifying if the access token exists and is valid
    public boolean isLoggedIn() {
        String accessToken = spotifyApi.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        // Check if the token has expired
        long currentTime = System.currentTimeMillis();
        if (currentTime >= accessTokenExpirationTime) {
            System.out.println("Access token has expired.");
            return false;
        }
        return true;
    }

}
