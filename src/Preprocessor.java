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
            // load and filter basic movie info
            Map<String, String[]> moviesBasics = readAndFilterBasics(basicsFilePath);
            System.out.println("Filtered basics data: " + moviesBasics.size() + " movies");

            // load ratings with >20k votes
            Map<String, String[]> highVoteRatings = readAndFilterRatings(ratingsFilePath);
            System.out.println("Filtered ratings data: " + highVoteRatings.size() + " entries");

            // merge movie info and ratings
            mergeData(moviesBasics, highVoteRatings);
            System.out.println("Final merged dataset: " + processedMovies.size() + " movies");

        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }

    private Map<String, String[]> readAndFilterBasics(String filePath) throws IOException {
        Map<String, String[]> moviesMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // skip header row
            String headerLine = reader.readLine();
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
            // skip header row
            String headerLine = reader.readLine();
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
                    if (numVotes > 20000) {
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

    // combine movie metadata and ratings, filtering out those without synopsis
    private void mergeData(Map<String, String[]> moviesBasics, Map<String, String[]> ratings) {
        int totalRatings = ratings.size();
        int processedCount = 0;

        for (Map.Entry<String, String[]> entry : ratings.entrySet()) {
            String tconst = entry.getKey();
            if (!moviesBasics.containsKey(tconst)) continue;

            String[] movieData = moviesBasics.get(tconst);
            try {
                // unpack movie basics and ratings
                String primaryTitle = movieData[0];
                String originalTitle = movieData[1];
                boolean isAdult = "1".equals(movieData[2]);
                int startYear = "\\N".equals(movieData[3]) ? -1 : Integer.parseInt(movieData[3]);
                int runtime = "\\N".equals(movieData[4]) ? -1 : Integer.parseInt(movieData[4]);
                String genres = "\\N".equals(movieData[5]) ? "Unknown" : movieData[5];
                double avgRating = Double.parseDouble(entry.getValue()[0]);

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

                if (movie.getSynopsisString() != null) {
                    // only keep movies with a plot
                    processedMovies.add(movie);
                }

                processedCount++;
                if (processedCount % 10 == 0) {
                    double percentProcessed = (processedCount / (double) totalRatings) * 100;
                    System.out.printf("Processed %d/%d movies (%.2f%%)%n", processedCount, totalRatings, percentProcessed);
                }
            } catch (Exception e) {
                System.err.println("Error processing " + tconst + ": " + e.getMessage());
            }
        }
    }

    // return processed movies list
    public List<Movie> getProcessedMovies() {
        return processedMovies;
    }

    // write processed movies to a CSV file
    public void saveProcessedMoviesToCsv(String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            // write CSV header
            writer.write("tconst,primaryTitle,originalTitle,isAdult,startYear,runtimeMinutes,genres,averageRating,synopsis");
            writer.newLine();
            for (Movie m : processedMovies) {
                String CSVString = getCSVString(m);
                writer.write(CSVString);
                writer.newLine();
            }
            System.out.println("CSV saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error saving CSV: " + e.getMessage());
        }
    }

    // build one CSV line for a movie
    private String getCSVString(Movie m) {
        String tconst = m.getTconst();
        String title = m.getPrimaryTitle().replace("\"", "\"\"");
        String originalTitle = m.getOriginalTitle().replace("\"", "\"\"");
        String genres = m.getGenres().replace("\"", "\"\"");

        String singleLineSynopsis = m.getSynopsisString()
                .replace("\"", "\"\"")
                .replaceAll("\\r?\\n+", " ");

        return String.format("\"%s\",\"%s\",\"%s\",%b,%d,%d,\"%s\",%.2f,\"%s\"",
                tconst,
                title,
                originalTitle,
                m.isAdult(),
                m.getStartYear(),
                m.getRuntimeMinutes(),
                genres,
                m.getAverageRating(),
                singleLineSynopsis
        );
    }

    // quick test driver for preprocessing steps
    public static void main(String[] args) {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.processData(
            "./data/raw/title.basics.tsv",
            "./data/raw/title.ratings.tsv"
        );

        // Save processed movies to CSV
        preprocessor.saveProcessedMoviesToCsv("./data/processed/testProcessedMovies.csv");

        // Print first movie as a sample
        List<Movie> movies = preprocessor.getProcessedMovies();
        System.out.println("\nSample of processed movie:");
        System.out.println(movies.get(0));
    }
}
