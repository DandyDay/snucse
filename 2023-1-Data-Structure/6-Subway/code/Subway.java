import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Subway {

    public static void main(String[] args) {
        // Check arguments
        if (args.length != 1) {
            System.err.println("usage: java Subway [SubwayInfo.txt]");
            System.exit(1);
        }

        // Initialize SubwayGraph
        SubwayGraph subwayGraph = new SubwayGraph(args[0]);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String input = br.readLine();

                // Exit Program when input is EOF or "QUIT"
                if (input == null || input.compareTo("QUIT") == 0)
                    break;

                // Handle command
                command(input, subwayGraph);
            } catch (IOException e) {
                System.out.println("입력이 잘못되었습니다. 오류 : " + e.toString());
            }
        }
    }

    public static void command(String input, SubwayGraph subwayGraph) throws IOException {
        String[] arguments = input.split(" ");

        // Check Input Format
        if (arguments.length != 2)
            throw new IOException("usage: Departure Destination");

        // Get departure and destination ArrayList
        ArrayList<Station> departures = subwayGraph.stations.get(arguments[0]);
        ArrayList<Station> destinations = subwayGraph.stations.get(arguments[1]);

        // Error Handling
        if (departures == null)
            throw new IOException(arguments[0] + " does not exist");
        else if (destinations == null)
            throw new IOException(arguments[1] + " does not exist");

        // Find path with dijkstra algorithm
        Route fastestRoute = subwayGraph.findFastestRoute(departures, destinations);

        // Error Handling
        if (fastestRoute == null)
            throw new IOException("There is no way from " + arguments[0] + " to " + arguments[1]);

        // Prints Result
        System.out.println(fastestRoute);
    }
}
