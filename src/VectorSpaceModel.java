import java.util.HashMap;
import java.util.Map;
import java.util.Set;


// Builds TF-IDF vectors and supports similarity queries
public class VectorSpaceModel {

    // source of all movies and term stats
    private Corpus corpus;

    // TF-IDF weights per movie
    private HashMap<Movie, HashMap<String, Double>> tfIdfWeights;

    // precomputed vector lengths
    private Map<Movie, Double> magnitudes;

    // initialize model and compute vectors
    public VectorSpaceModel(Corpus corpus) {
        this.corpus = corpus;
        tfIdfWeights = new HashMap<Movie, HashMap<String, Double>>();
        magnitudes = new HashMap<>();

        createTfIdfWeights();
    }

    // compute TF-IDF for each movie in batches
    private void createTfIdfWeights() {
        // start building TF-IDF weights
        int batchSize = 100; 
        int totalMovies = corpus.getMovies().size();
        int processedCount = 0;

        // Get all terms just once
        Set<String> terms = corpus.getInvertedIndex().keySet();

        // loop over all movies
        for (Movie movie : corpus.getMovies()) {
            // collect non-zero term weights
            HashMap<String, Double> weights = new HashMap<String, Double>();
            double magnitude = 0.0;

            for (String term : terms) {
                double tf = movie.getTermFrequency(term);
                // Skip terms not in this movie
                if (tf > 0) {
                    double idf = corpus.getInverseDocumentFrequency(term);
                    double weight = tf * idf;

                    if (weight > 0) {
                        weights.put(term, weight);
                        magnitude += weight * weight;
                    }
                }
            }

            // Store the weights and pre-calculated magnitude
            tfIdfWeights.put(movie, weights);
            magnitudes.put(movie, Math.sqrt(magnitude));

            processedCount++;
            if (processedCount % (batchSize * 10) == 0 || processedCount == totalMovies) {
                System.out.printf("Processed %d/%d movies (%.1f%%)%n",
                                   processedCount, totalMovies,
                                   (100.0 * processedCount / totalMovies));

                // Force garbage collection to free up memory
                System.gc();
            }
        }

        // done building TF-IDF vectors
    }

    // get stored vector length
    public double getMagnitude(Movie movie) {
        return magnitudes.get(movie);
    }

    // dot product of two TF-IDF vectors
    private double getDotProduct(Movie m1, Movie m2) {
        double product = 0;
        HashMap<String, Double> weights1 = tfIdfWeights.get(m1);
        HashMap<String, Double> weights2 = tfIdfWeights.get(m2);

        // Choose the smaller map to iterate through for efficiency
        HashMap<String, Double> smaller = weights1.size() < weights2.size() ? weights1 : weights2;
        HashMap<String, Double> larger = smaller == weights1 ? weights2 : weights1;

        // Only iterate through terms in the smaller map
        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            String term = entry.getKey();
            Double weight2 = larger.get(term);

            // Only add to product if term exists in both documents
            if (weight2 != null) {
                product += entry.getValue() * weight2;
            }
        }

        return product;
    }

    // cosine similarity between two movies
    public double cosineSimilarity(Movie m1, Movie m2) {
        return getDotProduct(m1, m2) / (getMagnitude(m1) * getMagnitude(m2));
    }

    // raw TF-IDF map
    public HashMap<Movie, HashMap<String, Double>> getTfIdfWeights() {
        return tfIdfWeights;
    }
}