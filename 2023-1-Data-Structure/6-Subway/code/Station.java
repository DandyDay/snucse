import java.io.IOException;
import java.util.ArrayList;

public class Station {
    String id;
    String name;
    String line;
    ArrayList<Railway> adjacentStations;

    Station(String id, String name, String line) {
        this.id = id;
        this.name = name;
        this.line = line;
        this.adjacentStations = new ArrayList<>();
    }

    Station(String[] arguments) throws IOException {
        if (arguments.length != 3)
            throw new IOException("Not Proper Station Information");
        this.id = arguments[0];
        this.name = arguments[1];
        this.line = arguments[2];
        this.adjacentStations = new ArrayList<>();
    }

    @Override
    public String toString() {
        return this.name;
    }
}