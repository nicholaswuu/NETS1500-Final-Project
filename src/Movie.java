import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Movie {
    private String tconst;
    private String primaryTitle;
    private String originalTitle;
    private boolean isAdult;
    private int startYear;
    private int runtimeMinutes;
    private String genres;
    private double averageRating;
    private String plotDescription;

    /**
     * Create a Movie object and immediately fetch its plot from IMDb.
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
        this.plotDescription = extractImdbPlot(tconst);
    }

    // Getters
    public String getTconst() { return tconst; }
    public String getPrimaryTitle() { return primaryTitle; }
    public String getOriginalTitle() { return originalTitle; }
    public boolean isAdult() { return isAdult; }
    public int getStartYear() { return startYear; }
    public int getRuntimeMinutes() { return runtimeMinutes; }
    public String getGenres() { return genres; }
    public double getAverageRating() { return averageRating; }
    public String getPlotDescription() { return plotDescription; }




    /**
     * Scrapes the full user-submitted plot synopsis from IMDb's /plotsummary page,
     * removing duplicate paragraphs.
     *
     * @param tconst IMDb title ID
     * @return The combined unique synopsis paragraphs, or null if none found.
     */
    private String extractImdbPlot(String tconst) {
        String url = "https://www.imdb.com/title/" + tconst + "/plotsummary/";
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10_000)
                    .get();

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
            return result.isEmpty() ? null : result;

        } catch (Exception e) {
            System.err.println("Failed to fetch IMDb plot for " + tconst + ": " + e.getMessage());
            return null;
        }
    }






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