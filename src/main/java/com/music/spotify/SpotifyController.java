package com.music.spotify;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    // Register routes, including the login and callback routes
    public void registerRoutes(Javalin app) {
        app.get("/", this::loginPage);  // Default route serving index.html
        app.get("/login/spotify", this::login);
        app.get("/login/spotify/callback", this::callback);
    }

    // Serve the login page
    private void loginPage(@NotNull Context ctx) {
        ctx.redirect("/index.html");
    }

    // Redirect to Spotify login
    public void login(Context ctx) {
        spotifyService.getAuthorizationUriAsync()
                .thenAccept(uri -> {
                    ctx.redirect(uri.toString());  // Redirect user to Spotify for authentication
                }).exceptionally(ex -> {
                    ctx.result("Error generating Spotify login URL: " + ex.getMessage());
                    return null;
                });
    }

    // Handle the Spotify callback
    public void callback(Context ctx) {
        String code = ctx.queryParam("code");  // read returned code from spotify in url param
        if (code != null) {
            spotifyService.exchangeAuthorizationCodeAsync(code);
            ctx.result("Successfully authenticated with Spotify!");
            ctx.redirect("/");
        } else {
            ctx.result("Authentication failed.");
        }
    }
}
