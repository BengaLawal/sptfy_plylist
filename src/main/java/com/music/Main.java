package com.music;

import com.music.spotify.SpotifyController;
import com.music.spotify.SpotifyService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.net.URI;

public class Main {

    public static void main(String[] args) {
        String clientId = args[1];
        String clientSecret = args[2];
        URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:5000/login/spotify/callback");

        // initialise javalin
        Javalin app = Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH)).start(5000);

        // Create Spotify service
        SpotifyService spotifyService = new SpotifyService(clientId, clientSecret, redirectUri);

        SpotifyController spotifyController = new SpotifyController(spotifyService);
        spotifyController.registerRoutes(app);
    }
}
