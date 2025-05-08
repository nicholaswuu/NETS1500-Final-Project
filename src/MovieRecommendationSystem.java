import java.io.File;
import java.util.*;

// Main CLI for movie recommendations
public class MovieRecommendationSystem {

    private static final String KILL_KEY = "q";

    public static void main(String[] args) {
        // CSV file path
        String csvPath = "./data/processed/processedMovies.csv";
        File csvFile = new File(csvPath);

        System.out.println("=== Movie Recommendation System ===");

        // If CSV missing, preprocess data
        if (!csvFile.exists()) {
            System.out.println("Processed movies CSV file not found. Running preprocessor...");
            Preprocessor preprocessor = new Preprocessor();
            preprocessor.processData(
                "./data/raw/title.basics.tsv",
                "./data/raw/title.ratings.tsv"
            );

            // Write processed data
            preprocessor.saveProcessedMoviesToCsv(csvPath);
            System.out.println("CSV file created at: " + csvPath);
        }

        System.out.println("Loading movies from: " + csvPath);

        // Initialize the recommender
        MovieRecommender recommender = new MovieRecommender(csvPath);

        // Show movie count
        ArrayList<Movie> allMovies = recommender.getMovies();
        System.out.println("Successfully loaded " + allMovies.size() + " movies");

        // User input loop
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== Movie Recommendation Options ===");
            System.out.println("1. Search for recommendations based on a single movie");
            System.out.println("2. Search for recommendations based on multiple movies");
            System.out.println("3. Search for movies similar to a text prompt");
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
                    searchMoviesByMultipleMovies(scanner, recommender, allMovies);
                    break;
                case "3":
                    searchMoviesByPrompt(scanner, recommender);
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }

        scanner.close();
    }

    // Option 1: recommend based on one movie
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

    // Option 3: recommend based on text prompt
    private static void searchMoviesByPrompt(Scanner scanner, MovieRecommender recommender) {
        System.out.println("\nEnter a text prompt describing the kind of movie you're looking for:");
        System.out.println("(Example: \"A sci-fi adventure with robots and space travel\")");
        String prompt = scanner.nextLine();

        System.out.println("\nFinding movies similar to your prompt...");
        List<Map.Entry<Movie, Double>> similarMovies = recommender.findMoviesSimilarToPrompt(prompt, 10);

        // Display similar movies
        displaySimilarMovies(similarMovies, "your prompt \"" + prompt + "\"");
    }

    // Option 2: recommend based on multiple movies
    private static void searchMoviesByMultipleMovies(Scanner scanner, MovieRecommender recommender, ArrayList<Movie> allMovies) {
        List<Movie> inputMovies = new ArrayList<>();
        boolean addingMovies = true;

        System.out.println("\nEnter movie titles one by one. Type 'done' when finished or '" + KILL_KEY + "' to cancel.");

        while (addingMovies) {
            if (inputMovies.isEmpty()) {
                System.out.print("Enter movie #1: ");
            } else {
                System.out.print("Enter movie #" + (inputMovies.size() + 1) + " (or 'done' to finish): ");
            }

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("done")) {
                if (inputMovies.isEmpty()) {
                    System.out.println("You must enter at least one movie. Please try again.");
                    continue;
                }
                addingMovies = false;
            } else if (input.equalsIgnoreCase(KILL_KEY)) {
                System.out.println("Operation canceled.");
                return;
            } else {
                Movie movie = recommender.getMovieByTitle(input);
                if (movie == null) {
                    System.out.println("Movie not found. Here are some available movies:");
                    // Show a sample of available movies
                    for (int i = 0; i < Math.min(5, allMovies.size()); i++) {
                        System.out.println("- " + allMovies.get(i).getPrimaryTitle() +
                                " (" + allMovies.get(i).getStartYear() + ")");
                    }
                    System.out.println("Please try another title.");
                } else {
                    // Check if movie is already in the list
                    boolean alreadyAdded = false;
                    for (Movie addedMovie : inputMovies) {
                        if (addedMovie.getTconst().equals(movie.getTconst())) {
                            System.out.println("This movie is already in your list. Please enter a different movie.");
                            alreadyAdded = true;
                            break;
                        }
                    }

                    if (!alreadyAdded) {
                        inputMovies.add(movie);
                        System.out.println("Added: " + movie.getPrimaryTitle() + " (" + movie.getStartYear() + ")");
                    }
                }
            }
        }

        // Display information about selected movies
        System.out.println("\nSelected Movies:");
        for (int i = 0; i < inputMovies.size(); i++) {
            Movie movie = inputMovies.get(i);
            System.out.println((i + 1) + ". " + movie.getPrimaryTitle() +
                " (" + movie.getStartYear() + ") - " + movie.getGenres());
        }

        // Find similar movies based on the input list
        System.out.println("\nFinding recommendations based on your selected movies...");
        List<Map.Entry<Movie, Double>> similarMovies = recommender.findSimilarMoviesFromList(inputMovies, 10);

        // Display similar movies
        displaySimilarMovies(similarMovies, "your selected movies");
    }

    // Print selected movie details
    private static void displayMovieInfo(Movie movie) {
        System.out.println("\nSelected Movie:");
        System.out.println("Title: " + movie.getPrimaryTitle());
        System.out.println("Year: " + movie.getStartYear());
        System.out.println("Genres: " + movie.getGenres());
        System.out.println("Rating: " + movie.getAverageRating());
    }

    // Print recommendation list
    private static void displaySimilarMovies(List<Map.Entry<Movie, Double>> similarMovies, String comparisonSource) {
        System.out.println("\nTop 10 Movies Similar to " + comparisonSource + ":");
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