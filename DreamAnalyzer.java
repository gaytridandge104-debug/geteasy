import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class DreamAnalyzer {

    private final List<DreamEntry> entries = new ArrayList<>();

    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "happy", "free", "calm", "peaceful", "laughing", "beautiful", "warm",
        "clear", "soft", "waving", "friends", "garden", "sunlight", "flying",
        "joy", "joyful", "excited", "love", "loved", "loving", "smiling",
        "smile", "bright", "light", "safe", "comfort", "comfortable", "hug",
        "hugging", "relaxed", "relaxing", "gentle", "hope", "hopeful",
        "wonderful", "amazing", "magical", "sunshine", "rainbow", "dancing",
        "dance", "celebration", "celebrating", "reunion", "playful", "fun",
        "laughter", "victory", "winning", "proud", "grateful", "gratitude",
        "healing", "healed", "fly", "soaring", "adventure", "wonder"
    ));
    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
        "falling", "chased", "chasing", "dark", "afraid", "lost", "late",
        "scream", "shadow", "heavy", "locked", "rising", "loose", "endless",
        "fear", "fearful", "scared", "scary", "terrified", "terrifying",
        "panic", "panicking", "trapped", "drowning", "crying", "cry",
        "anger", "angry", "alone", "lonely", "abandoned", "abandonment",
        "death", "dying", "dead", "danger", "dangerous", "threat",
        "threatening", "nightmare", "monster", "screaming", "hurt",
        "hurting", "pain", "painful", "broken", "breaking", "collapse",
        "collapsing", "attack", "attacked", "blood", "bleeding", "sick",
        "sickness", "grief", "guilt", "ashamed", "shame", "helpless",
        "hopeless", "suffocating", "stuck", "burning", "fire", "cold",
        "freezing", "empty", "emptiness", "betrayed", "betrayal"
    ));

    public void loadFromCsv(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;
                LocalDate date = LocalDate.parse(parts[0].trim());
                String mood = parts[1].trim();
                String text = parts[2].trim();
                entries.add(new DreamEntry(date, mood, text));
            }
        }
    }

    public void addEntry(DreamEntry entry) {
        entries.add(entry);
    }

    public List<DreamEntry> getEntries() {
        return entries;
    }

    public Map<String, Integer> wordFrequency() {
        Map<String, Integer> freq = new HashMap<>();
        for (DreamEntry e : entries) {
            for (String w : e.getWords()) {
                freq.merge(w, 1, Integer::sum);
            }
        }
        return freq;
    }

    public List<Map.Entry<String, Integer>> topThemes(int n) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(wordFrequency().entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    public Map<String, Integer> coOccurringWith(String target) {
        Map<String, Integer> co = new HashMap<>();
        for (DreamEntry e : entries) {
            Set<String> words = new HashSet<>(e.getWords());
            if (words.contains(target)) {
                for (String w : words) {
                    if (!w.equals(target)) {
                        co.merge(w, 1, Integer::sum);
                    }
                }
            }
        }
        return co;
    }

    public int sentimentScore(DreamEntry e) {
        int score = 0;
        for (String w : e.getWords()) {
            if (POSITIVE_WORDS.contains(w)) score++;
            if (NEGATIVE_WORDS.contains(w)) score--;
        }
        return score;
    }

    public List<Map.Entry<LocalDate, Integer>> moodTrend() {
        List<Map.Entry<LocalDate, Integer>> trend = new ArrayList<>();
        List<DreamEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(DreamEntry::getDate));
        for (DreamEntry e : sorted) {
            trend.add(new AbstractMap.SimpleEntry<>(e.getDate(), sentimentScore(e)));
        }
        return trend;
    }

    public Map<String, Integer> moodTagCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DreamEntry e : entries) {
            counts.merge(e.getMood(), 1, Integer::sum);
        }
        return counts;
    }

    public String buildSummary() {
        if (entries.isEmpty()) return "No dream entries yet.";
        List<Map.Entry<String, Integer>> top = topThemes(1);
        String topWord = top.isEmpty() ? "nothing recurring" : top.get(0).getKey();
        int topCount = top.isEmpty() ? 0 : top.get(0).getValue();
        double avgSentiment = entries.stream().mapToInt(this::sentimentScore).average().orElse(0);
        String moodLean = avgSentiment > 0.3 ? "mostly positive" :
                           avgSentiment < -0.3 ? "mostly anxious" : "fairly neutral";
        return String.format(
            "Across %d entries, your most recurring dream symbol is \"%s\" (appeared %d times). " +
            "Overall mood trend: %s.",
            entries.size(), topWord, topCount, moodLean
        );
    }
}