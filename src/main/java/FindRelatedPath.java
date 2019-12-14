import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FindRelatedPath {
    private static JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    public static void main(String[] args) throws IOException {
        String id1 = "LI87PRgIKks"; // Cassette Boy vs Boris Johnson
        String id2 = "otCpCn0l4Wo"; // MC Hammer - You can't touch this
        String apiKey = loadApiKey();
        // Takes 2 videos and finds out if they are related, and by what paths

        ApacheHttpTransport transport = new ApacheHttpTransport();

        YouTube youtube = new YouTube.Builder(transport, jsonFactory, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        })
                .setApplicationName("My Project")
                .build();

        Set<String> checkedVideosIds = new HashSet<>();

        // Step 1 - is there a path
        // Step 2 - what is a shortest path - listing the videos
        // Step 3 - are there multiple paths

        // Increment depth counter
        // From current video
        // Request related videos
        // Check for reaching destination
        // Filter out already seen videos

        checkRelatedVideos(apiKey, checkedVideosIds, 1, youtube, id1, id2);
    }

    private static boolean checkRelatedVideos(String apiKey, Set<String> checkedVideoIds, int currentDepth, YouTube youtube, String videoId, String destinationVideoId) throws IOException {
        List<SearchResult> relatedVideos = findRelatedVideos(youtube, apiKey, videoId);
        if (canFindDestination(destinationVideoId, relatedVideos)) {
            System.out.println("Found a path");
            return true;
        } else {
            Set<String> videoIds = getUncheckedVideoIds(checkedVideoIds, relatedVideos);

            if (!videoIds.isEmpty()) {
                currentDepth++;
                for (String nestedVideoId : videoIds) {
                    checkedVideoIds.add(nestedVideoId);
                    if (checkRelatedVideos(apiKey, checkedVideoIds, currentDepth, youtube, nestedVideoId, destinationVideoId)) {
                        System.out.println("Found a path at depth " + currentDepth);
                        return true;
                    }
                }
            }
        }
        System.out.println("didn't find a path");
        return false;
    }

    private static Set<String> getUncheckedVideoIds(Set<String> checkedVideosIds, List<SearchResult> relatedVideos) {
        return relatedVideos.stream()
                .map(r -> r.getId().getVideoId())
                .filter(id -> !checkedVideosIds.contains(id))
                .collect(Collectors.toSet());
    }

    private static boolean canFindDestination(String id2, List<SearchResult> relatedVideos) {
        return relatedVideos.stream().anyMatch(
                result -> result.getId().getVideoId().equals(id2)
        );
    }

    private static List<SearchResult> findRelatedVideos(YouTube youtube, String apiKey, String videoId) throws IOException {
        System.out.println("DEBUG - making call to api...");
        YouTube.Search.List search = youtube.search().list("id, snippet");
        search.setKey(apiKey);
        search.setType("video");
        search.setRelatedToVideoId(videoId);

        SearchListResponse result = search.execute();

        return result.getItems();
    }


    private static String loadApiKey() throws IOException {
        List<String> lines = null;
        try (BufferedReader buffer = new BufferedReader(
                new InputStreamReader(FindRelatedPath.class.getResourceAsStream("/apikey.txt"))
                )) {
            lines = buffer.lines().collect(Collectors.toList());
        }

        return lines.get(0);
    }
}
