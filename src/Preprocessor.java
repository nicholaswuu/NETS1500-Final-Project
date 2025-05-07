import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preprocessor {
    
    private List<Movie> processedMovies;
    
    public Preprocessor() {
        processedMovies = new ArrayList<>();
    }
    
    public void processData(String basicsFilePath, String ratingsFilePath) {
        try {
            // Read and filter basics data (only movies)
            Map<String, String[]> moviesBasics = readAndFilterBasics(basicsFilePath);
            System.out.println("Filtered basics data: " + moviesBasics.size() + " movies");
            
            // Step 2: Read and filter ratings data (only those with > 20000 votes)
            Map<String, String[]> highVoteRatings = readAndFilterRatings(ratingsFilePath);
            System.out.println("Filtered ratings data: " + highVoteRatings.size() + " entries");
            
            // Step 3: Merge the datasets
            mergeData(moviesBasics, highVoteRatings);
            System.out.println("Final merged dataset: " + processedMovies.size() + " movies");
            
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }

    private Map<String, String[]> readAndFilterBasics(String filePath) throws IOException {
        Map<String, String[]> moviesMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine(); // Skip header
            String[] headers = headerLine.split("\t");
            int tconstIndex = -1, titleTypeIndex = -1, primaryTitleIndex = -1,
                    originalTitleIndex = -1, isAdultIndex = -1, startYearIndex = -1,
                    runtimeMinutesIndex = -1, genresIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                switch (headers[i]) {
                    case "tconst": tconstIndex = i; break;
                    case "titleType": titleTypeIndex = i; break;
                    case "primaryTitle": primaryTitleIndex = i; break;
                    case "originalTitle": originalTitleIndex = i; break;
                    case "isAdult": isAdultIndex = i; break;
                    case "startYear": startYearIndex = i; break;
                    case "runtimeMinutes": runtimeMinutesIndex = i; break;
                    case "genres": genresIndex = i; break;
                }
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length > titleTypeIndex && "movie".equals(parts[titleTypeIndex])) {
                    String tconst = parts[tconstIndex];
                    String[] movieData = new String[6];
                    movieData[0] = parts[primaryTitleIndex];
                    movieData[1] = parts[originalTitleIndex];
                    movieData[2] = parts[isAdultIndex];
                    movieData[3] = parts[startYearIndex];
                    movieData[4] = parts[runtimeMinutesIndex];
                    movieData[5] = parts[genresIndex];
                    moviesMap.put(tconst, movieData);
                }
            }
        }
        return moviesMap;
    }

    private Map<String, String[]> readAndFilterRatings(String filePath) throws IOException {
        Map<String, String[]> ratingsMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine(); // Skip header
            String[] headers = headerLine.split("\t");
            int tconstIndex = -1, averageRatingIndex = -1, numVotesIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                switch (headers[i]) {
                    case "tconst": tconstIndex = i; break;
                    case "averageRating": averageRatingIndex = i; break;
                    case "numVotes": numVotesIndex = i; break;
                }
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                try {
                    int numVotes = Integer.parseInt(parts[numVotesIndex]);
                    if (numVotes > 500000) {
                        String tconst = parts[tconstIndex];
                        String[] ratingData = new String[2];
                        ratingData[0] = parts[averageRatingIndex];
                        ratingData[1] = parts[numVotesIndex];
                        ratingsMap.put(tconst, ratingData);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return ratingsMap;
    }

    private void mergeData(Map<String, String[]> moviesBasics, Map<String, String[]> ratings) {
        for (Map.Entry<String, String[]> entry : ratings.entrySet()) {
            String tconst = entry.getKey();
            if (!moviesBasics.containsKey(tconst)) continue;

            String[] bd = moviesBasics.get(tconst);
            try {
                String primaryTitle = bd[0];
                String originalTitle = bd[1];
                boolean isAdult = "1".equals(bd[2]);
                int startYear = "\\N".equals(bd[3]) ? -1 : Integer.parseInt(bd[3]);
                int runtime = "\\N".equals(bd[4]) ? -1 : Integer.parseInt(bd[4]);
                String genres = "\\N".equals(bd[5]) ? "Unknown" : bd[5];
                double avgRating = Double.parseDouble(entry.getValue()[0]);

                // Use the new Movie constructor that takes tconst
                Movie movie = new Movie(
                        tconst,
                        primaryTitle,
                        originalTitle,
                        isAdult,
                        startYear,
                        runtime,
                        genres,
                        avgRating
                );

                if (movie.getPlotDescription() != null) {
                    processedMovies.add(movie);
                }
            } catch (Exception e) {
                System.err.println("Error processing " + tconst + ": " + e.getMessage());
            }
        }
    }
    
    public List<Movie> getProcessedMovies() {
        return processedMovies;
    }

    public void saveProcessedMoviesToCsv(String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("tconst,primaryTitle,originalTitle,isAdult,startYear,runtimeMinutes,genres,averageRating,plotDescription");
            writer.newLine();
            for (Movie m : processedMovies) {
                String singleLinePlot = m.getPlotDescription()
                        .replace("\"", "\"\"")
                        .replaceAll("\\r?\\n+", " ");
                writer.write(String.format(
                        "\"%s\",\"%s\",\"%s\",%b,%d,%d,\"%s\",%.2f,\"%s\"",
                        m.getTconst(),
                        m.getPrimaryTitle().replace("\"","\"\""),
                        m.getOriginalTitle().replace("\"","\"\""),
                        m.isAdult(),
                        m.getStartYear(),
                        m.getRuntimeMinutes(),
                        m.getGenres().replace("\"","\"\""),
                        m.getAverageRating(),
                        singleLinePlot
                ));
                writer.newLine();
            }
            System.out.println("CSV saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error saving CSV: " + e.getMessage());
        }
    }

    private String getCSVString(Movie movie) {
        String plot = movie.getPlotDescription().replace("\"", "\"\"");
        String title = movie.getPrimaryTitle().replace("\"", "\"\"");
        String originalTitle = movie.getOriginalTitle().replace("\"", "\"\"");
        String genres = movie.getGenres().replace("\"", "\"\"");

        return String.format("\"%s\",\"%s\",%b,%d,%d,\"%s\",%.2f,\"%s\"",
                title,
                originalTitle,
                movie.isAdult(),
                movie.getStartYear(),
                movie.getRuntimeMinutes(),
                genres,
                movie.getAverageRating(),
                plot
        );
    }

    public static void main(String[] args) {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.processData(
            "./data/raw/title.basics.tsv",
            "./data/raw/title.ratings.tsv"
        );

        // Save processed movies to CSV
        preprocessor.saveProcessedMoviesToCsv("./data/processed/processedMovies.csv");

        // Print first movie as a sample
        List<Movie> movies = preprocessor.getProcessedMovies();
        System.out.println("\nSample of processed movie:");
        System.out.println(movies.get(0));
    }
}
