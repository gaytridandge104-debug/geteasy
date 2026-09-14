import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the Anthropic Claude API to generate a real AI interpretation
 * of a dream, instead of just statistics.
 *
 * Requires an API key from https://console.anthropic.com to be set as
 * an environment variable named ANTHROPIC_API_KEY before running the app.
 */
public class DreamInterpreter {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-5";

    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Sends the dream text to Claude and returns a short interpretation.
     * Throws an exception with a clear message if the API key is missing
     * or the request fails, so the GUI can show something useful.
     */
    public String interpret(String dreamText) throws IOException, InterruptedException {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "No API key found. Set the ANTHROPIC_API_KEY environment variable first."
            );
        }

        String prompt =
            "You are a thoughtful dream interpretation assistant for a student project. " +
            "In 3-4 sentences, offer a plausible psychological interpretation of the " +
            "following dream. Be specific to the details given, not generic. " +
            "Dream: " + dreamText;

        String requestBody = "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"max_tokens\":300,"
            + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API error (status " + response.statusCode() + "): " + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    /** Pulls the "text" field out of the JSON response without needing a JSON library. */
    private String extractTextFromResponse(String json) {
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return "Could not read interpretation from API response.";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
