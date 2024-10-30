package com.music.spotify;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpotifyController {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);
    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    // Register routes, including the login and callback routes
    public void registerRoutes(Javalin app) {
        app.get("/login/spotify", this::login);
        app.get("/login/spotify/callback", this::callback);
        app.get("/spotify/auth/status", this::isLoggedIn);
        app.get("/playlists", this::playlists);
        app.get("/spotify/refresh-token", this::refreshAccessToken);
    }

    private void refreshAccessToken(Context context) {
        spotifyService.refreshAccessTokenAsync()
                .thenAccept(unused -> {
                    logger.info("Refreshed access token successfully");
                    context.status(200);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to refresh access token", ex);
                    context.status(401); // Unauthorized, token refresh failed
                    return null;
                });
    }

    public void playlists(Context ctx) {
        ctx.future(() -> spotifyService.fetchUserPlaylists()
                .thenApply(playlists -> {
                    if (playlists == null || playlists.isEmpty()) {
                        logger.warn("No playlists found for the user.");
                        return Map.of("playlists", List.of()); // Return an empty list if no playlists are found
                    } else {
                        logger.info("Retrieved playlists: {}", playlists);
                        return Map.of("playlists", playlists);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error retrieving playlists: {}", ex.getMessage(), ex);
                    ctx.status(500);
                    return Map.of("error", Collections.singletonList("Error retrieving playlists: " + ex.getMessage()));
                })
                .thenAccept(ctx::json).toCompletableFuture()
        );
    }

    // Redirect to Spotify login
    public void login(Context ctx) {
        // Generate a random `state` value to prevent CSRF attacks
        String state = UUID.randomUUID().toString();
        ctx.res().addHeader("Set-Cookie", "spotify_auth_state=" + state + "; Max-Age=600; SameSite=Lax; HttpOnly; Secure");  // Store `state` in a cookie for 600 seconds

        spotifyService.getAuthorizationUriAsync(state)
                .thenAccept(uri -> {
                    logger.info("Redirecting to Spotify for authentication");
                    ctx.redirect(uri.toString());  // Redirect user to Spotify for authentication
                }).exceptionally(ex -> {
                    logger.error("Error generating Spotify login URL: " + ex.getMessage(), ex);
                    ctx.result("Error generating Spotify login URL: " + ex.getMessage());
                    return null;
                });
    }

    // Handle the Spotify callback
    public void callback(Context ctx) {
        String state = ctx.queryParam("state");  // Get state returned from Spotify
        String storedState = ctx.cookie("spotify_auth_state");  // Get stored `state` from cookie
        String code = ctx.queryParam("code");  // read returned code from spotify in url param

        // Check if `state` matches the stored value
        if (state == null || !state.equals(storedState)) {
            logger.warn("Invalid state parameter! Possible CSRF attack.");
            ctx.result("Invalid state parameter! Possible CSRF attack.");
            return;
        }

        // Remove the cookie after verifying the state
        ctx.removeCookie("spotify_auth_state");

        if (code != null) {
            spotifyService.exchangeAuthorizationCodeAsync(code);
            logger.info("Successfully authenticated with Spotify");

            ctx.res().setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            ctx.res().setHeader("Pragma", "no-cache");
            ctx.res().setHeader("Expires", "0");

            ctx.result("Successfully authenticated with Spotify!");
            ctx.redirect("/");
        } else {
            logger.error("Authentication failed.");
            ctx.redirect("/");
        }
    }

    // Expose the login status
    public void isLoggedIn(Context ctx) {
        boolean loggedIn = spotifyService.isLoggedIn();
        logger.info("Spotify login status: {}", loggedIn);
        ctx.json(Map.of("spotifyLoggedIn", loggedIn));
    }
}
