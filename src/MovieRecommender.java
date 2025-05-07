import java.io.*;
import java.util.*;

/**
 * This class implements a movie recommendation system that combines
 * textual similarity from synopses with genre similarity to provide
 * comprehensive movie recommendations.
 */
public class MovieRecommender {

    private ArrayList<Movie> movies;
    private Corpus corpus;
    private VectorSpaceModel vectorSpace;
    private HashMap<Movie, Set<String>> genreMap;

    // Similarity map to store precomputed similarities
    private HashMap<Movie, List<Map.Entry<Movie, Double>>> similarityMap;

    // Map to look up movies by their ID for faster loading
    private HashMap<String, Movie> movieIdMap;

    // Weights for combining different similarity metrics
    private final double CONTENT_WEIGHT = 0.5;
    private final double GENRE_WEIGHT = 0.5;

    /**
     * Constructor that loads movies from CSV and builds the necessary models
     * @param csvFilePath path to the processed movies CSV file
     */
    public MovieRecommender(String csvFilePath) {
        movies = new ArrayList<>();
        genreMap = new HashMap<>();
        movieIdMap = new HashMap<>();
        similarityMap = new HashMap<>();

        try {
            loadMoviesFromCsv(csvFilePath);
            System.out.println("Loaded " + movies.size() + " movies from CSV");

            // Create corpus and vector space model
            corpus = new Corpus(movies);
            vectorSpace = new VectorSpaceModel(corpus);

            // Extract and map genres
            extractGenres();

            // Build movie ID map for faster lookups
            buildMovieIdMap();

        } catch (IOException e) {
            System.err.println("Error loading movies: " + e.getMessage());
        }
    }

    /**
     * Builds a map from movie IDs to Movie objects for faster lookups
     */
    private void buildMovieIdMap() {
        for (Movie movie : movies) {
            movieIdMap.put(movie.getTconst(), movie);
        }
    }

    /**
     * Loads movie data from the CSV file
     * @param csvFilePath path to the CSV file
     * @throws IOException if file cannot be read
     */
    private void loadMoviesFromCsv(String csvFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String header = reader.readLine(); // Skip header

            String line;
            while ((line = reader.readLine()) != null) {
                Movie movie = parseMovieCsvLine(line);
                if (movie != null) {
                    movies.add(movie);
                }
            }
        }
    }

    /**
     * Parses a CSV line into a Movie object
     * Handles CSV escaping where fields may contain commas inside quotes
     * @param line the CSV line to parse
     * @return a Movie object or null if parsing fails
     */
    private Movie parseMovieCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quote - add a single quote
                    field.append('\"');
                    i++; // Skip the next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }

        // Add the last field
        fields.add(field.toString());

        // Parse fields into a Movie object
        try {
            if (fields.size() >= 9) {
                String tconst = fields.get(0);
                String primaryTitle = fields.get(1);
                String originalTitle = fields.get(2);
                boolean isAdult = Boolean.parseBoolean(fields.get(3));
                int startYear = Integer.parseInt(fields.get(4));
                int runtimeMinutes = Integer.parseInt(fields.get(5));
                String genres = fields.get(6);
                double averageRating = Double.parseDouble(fields.get(7));
                String synopsis = fields.get(8);

                return new Movie(
                        tconst,
                        primaryTitle,
                        originalTitle,
                        isAdult,
                        startYear,
                        runtimeMinutes,
                        genres,
                        averageRating,
                        synopsis
                );
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing movie line: " + e.getMessage());
        }

        return null;
    }

    /**
     * Extracts genre information from each movie and builds a genre map
     */
    private void extractGenres() {
        System.out.println("Extracting genre information");

        for (Movie movie : movies) {
            String genreString = movie.getGenres();
            String[] genreArray = genreString.split(",");

            Set<String> genreSet = new HashSet<>();
            for (String genre : genreArray) {
                genreSet.add(genre.trim());
            }

            genreMap.put(movie, genreSet);
        }
    }

    /**
     * Calculates the genre similarity between two movies
     * Uses Jaccard similarity (intersection over union)
     * @param m1 first movie
     * @param m2 second movie
     * @return similarity score between 0.0 and 1.0
     */
    public double genreSimilarity(Movie m1, Movie m2) {
        Set<String> genres1 = genreMap.get(m1);
        Set<String> genres2 = genreMap.get(m2);

        if (genres1 == null || genres2 == null || genres1.isEmpty() || genres2.isEmpty()) {
            return 0.0;
        }

        // Create a copy for the intersection operation
        Set<String> intersection = new HashSet<>(genres1);
        intersection.retainAll(genres2);

        // Create a copy for the union operation
        Set<String> union = new HashSet<>(genres1);
        union.addAll(genres2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Combines content-based similarity (from TF-IDF vectors) with genre similarity
     * to produce a comprehensive similarity score
     * @param m1 first movie
     * @param m2 second movie
     * @return combined similarity score between 0.0 and 1.0
     */
    public double combinedSimilarity(Movie m1, Movie m2) {
        double contentSimilarity = vectorSpace.cosineSimilarity(m1, m2);
        double genreSim = genreSimilarity(m1, m2);

        return (CONTENT_WEIGHT * contentSimilarity) + (GENRE_WEIGHT * genreSim);
    }

    /**
     * Finds the top N similar movies to the given movie
     * Uses precomputed similarity map if available for better performance
     * @param movie the reference movie
     * @param n number of similar movies to return
     * @return a list of similar movies with their similarity scores
     */
    public List<Map.Entry<Movie, Double>> findSimilarMovies(Movie movie, int n) {
        // Check if we have precomputed similarities for this movie
        if (!similarityMap.isEmpty() && similarityMap.containsKey(movie)) {
            List<Map.Entry<Movie, Double>> precomputed = similarityMap.get(movie);
            return precomputed.subList(0, Math.min(n, precomputed.size()));
        }

        // If no precomputed data, calculate similarities on the fly
        Map<Movie, Double> similarityScores = new HashMap<>();

        for (Movie other : movies) {
            if (!other.equals(movie)) {
                double similarity = combinedSimilarity(movie, other);
                similarityScores.put(other, similarity);
            }
        }

        // Sort by similarity in descending order
        List<Map.Entry<Movie, Double>> sortedMovies = new ArrayList<>(similarityScores.entrySet());
        sortedMovies.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // Return the top N movies
        return sortedMovies.subList(0, Math.min(n, sortedMovies.size()));
    }

    /**
     * Builds a complete similarity map between all movies in the corpus
     * @param threshold minimum similarity score to include in the map
     * @return a map where each movie maps to its similar movies above the threshold
     */
    public HashMap<Movie, List<Map.Entry<Movie, Double>>> buildSimilarityMap(double threshold) {
        System.out.println("Building similarity map with threshold " + threshold);
        HashMap<Movie, List<Map.Entry<Movie, Double>>> similarityMap = new HashMap<>();

        int totalMovies = movies.size();
        int processed = 0;

        for (Movie movie : movies) {
            Map<Movie, Double> movieSimilarities = new HashMap<>();

            for (Movie other : movies) {
                if (!other.equals(movie)) {
                    double similarity = combinedSimilarity(movie, other);
                    if (similarity >= threshold) {
                        movieSimilarities.put(other, similarity);
                    }
                }
            }

            // Sort by similarity in descending order
            List<Map.Entry<Movie, Double>> sortedSimilarities =
                    new ArrayList<>(movieSimilarities.entrySet());
            sortedSimilarities.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

            similarityMap.put(movie, sortedSimilarities);

            // Progress reporting
            processed++;
            if (processed % 100 == 0 || processed == totalMovies) {
                System.out.printf("Processed %d/%d movies (%.1f%%)\n",
                        processed, totalMovies, (100.0 * processed / totalMovies));
            }
        }

        return similarityMap;
    }

    /**
     * Gets a movie by its title (partial match)
     * @param title the title to search for
     * @return the first movie that matches the title, or null if none found
     */
    public Movie getMovieByTitle(String title) {
        String lowercaseTitle = title.toLowerCase();

        for (Movie movie : movies) {
            if (movie.getPrimaryTitle().toLowerCase().contains(lowercaseTitle)) {
                return movie;
            }
        }

        return null;
    }

    /**
     * Gets all movies in the corpus
     * @return list of all movies
     */
    public ArrayList<Movie> getMovies() {
        return movies;
    }

    /**
     * Finds movies that are similar to a text prompt provided by the user
     * This method calculates similarity directly without adding the prompt to the corpus
     * 
     * @param prompt the text prompt to compare with movie synopses
     * @param n number of similar movies to return
     * @return a list of movies similar to the prompt with their similarity scores
     */
    public List<Map.Entry<Movie, Double>> findMoviesSimilarToPrompt(String prompt, int n) {
        // Create a temporary movie object to represent the prompt
        Movie promptMovie = new Movie(
                "prompt",
                "User Prompt",
                "User Prompt",
                false,
                0,
                0,
                "",
                0.0,
                prompt
        );
        
        Map<Movie, Double> similarityScores = new HashMap<>();
        
        // Calculate TF values for the prompt terms
        Map<String, Double> promptTermFrequencies = new HashMap<>();
        double promptMagnitude = 0.0;
        
        // Process the prompt terms
        for (String term : promptMovie.getTermList()) {
            double tf = promptMovie.getTermFrequency(term);
            double idf = corpus.getInverseDocumentFrequency(term);
            double weight = tf * idf;
            
            if (weight > 0) {
                promptTermFrequencies.put(term, weight);
                promptMagnitude += weight * weight;
            }
        }
        promptMagnitude = Math.sqrt(promptMagnitude);
        
        // Calculate similarity with each movie
        for (Movie movie : movies) {
            // Calculate dot product between prompt and movie
            double dotProduct = 0.0;
            HashMap<String, Double> movieWeights = vectorSpace.getTfIdfWeights().get(movie);
            
            for (Map.Entry<String, Double> entry : promptTermFrequencies.entrySet()) {
                String term = entry.getKey();
                Double promptWeight = entry.getValue();
                
                // Check if the movie has this term
                Double movieWeight = movieWeights.get(term);
                if (movieWeight != null) {
                    dotProduct += promptWeight * movieWeight;
                }
            }
            
            // Calculate cosine similarity
            double movieMagnitude = vectorSpace.getMagnitude(movie);
            double similarity = (promptMagnitude > 0 && movieMagnitude > 0) ? 
                                dotProduct / (promptMagnitude * movieMagnitude) : 0.0;
            
            // We only use content similarity since the prompt has no genres
            similarityScores.put(movie, similarity);
        }
        
        // Sort by similarity in descending order
        List<Map.Entry<Movie, Double>> sortedMovies = new ArrayList<>(similarityScores.entrySet());
        sortedMovies.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // Return the top N movies
        return sortedMovies.subList(0, Math.min(n, sortedMovies.size()));
    }
}