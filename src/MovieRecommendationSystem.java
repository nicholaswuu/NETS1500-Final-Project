import java.io.File;
import java.util.*;

/**
 * Main class to demonstrate the movie recommendation system
 */
public class MovieRecommendationSystem {

    private static final String KILL_KEY = "q";

    public static void main(String[] args) {
        // Path to the processed movies CSV file
        String csvPath = "./data/processed/processedMovies.csv";
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
            System.out.println("Similarity map not found. Building and saving new similarity map...");
            recommender.saveSimilarityMap(similarityMapPath, threshold);
            System.out.println("Similarity map saved to " + similarityMapPath);
        }

        // Get statistics about the loaded movies
        ArrayList<Movie> allMovies = recommender.getMovies();
        System.out.println("Successfully loaded " + allMovies.size() + " movies");
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.println("\n=== Movie Recommendation Options ===");
            System.out.println("1. Search for a movie by title");
            System.out.println("2. Search for movies similar to a text prompt");
            System.out.println("Enter '" + KILL_KEY + "' to quit");
            System.out.print("\nChoose an option: ");
            
            String option = scanner.nextLine().trim();
            
            if (option.equalsIgnoreCase(KILL_KEY)) {
                running = false;
                System.out.println("Thank you for using the Movie Recommendation System!");
                continue;
            }
            
            switch (option) {
                case "1":
                    searchMovieByTitle(scanner, recommender, allMovies);
                    break;
                case "2":
                    searchMoviesByPrompt(scanner, recommender);
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
        
        scanner.close();
    }
    
    /**
     * Handles the search for a movie by title
     * 
     * @param scanner Scanner for user input
     * @param recommender The movie recommender instance
     * @param allMovies List of all available movies
     */
    private static void searchMovieByTitle(Scanner scanner, MovieRecommender recommender, ArrayList<Movie> allMovies) {
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
        displayMovieInfo(movie);

        // Find similar movies
        System.out.println("\nFinding similar movies...");
        List<Map.Entry<Movie, Double>> similarMovies = recommender.findSimilarMovies(movie, 10);

        // Display similar movies
        displaySimilarMovies(similarMovies, movie.getPrimaryTitle());
    }
    
    /**
     * Handles the search for movies similar to a text prompt
     * 
     * @param scanner Scanner for user input
     * @param recommender The movie recommender instance
     */
    private static void searchMoviesByPrompt(Scanner scanner, MovieRecommender recommender) {
        System.out.println("\nEnter a text prompt describing the kind of movie you're looking for:");
        System.out.println("(Example: \"A sci-fi adventure with robots and space travel\")");
        String prompt = scanner.nextLine();
        
        System.out.println("\nFinding movies similar to your prompt...");
        List<Map.Entry<Movie, Double>> similarMovies = recommender.findMoviesSimilarToPrompt(prompt, 10);
        
        // Display similar movies
        displaySimilarMovies(similarMovies, "your prompt \"" + prompt + "\"");
    }
    
    /**
     * Displays information about a movie
     * 
     * @param movie The movie to display
     */
    private static void displayMovieInfo(Movie movie) {
        System.out.println("\nSelected Movie:");
        System.out.println("Title: " + movie.getPrimaryTitle());
        System.out.println("Year: " + movie.getStartYear());
        System.out.println("Genres: " + movie.getGenres());
        System.out.println("Rating: " + movie.getAverageRating());
    }
    
    /**
     * Displays a list of similar movies
     * 
     * @param similarMovies List of similar movies with similarity scores
     * @param comparisonSource The source being compared to (movie title or prompt)
     */
    private static void displaySimilarMovies(List<Map.Entry<Movie, Double>> similarMovies, String comparisonSource) {
        System.out.println("\nTop 10 Movies Similar to \"" + comparisonSource + "\":");
        System.out.println("----------------------------------------------------");
        
        if (similarMovies.isEmpty()) {
            System.out.println("No similar movies found.");
            return;
        }
        
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
    }
}