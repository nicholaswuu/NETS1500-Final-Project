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
    
    public Movie(String primaryTitle, String originalTitle, boolean isAdult, 
                 int startYear, int runtimeMinutes, String genres, double averageRating) {
        this.primaryTitle = primaryTitle;
        this.originalTitle = originalTitle;
        this.isAdult = isAdult;
        this.startYear = startYear;
        this.runtimeMinutes = runtimeMinutes;
        this.genres = genres;
        this.averageRating = averageRating;
    }
    
    // Getters
    public String getPrimaryTitle() { return primaryTitle; }
    public String getOriginalTitle() { return originalTitle; }
    public boolean isAdult() { return isAdult; }
    public int getStartYear() { return startYear; }
    public int getRuntimeMinutes() { return runtimeMinutes; }
    public String getGenres() { return genres; }
    public double getAverageRating() { return averageRating; }
    
    /**
     * Extracts the plot section of the movie from Wikipedia.
     * Attempts multiple URL patterns to handle Wikipedia's different naming conventions for movie pages.
     * 
     * @return The plot text if found, or a message indicating the plot was not found
     */
    public String extractWikipediaPlot() {
        List<String> possibleUrls = generatePossibleWikipediaUrls();
        
        for (String url : possibleUrls) {
            String plot = tryExtractPlotFromUrl(url);
            if (plot != null && !plot.isEmpty()) {
                return plot;
            }
        }
        
        return "Plot not found. Could not retrieve plot from Wikipedia for " + primaryTitle;
    }
    
    /**
     * Generates a list of possible Wikipedia URLs for the movie.
     * 
     * @return List of URLs to try
     */
    private List<String> generatePossibleWikipediaUrls() {
        List<String> urls = new ArrayList<>();
        
        try {
            // Generate base URL from primary title
            String baseTitle = primaryTitle.replace(' ', '_');
            baseTitle = URLEncoder.encode(baseTitle, StandardCharsets.UTF_8.toString())
                    .replace("+", "_")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .replace("%27", "'");
            
            // Try different variations of the URL
            urls.add("https://en.wikipedia.org/wiki/" + baseTitle);
            urls.add("https://en.wikipedia.org/wiki/" + baseTitle + "_(film)");
            urls.add("https://en.wikipedia.org/wiki/" + baseTitle + "_(" + startYear + "_film)");
            
            // Try with original title if different
            if (!originalTitle.equals(primaryTitle)) {
                String originalTitleEncoded = originalTitle.replace(' ', '_');
                originalTitleEncoded = URLEncoder.encode(originalTitleEncoded, StandardCharsets.UTF_8.toString())
                        .replace("+", "_")
                        .replace("%28", "(")
                        .replace("%29", ")")
                        .replace("%27", "'");
                
                urls.add("https://en.wikipedia.org/wiki/" + originalTitleEncoded);
                urls.add("https://en.wikipedia.org/wiki/" + originalTitleEncoded + "_(film)");
                urls.add("https://en.wikipedia.org/wiki/" + originalTitleEncoded + "_(" + startYear + "_film)");
            }
        } catch (Exception e) {
            System.err.println("Error encoding Wikipedia URL: " + e.getMessage());
        }
        
        return urls;
    }
    
    /**
     * Attempts to extract the plot section from a given Wikipedia URL.
     * 
     * @param url The Wikipedia URL to try
     * @return The plot text if found, or null if not found
     */
    private String tryExtractPlotFromUrl(String url) {
        try {
            // Connect to the Wikipedia page
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();
            
            // First, look for sections with "Plot" heading
            Elements plotHeadings = doc.select("h2:has(span#Plot), h2:has(span#Synopsis), h2:has(span#Story), h2:has(span#Storyline)");
            
            if (!plotHeadings.isEmpty()) {
                Element plotHeading = plotHeadings.first();
                StringBuilder plotText = new StringBuilder();
                
                // Get all paragraphs until the next heading
                Element current = plotHeading.nextElementSibling();
                while (current != null && !current.tagName().matches("h[1-6]")) {
                    if (current.tagName().equals("p")) {
                        plotText.append(current.text()).append("\n\n");
                    }
                    current = current.nextElementSibling();
                }
                
                if (plotText.length() > 0) {
                    return plotText.toString().trim();
                }
            }
            
            // If no specific plot section, try to find the first few paragraphs in the main content
            // This is a fallback for movies where the plot might not have its own section
            Elements contentParas = doc.select(".mw-parser-output > p");
            if (!contentParas.isEmpty()) {
                StringBuilder mainContent = new StringBuilder();
                
                // Take up to the first 3 substantial paragraphs
                int count = 0;
                for (Element para : contentParas) {
                    String paraText = para.text().trim();
                    if (paraText.length() > 100) {  // Only consider substantial paragraphs
                        mainContent.append(paraText).append("\n\n");
                        count++;
                        if (count >= 3) break;
                    }
                }
                
                if (mainContent.length() > 0) {
                    return "Plot (extracted from main content):\n" + mainContent.toString().trim();
                }
            }
            
        } catch (IOException e) {
            // URL didn't work, will try the next one
            return null;
        } catch (Exception e) {
            System.err.println("Error extracting plot from " + url + ": " + e.getMessage());
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
               '}';
    }
}