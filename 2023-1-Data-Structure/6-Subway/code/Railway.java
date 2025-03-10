public class Railway {
    int time;
    Station destination;
    Railway(Station destination, int time) {
        this.destination = destination;
        this.time = time;
    }

    @Override
    public String toString() {
        return "->(" + time + ") " + destination.name + "[" + destination.line + "]";
    }
}