import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FindRelatedPath {
    private static final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    public static void main(String[] args) throws IOException {
        String id1 = "LI87PRgIKks"; // Cassette Boy vs Boris Johnson
        String id2 = "otCpCn0l4Wo"; // MC Hammer - You can't touch this
        String apiKey = loadApiKey();
        // Takes 2 videos and finds out if they are related, and by what paths
        System.out.println("DEBUG 1");
        ApacheHttpTransport transport = new ApacheHttpTransport();
        System.out.println("DEBUG 2");
        YouTube youtube = new YouTube.Builder(transport, jsonFactory, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
                System.out.println("DEBUG 3");
            }
        })
                .setApplicationName("My Project") // This may need to match
                .build();
        System.out.println("DEBUG 4");
        Set<String> checkedVideosIds = new HashSet<>();

        // Step 1 - is there a path
        // Step 2 - what is a shortest path - listing the videos
        // Step 3 - are there multiple paths
        // Optimisations and extensions
        //  / only fetch ids for initial pass through, then re-query with those ids for collating title information
        //  - Establish whether specifying a maximum results > default (10) produces more videos to include in consideration

        System.out.println("DEBUG 5");
        Deque<String> pathVideos = new ArrayDeque<>();
        checkRelatedVideos(apiKey, checkedVideosIds, 1, pathVideos, youtube, id1, id2);
    }

    private static boolean checkRelatedVideos(String apiKey, Set<String> checkedVideoIds, int currentDepth, Deque<String> pathVideos, YouTube youtube, String videoId, String destinationVideoId) throws IOException {

        System.out.println("DEBUG 6");
        List<SearchResult> relatedVideos = findRelatedVideos(youtube, apiKey, videoId);
        if (canFindDestination(destinationVideoId, relatedVideos)) {
            System.out.println("Found a path");
            return true;
        } else {
            Set<String> videoIds = getUncheckedVideoIds(checkedVideoIds, relatedVideos);

            if (!videoIds.isEmpty()) {
                currentDepth++;
                // FIXME: Something probably going wrong here - burning API quota
//                for (String nestedVideoId : videoIds) {
//                    checkedVideoIds.add(nestedVideoId);
//                    if (checkRelatedVideos(apiKey, checkedVideoIds, currentDepth, pathVideos, youtube, nestedVideoId, destinationVideoId)) {
//                        pathVideos.add(nestedVideoId);
//                        System.out.println("Found a path at depth " + currentDepth);
//                        return true;
//                    }
//                }
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
        // Single call costs 100 query quota, with a daily quota limit of  - see if there is something in the API for getting the video directly by id
        YouTube.Search.List search = youtube.search().list("id");
        search.setKey(apiKey);
        search.setType("video");
        search.setRelatedToVideoId(videoId);
        search.setMaxResults(10L);

        SearchListResponse result = search.execute();

        System.out.println("result items: " + result.getItems().size());
        System.out.println("total results: " + result.getPageInfo().getTotalResults());

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
