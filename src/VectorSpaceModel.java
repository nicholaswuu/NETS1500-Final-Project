import java.util.HashMap;
import java.util.Set;

/**
 * This class implements the Vector-Space model.
 * It takes a corpus and creates the tf-idf vectors for each movie.
 *
 */
public class VectorSpaceModel {

    /**
     * The corpus of movies.
     */
    private Corpus corpus;

    /**
     * The tf-idf weight vectors.
     * The hashmap maps a document to another hashmap.
     * The second hashmap maps a term to its tf-idf weight for this movie.
     */
    private HashMap<Movie, HashMap<String, Double>> tfIdfWeights;

    /**
     * The constructor.
     * It will take a corpus of movies.
     * Using the corpus, it will generate tf-idf vectors for each movie.
     * @param corpus the corpus of movies
     */
    public VectorSpaceModel(Corpus corpus) {
        this.corpus = corpus;
        tfIdfWeights = new HashMap<Movie, HashMap<String, Double>>();

        createTfIdfWeights();
    }

    /**
     * This creates the tf-idf vectors.
     */
    private void createTfIdfWeights() {
        System.out.println("Creating the tf-idf weight vectors");
        Set<String> terms = corpus.getInvertedIndex().keySet();

        for (Movie movie : corpus.getMovies()) {
            HashMap<String, Double> weights = new HashMap<String, Double>();

            for (String term : terms) {
                double tf = movie.getTermFrequency(term);
                double idf = corpus.getInverseDocumentFrequency(term);

                double weight = tf * idf;

                weights.put(term, weight);
            }
            tfIdfWeights.put(movie, weights);
        }
    }

    /**
     * This method will return the magnitude of a vector.
     * @param movie the movie whose magnitude is calculated.
     * @return the magnitude
     */
    private double getMagnitude(Movie movie) {
        double magnitude = 0;
        HashMap<String, Double> weights = tfIdfWeights.get(movie);

        for (double weight : weights.values()) {
            magnitude += weight * weight;
        }

        return Math.sqrt(magnitude);
    }

    /**
     * This will take two movies and return the dot product.
     * @param m1 Movie 1
     * @param m2 Movie 2
     * @return the dot product of the movies
     */
    private double getDotProduct(Movie m1, Movie m2) {
        double product = 0;
        HashMap<String, Double> weights1 = tfIdfWeights.get(m1);
        HashMap<String, Double> weights2 = tfIdfWeights.get(m2);

        for (String term : weights1.keySet()) {
            product += weights1.get(term) * weights2.get(term);
        }

        return product;
    }

    /**
     * This will return the cosine similarity of two movies.
     * This will range from 0 (not similar) to 1 (very similar).
     * @param m1 Movie 1
     * @param m2 Movie 2
     * @return the cosine similarity
     */
    public double cosineSimilarity(Movie m1, Movie m2) {
        return getDotProduct(m1, m2) / (getMagnitude(m1) * getMagnitude(m1));
    }
}