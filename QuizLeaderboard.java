import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class QuizLeaderboard {

    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final String REG_NO = "2024CS101";

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        Set<String> uniqueSet = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + i;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<Event> events = parseEvents(response.body());

            for (Event event : events) {
                String roundId = event.roundId;
                String participant = event.participant;
                int score = event.score;

                String key = roundId + "_" + participant;

                if (!uniqueSet.contains(key)) {
                    uniqueSet.add(key);
                    scores.put(participant, scores.getOrDefault(participant, 0) + score);
                }
            }

            System.out.println("Poll " + i + " completed");
            Thread.sleep(5000);
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder leaderboardJson = new StringBuilder();
        leaderboardJson.append("[");

        for (int k = 0; k < list.size(); k++) {
            Map.Entry<String, Integer> entry = list.get(k);
            if (k > 0) {
                leaderboardJson.append(",");
            }
            leaderboardJson.append("{");
            leaderboardJson.append("\"participant\":").append(escapeJson(entry.getKey()));
            leaderboardJson.append(",\"totalScore\":").append(entry.getValue());
            leaderboardJson.append("}");
        }

        leaderboardJson.append("]");

        int total = scores.values().stream().mapToInt(Integer::intValue).sum();

        System.out.println("Total Score: " + total);
        System.out.println("Leaderboard: " + leaderboardJson.toString());

        String requestBody = "{\"regNo\":\"" + REG_NO + "\",\"leaderboard\":" + leaderboardJson.toString() + "}";

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Submission Response:");
        System.out.println(postResponse.body());
    }

    private static String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private static List<Event> parseEvents(String body) {
        String eventsKey = "\"events\"";
        int eventsIndex = body.indexOf(eventsKey);
        if (eventsIndex < 0) {
            return Collections.emptyList();
        }

        int arrayStart = body.indexOf('[', eventsIndex);
        if (arrayStart < 0) {
            return Collections.emptyList();
        }

        int arrayEnd = findMatchingBracket(body, arrayStart);
        if (arrayEnd < 0) {
            return Collections.emptyList();
        }

        String arrayBody = body.substring(arrayStart + 1, arrayEnd);
        List<Event> events = new ArrayList<>();
        int pos = 0;

        while (pos < arrayBody.length()) {
            int objectStart = arrayBody.indexOf('{', pos);
            if (objectStart < 0) {
                break;
            }
            int objectEnd = findMatchingBracket(arrayBody, objectStart);
            if (objectEnd < 0) {
                break;
            }

            String objectBody = arrayBody.substring(objectStart + 1, objectEnd);
            String roundId = parseJsonString(objectBody, "roundId");
            String participant = parseJsonString(objectBody, "participant");
            Integer score = parseJsonNumber(objectBody, "score");

            if (roundId != null && participant != null && score != null) {
                events.add(new Event(roundId, participant, score));
            }

            pos = objectEnd + 1;
        }

        return events;
    }

    private static int findMatchingBracket(String text, int startIndex) {
        char open = text.charAt(startIndex);
        char close = open == '{' ? '}' : open == '[' ? ']' : 0;
        if (close == 0) {
            return -1;
        }

        int depth = 0;
        boolean inString = false;
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                i++; // skip escaped char
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String parseJsonString(String body, String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = body.indexOf(quotedKey);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = body.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex < 0) {
            return null;
        }
        int valueStart = body.indexOf('"', colonIndex);
        if (valueStart < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = valueStart + 1; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escape) {
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(c); break;
                }
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                return sb.toString();
            }
            sb.append(c);
        }
        return null;
    }

    private static Integer parseJsonNumber(String body, String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = body.indexOf(quotedKey);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = body.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex < 0) {
            return null;
        }
        int pos = colonIndex + 1;
        while (pos < body.length() && Character.isWhitespace(body.charAt(pos))) {
            pos++;
        }
        int end = pos;
        while (end < body.length() && (Character.isDigit(body.charAt(end)) || body.charAt(end) == '-')) {
            end++;
        }
        if (end == pos) {
            return null;
        }
        try {
            return Integer.parseInt(body.substring(pos, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class Event {
        final String roundId;
        final String participant;
        final int score;

        Event(String roundId, String participant, int score) {
            this.roundId = roundId;
            this.participant = participant;
            this.score = score;
        }
    }
}
