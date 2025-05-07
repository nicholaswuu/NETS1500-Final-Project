import java.io.File;
import java.util.*;

/**
 * Main class to demonstrate the movie recommendation system
 */
public class MovieRecommendationSystem {

    public static void main(String[] args) {
        // Path to the processed movies CSV file
        String csvPath = "./data/processed/testProcessedMovies.csv";
        File csvFile = new File(csvPath);

        System.out.println("=== Movie Recommendation System ===");

        // Check if the processed CSV file exists, if not, run the Preprocessor
        if (!csvFile.exists()) {
            System.out.println("Processed movies CSV file not found. Running preprocessor...");
            Preprocessor preprocessor = new Preprocessor();
            preprocessor.processData(
                "./data/raw/title.basics.tsv",
                "./data/raw/title.ratings.tsv"
            );
            
            // Save processed movies to CSV
            preprocessor.saveProcessedMoviesToCsv(csvPath);
            System.out.println("CSV file created at: " + csvPath);
        }

        System.out.println("Loading movies from: " + csvPath);

        // Initialize the recommender
        MovieRecommender recommender = new MovieRecommender(csvPath);

        // Check if a similarity map already exists
        String similarityMapPath = "./data/processed/similarity_map.dat";
        File similarityFile = new File(similarityMapPath);
        double threshold = 0.0;

        if (similarityFile.exists()) {
            System.out.println("Found existing similarity map. Loading...");
            recommender.loadSimilarityMap(similarityMapPath);
        } else {
            System.out.println("Similarity map not found. Building new similarity map...");
            System.out.println("This may take a while depending on the dataset size.");
            HashMap<Movie, List<Map.Entry<Movie, Double>>> similarityMap = 
                    recommender.buildSimilarityMap(threshold);
            
            System.out.println("Saving similarity map for future use...");
            recommender.saveSimilarityMap(similarityMapPath, threshold);
            System.out.println("Similarity map saved to " + similarityMapPath);
        }

        // Get statistics about the loaded movies
        ArrayList<Movie> allMovies = recommender.getMovies();
        System.out.println("Successfully loaded " + allMovies.size() + " movies");

        // Find a specific movie
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter a movie title to find recommendations: ");
        String searchTitle = scanner.nextLine();

        Movie movie = recommender.getMovieByTitle(searchTitle);
        if (movie == null) {
            System.out.println("Movie not found. Here are some available movies:");
            // Show a sample of available movies
            for (int i = 0; i < Math.min(10, allMovies.size()); i++) {
                System.out.println("- " + allMovies.get(i).getPrimaryTitle() +
                        " (" + allMovies.get(i).getStartYear() + ")");
            }
            return;
        }

        // Display information about the selected movie
        System.out.println("\nSelected Movie:");
        System.out.println("Title: " + movie.getPrimaryTitle());
        System.out.println("Year: " + movie.getStartYear());
        System.out.println("Genres: " + movie.getGenres());
        System.out.println("Rating: " + movie.getAverageRating());

        // Find similar movies
        System.out.println("\nFinding similar movies...");
        List<Map.Entry<Movie, Double>> similarMovies = recommender.findSimilarMovies(movie, 10);

        System.out.println("\nTop 10 Movies Similar to \"" + movie.getPrimaryTitle() + "\":");
        System.out.println("----------------------------------------------------");
        for (int i = 0; i < similarMovies.size(); i++) {
            Map.Entry<Movie, Double> entry = similarMovies.get(i);
            Movie similar = entry.getKey();
            double similarity = entry.getValue();

            System.out.printf("%d. %s (%d) - %.2f similarity\n",
                    i + 1,
                    similar.getPrimaryTitle(),
                    similar.getStartYear(),
                    similarity);
            System.out.println("   Genres: " + similar.getGenres());
            System.out.println("   Rating: " + similar.getAverageRating());
        }

        System.out.println("\nThank you for using the Movie Recommendation System!");
        scanner.close();
    }
}