package demux;

/**
*/
public class IdMessage implements Message {
    private final int id;

    IdMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
