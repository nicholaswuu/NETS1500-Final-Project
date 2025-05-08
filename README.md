# Movie Recommendation System

A Java-based content-driven recommendation engine that processes movie plot descriptions to suggest relevant films based on TF-IDF vectorization and cosine similarity. It supports free-text queries or “find-similar” recommendations from an existing title, and presents results in an interactive command-line interface. The system includes text preprocessing (tokenization, stop-word removal, stemming), TF-IDF model building, and a ranking module for top-K suggestions. Recent updates improved term-weight caching and prompt handling to boost performance and usability. 

A change we made from our proposal was that we implemented Document Search instead of Graph Algorithms, because using similarity scores meant there was no need for graph algorithms.

## Project Categories

- Document Search (Information Retrieval)

Built an IR pipeline that loads each movie’s synopsis (fetched with web scraping from the IMDB site), then splits on whitespace, strips out non-alphanumeric characters, lower-cases each token, and counts raw term frequencies. Corpus consumes all Movie objects to assemble a global document-frequency map, which in turn lets us compute IDF values. Finally, VectorSpaceModel multiplies per-movie TFs by the corresponding IDFs to produce TF-IDF vectors for every film, so that both title-based and free-text queries can be cast into “term space” and answered via nearest-neighbor lookup.

- Advanced Topics (Recommendations)

We turned similarity scores into recommendations: VectorSpaceModel computes cosine similarities between a query vector (from an existing movie or custom keywords) and every cached TF-IDF vector in its tfIdfWeights map. MovieRecommender then sorts those scores and picks the top-K matches. MovieRecommendationSystem wraps it in a simple CLI that lets users choose title or keyword-based searches.


## Usage

Run the recommendation engine by running the main method in MovieRecommendationSystem.java, and then follow the prompts in the terminal. You can also run the data collection (of the synopses) by running the main method in Preprocessor.java, but it takes a very long time to run on the entire dataset so we have provided an already processed csv (processedMovies.csv).

## Work Breakdown

- Nicholas: Built Corpus and VectorSpaceModel, developed TF-IDF computation and cosine similarity scoring modules.
- Joey: Implemented text preprocessing, helped build Movie and CLI modules, authored user manual, project documentation, and examples.
- Arjun: Helped with text preprocessing, helped with designing cosine similarity scoring modules, contributed debugging fixes.