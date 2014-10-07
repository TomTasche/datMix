package at.tomtasche.datmix.spotify.rest;

public class PositionedTrack {
    private final String uri;
    private final Integer[] positions;

    public PositionedTrack(final String uri, final Integer[] positions) {
        this.uri = uri;
        this.positions = positions;
    }

    public String getUri() {
        return uri;
    }

    public Integer[] getPositions() {
        return positions;
    }
}
