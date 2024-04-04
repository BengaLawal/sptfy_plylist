package fileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileProcessor {
    Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private List<List<String>> data;

    public FileProcessor(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            System.err.println("Unsupported file format.");
        } else{
            data = processFile(filePath);
        }
    }

    private List<List<String>> processFile(String filePath) {
        String extension = FilenameUtils.getExtension(filePath);
        return switch (extension.toLowerCase()) {
            case "json" -> processJsonFile(filePath);
            case "csv" -> processCsvFile(filePath);
            default -> {
                System.err.println("Unsupported file format.");
                yield new ArrayList<>();
            }
        };
    }

    private List<List<String>> processJsonFile(String filePath) {
        //List<String> localTrackURIs = new ArrayList<>();
        List<List<String>> spotifyURIs = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(filePath));

            JsonNode playlistsNode = rootNode.get("playlists");
            if (playlistsNode != null && playlistsNode.isArray()) {
                for (JsonNode playlistNode : playlistsNode) {
                    List<String> playlistInfo = new ArrayList<>();
                    playlistInfo.add(0, playlistNode.get("name").asText());
                    playlistInfo.add(1, playlistNode.get("description").asText());

                    JsonNode itemsNode = playlistNode.get("items");
                    if (itemsNode != null && itemsNode.isArray()) {
                        for (JsonNode itemNode : itemsNode) {
                            JsonNode trackNode = itemNode.get("track");
                            JsonNode localTrackNode = itemNode.get("localTrack");
                            JsonNode episodeNode = itemNode.get("episode");

                            // the item could either have a spotify track, podcast episode or local file track
                            if (!Objects.equals(trackNode.asText(), "null")){
                                String trackUri = trackNode.get("trackUri").asText();
                                playlistInfo.add(trackUri);
                            } else if (!Objects.equals(localTrackNode.asText(), "null")){
                                String uri = localTrackNode.get("uri").asText();
                                //localTrackURIs.add(uri);
                                playlistInfo.add(uri);
                            } else if (!Objects.equals(episodeNode.asText(), "null")){
                                String episodeUri = episodeNode.get("episodeUri").asText();
                                playlistInfo.add(episodeUri);
                            }
                        }
                    }
                    // add playlist list into spotifyURIs list collection
                    spotifyURIs.add(playlistInfo);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error processing JSON file: " + filePath, e);
        }
        List<List<String>> result = new ArrayList<>();
        result.addAll(0, spotifyURIs);
        //result.add(localTrackURIs);
        return result;
    }

    private List<List<String>> processCsvFile(String filePath) {
        return null;
    }

    public List<List<String>> getData() {
        return data;
    }
}
