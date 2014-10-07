package at.tomtasche.datmix.spotify.rest;

public class Track {
    private final String id;
    private final String name;
    private final String uri;

    public Track(final String id, final String name, final String uri) {
        this.id = id;
        this.name = name;
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
