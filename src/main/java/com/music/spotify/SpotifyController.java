package com.music.spotify;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;
import java.util.UUID;

public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    // Register routes, including the login and callback routes
    public void registerRoutes(Javalin app) {
        app.get("/login/spotify", this::login);
        app.get("/login/spotify/callback", this::callback);
        app.get("/spotify/auth/status", this::isLoggedIn);
    }

    // Redirect to Spotify login
    public void login(Context ctx) {
        // Generate a random `state` value to prevent CSRF attacks
        String state = UUID.randomUUID().toString();
        ctx.res().addHeader("Set-Cookie", "spotify_auth_state=" + state + "; Max-Age=600; SameSite=Lax; HttpOnly; Secure");  // Store `state` in a cookie for 600 seconds

        System.out.println(state);
        spotifyService.getAuthorizationUriAsync(state)
                .thenAccept(uri -> {
                    ctx.redirect(uri.toString());  // Redirect user to Spotify for authentication
                }).exceptionally(ex -> {
                    ctx.result("Error generating Spotify login URL: " + ex.getMessage());
                    return null;
                });
    }

    // Handle the Spotify callback
    public void callback(Context ctx) {
        String state = ctx.queryParam("state");  // Get state returned from Spotify
        String storedState = ctx.cookie("spotify_auth_state");  // Get stored `state` from cookie
        String code = ctx.queryParam("code");  // read returned code from spotify in url param

        System.out.println(state);
        // Check if `state` matches the stored value
        if (state == null || !state.equals(storedState)) {
            ctx.result("Invalid state parameter! Possible CSRF attack.");
            return;
        }

        // Remove the cookie after verifying the state
        ctx.removeCookie("spotify_auth_state");

        if (code != null) {
            spotifyService.exchangeAuthorizationCodeAsync(code);
            ctx.result("Successfully authenticated with Spotify!");
            ctx.redirect("/");
        } else {
            ctx.result("Authentication failed.");
        }
    }

    // Expose the login status
    public void isLoggedIn(Context ctx) {
        boolean loggedIn = spotifyService.isLoggedIn();
        ctx.json(Map.of("spotifyLoggedIn", loggedIn));
    }
}
