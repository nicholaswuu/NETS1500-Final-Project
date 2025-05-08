import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

// Manages a collection of Movie objects and builds an inverted index of terms.
public class Corpus {

    // List of all movies
    private ArrayList<Movie> movies;

    // Maps term -> set of movies containing that term
    private HashMap<String, Set<Movie>> invertedIndex;

    // Initialize with a list of movies and build the inverted index
    public Corpus(ArrayList<Movie> movies) {
        this.movies = movies;
        invertedIndex = new HashMap<String, Set<Movie>>();

        createInvertedIndex();
    }

    // Build the inverted index from movie term lists
    private void createInvertedIndex() {
        // Debug: starting index build

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


    // Return IDF of a term, or 0 if term not in index
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

    // Get the list of movies
    public ArrayList<Movie> getMovies() {
        return movies;
    }

    // Get the term-to-movies map
    public HashMap<String, Set<Movie>> getInvertedIndex() {
        return invertedIndex;
    }
}