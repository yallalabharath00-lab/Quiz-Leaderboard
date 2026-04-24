import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuizLeaderboard {

    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final String REG_NO = "2024CS101"; // 🔥 CHANGE THIS

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        // To store unique entries
        Set<String> uniqueSet = new HashSet<>();

        // Score aggregation
        Map<String, Integer> scores = new HashMap<>();

        // 🔁 Step 1: Poll API 10 times
        for (int i = 0; i < 10; i++) {

            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + i;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            JSONArray events = json.getJSONArray("events");

            // 🔁 Step 2 + 3: Process & deduplicate
            for (int j = 0; j < events.length(); j++) {

                JSONObject event = events.getJSONObject(j);

                String roundId = event.getString("roundId");
                String participant = event.getString("participant");
                int score = event.getInt("score");

                String key = roundId + "_" + participant;

                if (!uniqueSet.contains(key)) {
                    uniqueSet.add(key);

                    // 🔁 Step 4: Aggregate scores
                    scores.put(participant,
                            scores.getOrDefault(participant, 0) + score);
                }
            }

            System.out.println("Poll " + i + " completed");

            // ⏳ Mandatory delay
            Thread.sleep(5000);
        }

        // 🔁 Step 5: Create leaderboard
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());

        list.sort((a, b) -> b.getValue() - a.getValue());

        JSONArray leaderboard = new JSONArray();

        for (Map.Entry<String, Integer> entry : list) {
            JSONObject obj = new JSONObject();
            obj.put("participant", entry.getKey());
            obj.put("totalScore", entry.getValue());
            leaderboard.put(obj);
        }

        // 🔁 Step 6: Total score
        int total = scores.values().stream().mapToInt(Integer::intValue).sum();

        System.out.println("Total Score: " + total);
        System.out.println("Leaderboard: " + leaderboard.toString(2));

        // 🔁 Step 7: Submit
        JSONObject requestBody = new JSONObject();
        requestBody.put("regNo", REG_NO);
        requestBody.put("leaderboard", leaderboard);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Submission Response:");
        System.out.println(postResponse.body());
    }
}