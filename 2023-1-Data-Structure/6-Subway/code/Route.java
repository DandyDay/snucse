import java.util.ArrayList;
import java.util.Stack;

public class Route implements Comparable<Route> {
    int time;
    ArrayList<Station> route;

    Route(Station departure) {
        this.time = 0;
        this.route = new ArrayList<>();
        this.route.add(departure);
    }

    Route(Route o) {
        this.time = o.time;
        this.route = new ArrayList<>();
        for (Station station : o.route)
            route.add(station);
    }

    public void add(Railway railway) {
        this.time += railway.time;
        this.route.add(railway.destination);
    }

    public Station getLast() {
        return route.get(route.size() - 1);
    }

    @Override
    public int compareTo(Route o) {
        return Integer.compare(this.time, o.time);
    }

    @Override
    public String toString() {
        Stack<String> stations = new Stack<>();
        for (Station station : this.route) {
            if (stations.isEmpty() || !stations.peek().equals(station.toString()))
                stations.add(station.toString());
            else {
                stations.pop();
                stations.add("[" + station.toString() + "]");
            }
        }
        String ret = "";
        for (String stationString : stations) {
            ret += " " + stationString;
        }
        return ret.substring(1, ret.length()) + "\n" + Integer.toString(this.time);
    }
}