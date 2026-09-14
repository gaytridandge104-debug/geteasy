import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/** Common English filler words to ignore when analyzing dream text. */
public class StopWords {
    public static final Set<String> SET = new HashSet<>(Arrays.asList(
        "the", "and", "was", "were", "for", "with", "that", "this", "from",
        "had", "have", "has", "not", "but", "you", "your", "his", "her",
        "them", "they", "then", "than", "into", "onto", "out", "over",
        "under", "again", "could", "would", "should", "did", "does",
        "felt", "feel", "feeling", "being", "been", "are", "our", "who",
        "what", "when", "where", "while", "there", "their", "some", "any",
        "every", "each", "all", "one", "two", "still", "just", "like",
        "even", "very", "really", "kept", "found", "looking", "looked"
    ));
}
