import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    /**
     * Create a Movie object and fetch its plot from IMDb.
     *
     * @param tconst IMDb title ID, e.g. "tt2649356"
     * @param primaryTitle The main title
     * @param originalTitle The original title (if different)
     * @param isAdult True if adult content
     * @param startYear Release year
     * @param runtimeMinutes Duration in minutes
     * @param genres Comma-separated genres
     * @param averageRating IMDb average rating
     */
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

    /**
     * Create a Movie object given the synopsis.
     *
     * @param tconst IMDb title ID, e.g. "tt2649356"
     * @param primaryTitle The main title
     * @param originalTitle The original title (if different)
     * @param isAdult True if adult content
     * @param startYear Release year
     * @param runtimeMinutes Duration in minutes
     * @param genres Comma-separated genres
     * @param averageRating IMDb average rating
     * @param synopsisStr Movie synopsis
     */
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

    /**
     * Getters
     */
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

    /**
     * This method will return the term frequency for a given word.
     * If this document doesn't contain the word, it will return 0
     * @param word The word to look for
     * @return the term frequency for this word in this document
     */
    public double getTermFrequency(String word) {
        if (termFrequency == null) {
            termFrequency = processRawSynopsis(synopsisStr);
        }
        return termFrequency.getOrDefault(word, 0);
    }

    /**
     * Processes a raw synopsis to extract term frequencies.
     * Splits text into words, removes non-alphanumeric characters, converts to lowercase,
     * and counts the frequency of each term.
     *
     * @param rawText The raw synopsis to process (can be null)
     * @return HashMap containing each term and its frequency
     */
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

    /**
     * Scrapes the full user-submitted plot synopsis from IMDb's /plotsummary page,
     * removing duplicate paragraphs.
     *
     * @return The combined unique synopsis paragraphs, or null if none found.
     */
    private String extractImdbSynopsis() {
        String url = "https://www.imdb.com/title/" + tconst + "/plotsummary/";
        try {
            Document doc = Jsoup.connect(url).get();

            // select each synopsis block under the metadata list items
            Elements blocks = doc.select(
                    "li.ipc-metadata-list__item[data-testid=list-item] div.ipc-html-content-inner-div"
            );
            if (blocks.isEmpty()) {
                return null;
            }

            StringBuilder fullPlot = new StringBuilder();
            // track paragraphs we've already added to avoid duplicates
            Set<String> seen = new HashSet<>();

            for (Element block : blocks) {
                // convert <br> runs to paragraph breaks
                String html = block.html();
                String withPars = html.replaceAll("(?i)(<br\\s*/?>\\s*)+", "\n\n");

                // strip any remaining tags
                String[] paras = Jsoup.parse(withPars).text().trim().split("\\n\\n");

                for (String para : paras) {
                    String trimmed = para.trim();
                    if (!trimmed.isEmpty() && seen.add(trimmed)) {
                        fullPlot.append(trimmed).append("\n\n");
                    }
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