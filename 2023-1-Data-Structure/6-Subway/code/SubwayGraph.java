import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

public class SubwayGraph {
    HashMap<String, ArrayList<Station>> stations;       // Subway Graph Data Structure

    public SubwayGraph(String filename) {
        // Initialize Graph
        stations = new HashMap<>();
        parseFile(filename);
    }

    /*
     * Find the Fastest Path from departure to destination, using dijkstra algorithm.
     * Edited code by ChatGPT, the response code is attached in comments at the end of this file.
     */
    public Route findFastestRoute(ArrayList<Station> departures, ArrayList<Station> destinations) {
        HashSet<Station> visited = new HashSet<>();                     // Check the Station Node Already in the shortest path set
        PriorityQueue<Route> priorityQueue = new PriorityQueue<>();     // To get the shortest path in every step

        for (Station station : departures) {
            priorityQueue.add(new Route(station));                      // Add Start Node
        }
        while (!priorityQueue.isEmpty()) {
            // Get the shortest path in this step
            Route route = priorityQueue.poll();

            // If the looking node(Station) is already in the shortest path set, just pop it
            Station station = route.getLast();
            if (visited.contains(station))
                continue;
            else
                visited.add(station);

            // If one of the destination(Station) is in the shortest path set, return the route
            if (destinations.contains(station))
                return route;

            // Perform dijkstra algorithm for each
            for (Railway railway : station.adjacentStations) {
                if (visited.contains(railway.destination))
                    continue;
                else {
                    Route tempRoute = new Route(route);
                    tempRoute.add(railway);
                    priorityQueue.add(tempRoute);
                }
            }
        }
        return null;
    }

    private void parseFile(String filename) {
        HashMap<String, Station> tempStationMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            //Parse first section
            String line;
            while (!(line = reader.readLine()).equals("")) {
                String[] arguments = line.split(" ");
                Station station = new Station(arguments);

                ArrayList<Station> stationList = stations.get(arguments[1]);
                if (stationList == null) {
                    stationList = new ArrayList<>();
                    stations.put(arguments[1], stationList);
                }
                stationList.add(station);
                if (tempStationMap.get(arguments[0]) != null)
                    throw new IOException("Station ID Duplicated");
                tempStationMap.put(arguments[0], station);
            }

            //Parse second section
            while ((line = reader.readLine()) != null && !line.equals("")) {
                String[] arguments = line.split(" ");
                if (arguments.length != 3)
                    throw new IOException("Not Proper Railway Information");
                Station departure = tempStationMap.get(arguments[0]);
                Station destination = tempStationMap.get(arguments[1]);
                if (departure == null) {
                    throw new IOException(arguments[0] + ": No Such Station ID");
                }
                else if (destination == null) {
                    throw new IOException(arguments[1] + ": No Such Station ID");
                }
                departure.adjacentStations.add(new Railway(destination, Integer.parseInt(arguments[2])));
            }
            if (line != null) {
                //Parse third section
                while ((line = reader.readLine()) != null) {
                    String[] arguments = line.split(" ");
                    if (arguments.length != 2)
                        throw new IOException("Not Proper Transfer Information");
                    ArrayList<Station> stationList = stations.get(arguments[0]);
                    if (stationList == null)
                        throw new IOException(arguments[0] + ": Station not exists");
                    else {
                        connectTransfer(stationList, Integer.parseInt(arguments[1]));
                    }
                }
            }
            //Connect other transfer stations
            for (ArrayList<Station> stationList : stations.values()) {
                if (stationList.size() == 1)
                    continue;
                if (!isTransferConnected(stationList)) {
                    connectTransfer(stationList, 5);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("File Not Found");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isTransferConnected(ArrayList<Station> stationList) {
        Station station = stationList.get(0);
        if (station.adjacentStations.isEmpty())
            return false;
        else
            return station.adjacentStations.get(station.adjacentStations.size() - 1).destination.name.equals(station.name);
    }

    private void connectTransfer(ArrayList<Station> stationList, int transferTime) {
        for (int i = 0; i < stationList.size() - 1; i++) {
            for (int j = i + 1; j < stationList.size(); j++) {
                Station transferFrom = stationList.get(i);
                Station transferTo = stationList.get(j);

                transferFrom.adjacentStations.add(new Railway(transferTo, transferTime));
                transferTo.adjacentStations.add(new Railway(transferFrom, transferTime));
            }
        }
    }
}


/*
    Reference by ChatGPT code for Dijkstra Algorithm

public class DijkstraAlgorithm {
    public static Route findShortestPath(HashMap<String, ArrayList<Station>> stations, String sourceName, String destinationName) {
        // Create a PriorityQueue to store the routes based on their times
        PriorityQueue<Route> queue = new PriorityQueue<>();

        // Create a HashSet to keep track of visited stations
        HashSet<Station> visited = new HashSet<>();

        // Get the source and destination stations from the HashMap
        Station source = findStationByName(stations, sourceName);
        Station destination = findStationByName(stations, destinationName);

        // Create an initial route starting from the source station
        Route initialRoute = new Route(source);
        queue.add(initialRoute);

        while (!queue.isEmpty()) {
            // Remove the route with the minimum time from the queue
            Route currentRoute = queue.poll();
            Station currentStation = currentRoute.getLast();

            // If the current station is the destination, return the current route
            if (currentStation == destination) {
                return currentRoute;
            }

            // Check if the current station has been visited
            if (visited.contains(currentStation)) {
                continue;
            }

            // Mark the current station as visited
            visited.add(currentStation);

            // Get the adjacent stations of the current station
            ArrayList<Railway> adjacentStations = currentStation.adjacentStations;

            // Iterate over the adjacent stations
            for (Railway railway : adjacentStations) {
                Station adjacentStation = railway.destination;

                // Check if the adjacent station has been visited
                if (visited.contains(adjacentStation)) {
                    continue;
                }

                // Calculate the time for the new route
                int newTime = currentRoute.time + railway.time;

                // Create a new route with the adjacent station as the last station
                Route newRoute = new Route(source);
                newRoute.route.addAll(currentRoute.route);
                newRoute.add(railway);

                // Add the new route to the queue
                queue.add(newRoute);
            }
        }

        // If there is no path from the source to the destination, return null
        return null;
    }

    // Helper method to find a station by name in the HashMap
    private static Station findStationByName(HashMap<String, ArrayList<Station>> stations, String name) {
        for (ArrayList<Station> stationList : stations.values()) {
            for (Station station : stationList) {
                if (station.name.equals(name)) {
                    return station;
                }
            }
        }
        return null;
    }
}

 */


