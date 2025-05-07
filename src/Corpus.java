import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class represents a corpus of movies.
 * It will create an inverted index for these movies.
 *
 */
public class Corpus {

    /**
     * An arraylist of all movies in the corpus.
     */
    private ArrayList<Movie> movies;

    /**
     * The inverted index.
     * It will map a term to a set of movies that contain that term.
     */
    private HashMap<String, Set<Movie>> invertedIndex;

    /**
     * The constructor - it takes in an arraylist of movies.
     * It will generate the inverted index based on the movie synopses.
     * @param movies the list of movies
     */
    public Corpus(ArrayList<Movie> movies) {
        this.movies = movies;
        invertedIndex = new HashMap<String, Set<Movie>>();

        createInvertedIndex();
    }

    /**
     * This method will create an inverted index.
     */
    private void createInvertedIndex() {
        System.out.println("Creating the inverted index");

        for (Movie movie : movies) {
            Set<String> terms = movie.getTermList();

            for (String term : terms) {
                if (invertedIndex.containsKey(term)) {
                    Set<Movie> list = invertedIndex.get(term);
                    list.add(movie);
                } else {
                    Set<Movie> list = new TreeSet<Movie>();
                    list.add(movie);
                    invertedIndex.put(term, list);
                }
            }
        }
    }

    /**
     * This method returns the idf for a given term.
     * @param term a term in a document
     * @return the idf for the term
     */
    public double getInverseDocumentFrequency(String term) {
        if (invertedIndex.containsKey(term)) {
            double size = movies.size();
            Set<Movie> list = invertedIndex.get(term);
            double documentFrequency = list.size();

            return Math.log10(size / documentFrequency);
        } else {
            return 0;
        }
    }

    /**
     * @return the movies
     */
    public ArrayList<Movie> getMovies() {
        return movies;
    }

    /**
     * @return the invertedIndex
     */
    public HashMap<String, Set<Movie>> getInvertedIndex() {
        return invertedIndex;
    }
}