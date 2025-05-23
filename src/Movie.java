import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;

// Represents a movie with metadata and text processing utilities
public class Movie implements Comparable<Movie>{
    private String tconst;
    private String primaryTitle;
    private String originalTitle;
    private boolean isAdult;
    private int startYear;
    private int runtimeMinutes;
    private String genres;
    private double averageRating;
    private String synopsisStr;
    private HashMap<String, Integer> termFrequency;

    // Create movie and load plot from IMDb
    public Movie(String tconst,
                 String primaryTitle,
                 String originalTitle,
                 boolean isAdult,
                 int startYear,
                 int runtimeMinutes,
                 String genres,
                 double averageRating) {
        this.tconst = tconst;
        this.primaryTitle = primaryTitle;
        this.originalTitle = originalTitle;
        this.isAdult = isAdult;
        this.startYear = startYear;
        this.runtimeMinutes = runtimeMinutes;
        this.genres = genres;
        this.averageRating = averageRating;
        this.synopsisStr = extractImdbSynopsis();
    }

    // Create movie with provided synopsis
    public Movie(String tconst,
                 String primaryTitle,
                 String originalTitle,
                 boolean isAdult,
                 int startYear,
                 int runtimeMinutes,
                 String genres,
                 double averageRating,
                 String synopsisStr) {
        this.tconst = tconst;
        this.primaryTitle = primaryTitle;
        this.originalTitle = originalTitle;
        this.isAdult = isAdult;
        this.startYear = startYear;
        this.runtimeMinutes = runtimeMinutes;
        this.genres = genres;
        this.averageRating = averageRating;
        this.synopsisStr = synopsisStr;
    }

    // Simple getters
    public String getTconst() { return tconst; }
    public String getPrimaryTitle() { return primaryTitle; }
    public String getOriginalTitle() { return originalTitle; }
    public boolean isAdult() { return isAdult; }
    public int getStartYear() { return startYear; }
    public int getRuntimeMinutes() { return runtimeMinutes; }
    public String getGenres() { return genres; }
    public double getAverageRating() { return averageRating; }
    public String getSynopsisString() { return synopsisStr; }


    public Set<String> getTermList() {
        if (termFrequency == null && getSynopsisString() != null) {
            termFrequency = processRawSynopsis(synopsisStr);
        }
        return termFrequency.keySet();
    }

    // Return term frequency (0 if missing)
    public double getTermFrequency(String word) {
        if (termFrequency == null) {
            termFrequency = processRawSynopsis(synopsisStr);
        }
        return termFrequency.getOrDefault(word, 0);
    }

    // Extract and count terms from text
    public HashMap<String, Integer> processRawSynopsis(String rawText) {
        HashMap<String, Integer> termFrequency = new HashMap<>();

        // Return empty map if text is null or empty
        if (rawText == null || rawText.isEmpty()) {
            return termFrequency;
        }

        // Split text into words
        String[] words = rawText.split("\\s+");

        // Process each word
        for (String word : words) {
            // Remove non-alphanumeric characters and convert to lowercase
            String term = word.replaceAll("[^A-Za-z0-9]", "").toLowerCase();

            // Add non-empty terms to frequency map
            if (!term.isEmpty()) {
                // Use merge for a more concise way to update frequencies
                termFrequency.merge(term, 1, Integer::sum);
            }
        }
        return termFrequency;
    }

    // Fetch and clean plot synopsis from IMDb
    private String extractImdbSynopsis() {
        String url = "https://www.imdb.com/title/" + tconst + "/plotsummary/";
        try {
            Document doc = Jsoup.connect(url).get();

            Elements blocks = doc.select("div[data-testid='sub-section-synopsis']");
            if (blocks.isEmpty()) {
                return null;
            }

            StringBuilder fullPlot = new StringBuilder();
            Set<String> seen = new HashSet<>();


            Element block = blocks.first();
            String text = block.text();
            String[] paras = text.split("\\n\\n");

            for (String para : paras) {
                String trimmed = para.trim();
                if (!trimmed.isEmpty() && seen.add(trimmed)) {
                    fullPlot.append(trimmed).append("\n\n");
                }
            }

            String result = fullPlot.toString().trim();

            if (result.isEmpty()) {
                System.out.println("No synopsis found for " + primaryTitle);
                return null;
            }

            return result;

        } catch (Exception e) {
            System.err.println("Failed to fetch IMDb plot for " + tconst + ": " + e.getMessage());
            return null;
        }
    }

    // Simple string representation
    @Override
    public String toString() {
        return "Movie{" +
               "primaryTitle='" + primaryTitle + '\'' +
               ", originalTitle='" + originalTitle + '\'' +
               ", isAdult=" + isAdult +
               ", startYear=" + startYear +
               ", runtimeMinutes=" + runtimeMinutes +
               ", genres='" + genres + '\'' +
               ", averageRating=" + averageRating +
                "\n synopsis=" + synopsisStr +
               '}';
    }

    @Override
    public int compareTo(Movie other) {
        return primaryTitle.compareTo(other.getPrimaryTitle());
    }
}