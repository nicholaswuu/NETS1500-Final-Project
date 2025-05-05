import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Movie {
    private String primaryTitle;
    private String originalTitle;
    private boolean isAdult;
    private int startYear;
    private int runtimeMinutes;
    private String genres;
    private double averageRating;
    private String plotDescription;
    
    public Movie(String primaryTitle, String originalTitle, boolean isAdult, 
                 int startYear, int runtimeMinutes, String genres, double averageRating) {
        this.primaryTitle = primaryTitle;
        this.originalTitle = originalTitle;
        this.isAdult = isAdult;
        this.startYear = startYear;
        this.runtimeMinutes = runtimeMinutes;
        this.genres = genres;
        this.averageRating = averageRating;
        this.plotDescription = extractWikipediaPlot();
    }
    
    // Getters
    public String getPrimaryTitle() { return primaryTitle; }
    public String getOriginalTitle() { return originalTitle; }
    public boolean isAdult() { return isAdult; }
    public int getStartYear() { return startYear; }
    public int getRuntimeMinutes() { return runtimeMinutes; }
    public String getGenres() { return genres; }
    public double getAverageRating() { return averageRating; }
    public String getPlotDescription() { return plotDescription; }
    
    /**
     * Extracts the plot section of the movie from Wikipedia.
     * 
     * @return The plot text if found, or null if not found
     */
    public String extractWikipediaPlot() {
        List<String> possibleUrls = generatePossibleWikipediaUrls();
        
        for (String url : possibleUrls) {
            String plot = tryExtractPlotFromUrl(url);
            if (plot != null && !plot.isEmpty()) {
                return plot;
            }
        }
        System.out.println("No plot description found: " + primaryTitle);
        return null;
    }
    
    /**
     * Generates a list of possible Wikipedia URLs for the movie.
     * 
     * @return List of URLs to try
     */
    private List<String> generatePossibleWikipediaUrls() {
        List<String> urls = new ArrayList<>();
        
        try {
            // Generate base URL using primary title
            String baseTitle = primaryTitle.replace(' ', '_');
            baseTitle = URLEncoder.encode(baseTitle, StandardCharsets.UTF_8)
                    .replace("'", "%27");

            // Try different variations of the URL
            urls.add("https://en.wikipedia.org/wiki/" + baseTitle + "_(" + startYear + "_film)");
            urls.add("https://en.wikipedia.org/wiki/" + baseTitle + "_(film)");
            urls.add("https://en.wikipedia.org/wiki/" + baseTitle);

            // Try with original title
            if (!originalTitle.equals(primaryTitle)) {
                String originalTitleEncoded = originalTitle.replace(' ', '_');
                originalTitleEncoded = URLEncoder.encode(originalTitleEncoded, StandardCharsets.UTF_8)
                        .replace("'", "%27");

                urls.add("https://en.wikipedia.org/wiki/" + originalTitleEncoded + "_(" + startYear + "_film)");
                urls.add("https://en.wikipedia.org/wiki/" + originalTitleEncoded + "_(film)");
                urls.add("https://en.wikipedia.org/wiki/" + originalTitleEncoded);
            }
        } catch (Exception e) {
            System.err.println("Error encoding Wikipedia URL: " + e.getMessage());
        }
        
        return urls;
    }
    
    /**
     * Extracts the plot section from a Wikipedia URL.
     * 
     * @param url The Wikipedia URL to try
     * @return The plot text if found, or null if not found
     */
    private String tryExtractPlotFromUrl(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements plotHeaders = doc.select("h2[id^=Plot], h2[id=Synopsis]");
            
            if (!plotHeaders.isEmpty()) {
                Element plotHeader = plotHeaders.first();
                Element plotDiv = plotHeader.parent();
                StringBuilder plotText = new StringBuilder();
                
                // Get all paragraphs until next heading
                Element current = plotDiv.nextElementSibling();
                while (current != null && !current.hasClass("mw-heading2")) {
                    if (current.tagName().equals("p")) {
                        plotText.append(current.text()).append("\n\n");
                    }
                    current = current.nextElementSibling();
                }
                
                if (!plotText.isEmpty()) {
                    return plotText.toString().trim();
                }
            }
        } catch (IOException e) {
            // URL didn't work, try the next one
            return null;
        }
        
        return null;
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
                "\n description=" + plotDescription +
               '}';
    }
}