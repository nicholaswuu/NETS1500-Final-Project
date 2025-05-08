import java.io.*;
import java.util.*;

// Combines text and genre similarity for movie recs
public class MovieRecommender {

    private ArrayList<Movie> movies;
    private Corpus corpus;
    private VectorSpaceModel vectorSpace;
    private HashMap<Movie, Set<String>> genreMap;

    // Map to look up movies by their ID for faster loading
    private HashMap<String, Movie> movieIdMap;

    // Weights for combining different similarity metrics
    private final double CONTENT_WEIGHT = 0.8;
    private final double GENRE_WEIGHT = 0.2;

    // Load CSV, build models, and prep genres
    public MovieRecommender(String csvFilePath) {
        movies = new ArrayList<>();
        genreMap = new HashMap<>();
        movieIdMap = new HashMap<>();

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

    // Map movie IDs for quick lookup
    private void buildMovieIdMap() {
        for (Movie movie : movies) {
            movieIdMap.put(movie.getTconst(), movie);
        }
    }

    // Read movies from CSV
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

    // Turn one CSV row into a Movie
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

    // Fill genreMap from each movie
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

    // How much two movies share genres
    public double genreSimilarity(Movie m1, Movie m2) {
        Set<String> genres1 = genreMap.get(m1);
        Set<String> genres2 = genreMap.get(m2);

        if (genres1 == null || genres2 == null || genres1.isEmpty() || genres2.isEmpty()) {
            return 0.0;
        }

        int count = 0;

        for (String genre : genres1) {
            count += genres2.contains(genre) ? 1 : 0;
        }

        return (double) count / genres2.size();
    }

    // Blend TF-IDF and genres
    public double combinedSimilarity(Movie m1, Movie m2) {
        double contentSimilarity = vectorSpace.cosineSimilarity(m1, m2);
        double genreSim = genreSimilarity(m1, m2);

        return (CONTENT_WEIGHT * contentSimilarity) + (GENRE_WEIGHT * genreSim);
    }

    // Get top N similar to a given movie
    public List<Map.Entry<Movie, Double>> findSimilarMovies(Movie movie, int n) {
        // Calculate similarities
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

    // Precompute all pairs above threshold
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

    // Find a movie by exact or partial title
    public Movie getMovieByTitle(String title) {
        String lowercaseTitle = title.toLowerCase();

        // First check for exact match
        for (Movie movie : movies) {
            if (movie.getPrimaryTitle().toLowerCase().equals(lowercaseTitle)) {
                return movie;
            }
        }

        // Backup check for partial match
        for (Movie movie : movies) {
            if (movie.getPrimaryTitle().toLowerCase().contains(lowercaseTitle)) {
                return movie;
            }
        }

        // No movie found
        return null;
    }

    // All loaded movies
    public ArrayList<Movie> getMovies() {
        return movies;
    }

    // Treat prompt as a movie and find matches
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

        // Prepare prompt text for genre matching (convert to lowercase for case-insensitive matching)
        String promptLower = prompt.toLowerCase();

        // Get all unique genres from our dataset for matching
        Set<String> allGenres = new HashSet<>();
        for (Movie movie : movies) {
            String[] genreArray = movie.getGenres().split(",");
            for (String genre : genreArray) {
                allGenres.add(genre.trim().toLowerCase());
            }
        }

        // Define genre boost factor
        final double GENRE_BOOST_PER_MATCH = 0.05;

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

            // Add genre matching boost
            double genreBoost = 0.00;
            String[] movieGenres = movie.getGenres().split(",");
            int matchedGenres = 0;

            for (String genre : movieGenres) {
                String genreLower = genre.trim().toLowerCase();
                if (promptLower.contains(genreLower)) {
                    matchedGenres++;
                }
            }

            // Calculate genre boost (capped at MAX_GENRE_BOOST)
            genreBoost = matchedGenres * GENRE_BOOST_PER_MATCH;

            // Apply genre boost to similarity score
            similarity += genreBoost;

            // Ensure similarity doesn't exceed 1.0
            similarity = Math.min(similarity, 1.0);

            // Store the combined similarity score
            similarityScores.put(movie, similarity);
        }

        // Sort by similarity in descending order
        List<Map.Entry<Movie, Double>> sortedMovies = new ArrayList<>(similarityScores.entrySet());
        sortedMovies.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // Return the top N movies
        return sortedMovies.subList(0, Math.min(n, sortedMovies.size()));
    }

    // Avg similarity across a list of movies
    public List<Map.Entry<Movie, Double>> findSimilarMoviesFromList(List<Movie> inputMovies, int n) {
        // If the input list is empty, return an empty list
        if (inputMovies == null || inputMovies.isEmpty()) {
            return new ArrayList<>();
        }
        
        // If only one movie in the list, use the standard method
        if (inputMovies.size() == 1) {
            return findSimilarMovies(inputMovies.get(0), n);
        }
        
        Map<Movie, Double> aggregateSimilarityScores = new HashMap<>();
        Set<Movie> inputMovieSet = new HashSet<>(inputMovies);
        
        // For each movie in our corpus
        for (Movie candidateMovie : movies) {
            // Skip if this movie is in the input list
            if (inputMovieSet.contains(candidateMovie)) {
                continue;
            }
            
            double totalSimilarity = 0.0;
            
            // Calculate similarity between this candidate and each input movie
            for (Movie inputMovie : inputMovies) {
                double similarity = combinedSimilarity(inputMovie, candidateMovie);
                totalSimilarity += similarity;
            }
            
            // Use average similarity across all input movies
            double avgSimilarity = totalSimilarity / inputMovies.size();
            aggregateSimilarityScores.put(candidateMovie, avgSimilarity);
        }
        
        // Sort by similarity in descending order
        List<Map.Entry<Movie, Double>> sortedMovies = new ArrayList<>(aggregateSimilarityScores.entrySet());
        sortedMovies.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // Return the top N movies
        return sortedMovies.subList(0, Math.min(n, sortedMovies.size()));
    }
}