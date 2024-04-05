package fileProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import joinery.DataFrame;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileProcessor {
    Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private List<List<String>> data;

    /**
     * Class Constructor
     * @param filePath path to file
     */
    public FileProcessor(String filePath) {
        if (filePath == null || filePath.isEmpty() || !new File(filePath).exists()) {
            logger.log(Level.SEVERE, "Specify a path for an existing file");
            return;
        }
        data = processFile(filePath);
    }

    /**
     * Determines how to process the file based on its extension
     *
     * @param filePath path for file
     * @return A list within a list containing details for the playlists
     */
    private List<List<String>> processFile(String filePath) {
        String extension = FilenameUtils.getExtension(filePath);
        return switch (extension.toLowerCase()) {
            case "json" -> processJsonFile(filePath);
            case "csv" -> processCsvFile(filePath);
            default -> {
                logger.log(Level.SEVERE, filePath + "is neither a csv or json file");
                yield new ArrayList<>();
            }
        };
    }

    /**
     * Gets data from Json file
     *
     * @param filePath path for file
     * @return A list within a list containing details for the playlists
     */
    private List<List<String>> processJsonFile(String filePath) {
        List<List<String>> jsonData = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(filePath));

            JsonNode playlistsNode = rootNode.get("playlists");
            if (playlistsNode != null && playlistsNode.isArray()) {
                for (JsonNode playlistNode : playlistsNode) {
                    List<String> playlistInfo = new ArrayList<>();
                    playlistInfo.add(0, playlistNode.get("name").asText());  // add name of playlist
                    playlistInfo.add(1, playlistNode.get("description").asText());  // add description

                    JsonNode itemsNode = playlistNode.get("items");
                    if (itemsNode != null && itemsNode.isArray()) {
                        for (JsonNode itemNode : itemsNode) {
                            JsonNode trackNode = itemNode.get("track");
                            JsonNode localTrackNode = itemNode.get("localTrack");
                            JsonNode episodeNode = itemNode.get("episode");

                            // the item could either have a spotify track, podcast episode or local file track
                            if (!Objects.equals(trackNode.asText(), "null")) {
                                String trackUri = trackNode.get("trackUri").asText();
                                playlistInfo.add(trackUri);
                            } else if (!Objects.equals(localTrackNode.asText(), "null")) {
                                String uri = localTrackNode.get("uri").asText();
                                playlistInfo.add(uri);
                            } else if (!Objects.equals(episodeNode.asText(), "null")) {
                                String episodeUri = episodeNode.get("episodeUri").asText();
                                playlistInfo.add(episodeUri);
                            }
                        }
                    }
                    // add playlist list into spotifyURIs list collection
                    jsonData.add(playlistInfo);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error processing JSON file: " + filePath, e);
        }
        return jsonData;
    }


    /**
     * Gets data from CSV file
     *
     * @param filePath path for file
     * @return A list within a list containing details for the playlists
     */
    private List<List<String>> processCsvFile(String filePath) {
        List<List<String>> csvData = new ArrayList<>();
        String playlistName = FilenameUtils.removeExtension(filePath)
                .split("/", 2)[1]  // remove everything before /
                .replace("_", " ");
        try {
            DataFrame<Object> df = DataFrame.readCsv(new File(filePath).toString(), ",");
            List<Object> trackURIs = df.col("Track URI");  // returns a list of only track URIs

            // Convert List<Object> to List<String>
            List<String> trackURIStrings = new ArrayList<>();
            trackURIStrings.add(0, playlistName);  // add name of the playlist
            trackURIStrings.add(1, null); // add null as description
            for (Object obj : trackURIs) {
                if (obj != null) {
                    trackURIStrings.add(obj.toString());
                }
            }
            csvData.add(trackURIStrings);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error processing CSV file: " + filePath, e);
        }
        return csvData;
    }

    /**
     * Getter for data variable
     * @return data
     */
    public List<List<String>> getData() {
        return data;
    }
}
