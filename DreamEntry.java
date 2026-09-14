import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DreamEntry {
    private final LocalDate date;
    private final String mood;
    private final String rawText;
    private final List<String> words;

    public DreamEntry(LocalDate date, String mood, String rawText) {
        this.date = date;
        this.mood = mood.toLowerCase().trim();
        this.rawText = rawText;
        this.words = tokenize(rawText);
    }

    private List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        String cleaned = text.toLowerCase().replaceAll("[^a-z\\s]", " ");
        for (String w : cleaned.split("\\s+")) {
            if (w.length() > 2 && !StopWords.SET.contains(w)) {
                result.add(w);
            }
        }
        return result;
    }

    public LocalDate getDate() { return date; }
    public String getMood() { return mood; }
    public String getRawText() { return rawText; }
    public List<String> getWords() { return words; }

    @Override
    public String toString() {
        return date + " [" + mood + "] " + rawText;
    }
}