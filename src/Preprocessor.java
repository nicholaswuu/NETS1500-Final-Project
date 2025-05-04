import java.io.BufferedReader;
import java.io.FileReader;
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
            
            // Step 2: Read and filter ratings data (only those with > 10000 votes)
            Map<String, String[]> highVoteRatings = readAndFilterRatings(ratingsFilePath);
            System.out.println("Filtered ratings data: " + highVoteRatings.size() + " entries");
            
            // Step 3: Merge the datasets
            mergeData(moviesBasics, highVoteRatings);
            System.out.println("Final merged dataset: " + processedMovies.size() + " movies");
            
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Map<String, String[]> readAndFilterBasics(String filePath) throws IOException {
        Map<String, String[]> moviesMap = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine(); // Skip header
            String[] headers = headerLine.split("\t");
            
            // Find column indices
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
                if (parts.length > titleTypeIndex && parts[titleTypeIndex].equals("movie")) {
                    String tconst = parts[tconstIndex];
                    String[] movieData = new String[7];
                    movieData[0] = parts[primaryTitleIndex];             // primaryTitle
                    movieData[1] = parts[originalTitleIndex];            // originalTitle
                    movieData[2] = parts[isAdultIndex];                  // isAdult
                    movieData[3] = parts[startYearIndex];                // startYear
                    movieData[4] = parts[runtimeMinutesIndex];           // runtimeMinutes
                    movieData[5] = parts[genresIndex];                   // genres
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
            
            // Find column indices
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
                    if (numVotes > 10000) {
                        String tconst = parts[tconstIndex];
                        String[] ratingData = new String[2];
                        ratingData[0] = parts[averageRatingIndex];    // averageRating
                        ratingData[1] = parts[numVotesIndex];         // numVotes
                        ratingsMap.put(tconst, ratingData);
                    }
                } catch (NumberFormatException e) {
                    // Skip this line if numVotes is not a valid integer
                }
            }
        }
        return ratingsMap;
    }
    
    private void mergeData(Map<String, String[]> moviesBasics, Map<String, String[]> ratings) {
        // Iterate through each entry in the ratings map
        for (Map.Entry<String, String[]> entry : ratings.entrySet()) {
            String tconst = entry.getKey();
            String[] ratingData = entry.getValue();
            
            // Check if this tconst exists in moviesBasics
            if (moviesBasics.containsKey(tconst)) {
                String[] movieData = moviesBasics.get(tconst);
                
                try {
                    // Parse all the data
                    String primaryTitle = movieData[0];
                    String originalTitle = movieData[1];
                    boolean isAdult = "1".equals(movieData[2]);
                    
                    int startYear;
                    try {
                        startYear = movieData[3].equals("\\N") ? -1 : Integer.parseInt(movieData[3]);
                    } catch (NumberFormatException e) {
                        startYear = -1;
                    }
                    
                    int runtimeMinutes;
                    try {
                        runtimeMinutes = movieData[4].equals("\\N") ? -1 : Integer.parseInt(movieData[4]);
                    } catch (NumberFormatException e) {
                        runtimeMinutes = -1;
                    }
                    
                    String genres = movieData[5].equals("\\N") ? "Unknown" : movieData[5];
                    double averageRating = Double.parseDouble(ratingData[0]);
                    
                    // Create a Movie object and add it to the list
                    Movie movie = new Movie(
                        primaryTitle,
                        originalTitle,
                        isAdult,
                        startYear,
                        runtimeMinutes,
                        genres,
                        averageRating
                    );

                    processedMovies.add(movie);
                } catch (Exception e) {
                    // Skip this movie if there's an issue with the data
                    System.err.println("Error processing movie: " + tconst + " - " + e.getMessage());
                }
            }
        }
    }
    
    public List<Movie> getProcessedMovies() {
        return processedMovies;
    }
    
    public static void main(String[] args) {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.processData(
            "./data/title.basics.tsv", 
            "./data/title.ratings.tsv"
        );
        
        // Print first 5 movies as a sample
        List<Movie> movies = preprocessor.getProcessedMovies();
        System.out.println("\nSample of processed movie:");
        System.out.println(movies.get(0));

        System.out.println(movies.get(0).extractWikipediaPlot());
    }
}
