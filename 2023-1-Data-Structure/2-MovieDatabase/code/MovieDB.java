import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Genre, Title 을 관리하는 영화 데이터베이스.
 * <p>
 * MyLinkedList 를 사용해 각각 Genre와 Title에 따라 내부적으로 정렬된 상태를
 * 유지하는 데이터베이스이다.
 */
public class MovieDB {
    private MyLinkedList<MovieList> database;

    public MovieDB() {
        // FIXME implement this

        database = new MyLinkedList<>();
        // HINT: MovieDBGenre 클래스를 정렬된 상태로 유지하기 위한
        // MyLinkedList 타입의 멤버 변수를 초기화 한다.
    }

    private void insertMovieList(String genre) {
        Node<MovieList> movieListNode = this.database.head;
        while (movieListNode.getNext() != null && movieListNode.getNext().getItem().getGenre().compareTo(genre) < 0) {
            movieListNode = movieListNode.getNext();
        }
        movieListNode.insertNext(new MovieList(genre));
    }

    private MovieList getMovieList(String genre) {
        Node<MovieList> movieListNode = this.database.head;
        while (movieListNode.getNext() != null && movieListNode.getNext().getItem().getGenre().compareTo(genre) <= 0) {
            if (movieListNode.getNext().getItem().getGenre().compareTo(genre) == 0) {
                return movieListNode.getNext().getItem();
            }
            movieListNode = movieListNode.getNext();
        }
        return null;
    }

    public void insert(MovieDBItem item) {
        // FIXME implement this
        // Insert the given item to the MovieDB.

        MovieList movieList = this.getMovieList(item.getGenre());

        if (movieList == null) {
            this.insertMovieList(item.getGenre());
            movieList = this.getMovieList(item.getGenre());
        }

        Node<String> movie = movieList.head;

        while (movie.getNext() != null &&
                movie.getNext().getItem().compareTo(item.getTitle()) <= 0) {
            if (movie.getNext().getItem().equals(item.getTitle()))
                return;
            movie = movie.getNext();
        }
        movie.insertNext(item.getTitle());

        // Printing functionality is provided for the sake of debugging.
        // This code should be removed before submitting your work.
//        System.err.printf("[trace] MovieDB: INSERT [%s] [%s]\n", item.getGenre(), item.getTitle());
    }

    public void delete(MovieDBItem item) {
        // FIXME implement this
        // Remove the given item from the MovieDB.

        MovieList movieList = this.getMovieList(item.getGenre());
        movieList.removeItem(item.getTitle());
        if (movieList.isEmpty())
        {
            this.database.removeItem(movieList);
        }
        // Printing functionality is provided for the sake of debugging.
        // This code should be removed before submitting your work.
//        System.err.printf("[trace] MovieDB: DELETE [%s] [%s]\n", item.getGenre(), item.getTitle());
    }

    public MyLinkedList<MovieDBItem> search(String term) {
        // FIXME implement this
        // Search the given term from the MovieDB.
        // You should return a linked list of MovieDBItem.
        // The search command is handled at SearchCmd class.

        // Printing search results is the responsibility of SearchCmd class.
        // So you must not use System.out in this method to achieve specs of the assignment.

        // This tracing functionality is provided for the sake of debugging.
        // This code should be removed before submitting your work.
//        System.err.printf("[trace] MovieDB: SEARCH [%s]\n", term);

        // FIXME remove this code and return an appropriate MyLinkedList<MovieDBItem> instance.
        // This code is supplied for avoiding compilation error.
        MyLinkedList<MovieDBItem> results = new MyLinkedList<MovieDBItem>();

        for (MovieList movieList : this.database) {
            for (String movieTitle : movieList) {
                if (movieTitle.contains(term))
                    results.add(new MovieDBItem(movieList.head.getItem(), movieTitle));
            }
        }

        return results;
    }

    public MyLinkedList<MovieDBItem> items() {
        // FIXME implement this
        // Search the given term from the MovieDatabase.
        // You should return a linked list of QueryResult.
        // The print command is handled at PrintCmd class.

        // Printing movie items is the responsibility of PrintCmd class.
        // So you must not use System.out in this method to achieve specs of the assignment.

        // Printing functionality is provided for the sake of debugging.
        // This code should be removed before submitting your work.
//        System.err.printf("[trace] MovieDB: ITEMS\n");

        // FIXME remove this code and return an appropriate MyLinkedList<MovieDBItem> instance.
        // This code is supplied for avoiding compilation error.
        MyLinkedList<MovieDBItem> results = new MyLinkedList<MovieDBItem>();

        for (MovieList movieList : this.database) {
            for (String movieTitle : movieList) {
                results.add(new MovieDBItem(movieList.head.getItem(), movieTitle));
            }
        }

        return results;
    }
}

class Genre extends Node<String> implements Comparable<Genre> {
    public Genre(String name) {
        super(name);
    }

    @Override
    public int compareTo(Genre o) {
        return (this.getItem().compareTo(o.getItem()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getItem() == null) ? 0 : this.getItem().hashCode());
        result = prime * result + ((this.getNext() == null) ? 0 : this.getNext().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Genre other = (Genre) obj;
        if (this.getItem() == null) {
            if (other.getItem() != null)
                return false;
        } else if (!this.getItem().equals(other.getItem()))
            return false;
        if (this.getNext() == null) {
            if (other.getNext() != null)
                return false;
        } else if (!this.getNext().equals(other.getNext()))
            return false;
        return true;
    }
}

class MovieList implements ListInterface<String> {
    Node<String> head;
    int numItems;

    public MovieList(String genre) {
        this.head = new Genre(genre);
    }

    public String getGenre() {
        return this.head.getItem();
    }

    @Override
    public Iterator<String> iterator() {
        return new MovieListIterator(this);
    }

    @Override
    public boolean isEmpty() {
        return head.getNext() == null;
    }

    @Override
    public int size() {
        return numItems;
    }

    @Override
    public void add(String item) {
        Node<String> prev = head;
        while (prev.getNext().getItem().compareTo(item) < 0) {
            prev = prev.getNext();
        }
        prev.insertNext(item);
        numItems += 1;
    }

    public void removeItem(String item)
    {
        Node<String> curr = head;
        while (!curr.getNext().getItem().equals(item)) {
            curr = curr.getNext();
        }
        curr.removeNext();
        numItems -= 1;
    }

    @Override
    public String first() {
        return head.getNext().getItem();
    }

    @Override
    public void removeAll() {
        head.setNext(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MovieList other = (MovieList) obj;
        if (this.getGenre() == null) {
            if (other.getGenre() != null)
                return false;
        } else if (!this.getGenre().equals(other.getGenre()))
            return false;
        return true;
    }
}

class MovieListIterator implements Iterator<String> {
    // FIXME implement this
    // Implement the iterator for MovieList.
    // You have to maintain the current position of the iterator.
    private MovieList list;
    private Node<String> curr;
    private Node<String> prev;

    public MovieListIterator(MovieList list) {
        this.list = list;
        this.curr = list.head;
        this.prev = null;
    }

    @Override
    public boolean hasNext() {
        return curr.getNext() != null;
    }

    @Override
    public String next() {
        if (!hasNext())
            throw new NoSuchElementException();

        prev = curr;
        curr = curr.getNext();

        return curr.getItem();
    }

    @Override
    public void remove() {
        if (prev == null)
            throw new IllegalStateException("next() should be called first");
        if (curr == null)
            throw new NoSuchElementException();
        prev.removeNext();
        list.numItems -= 1;
        curr = prev;
        prev = null;
    }
}
