package at.tomtasche.datmix.spotify.rest;

/**
 * Created by tom on 10/7/14.
 */
public class PositionedTracksContainer {
    private final PositionedTrack[] tracks;

    public PositionedTracksContainer(PositionedTrack... tracks) {
        this.tracks = tracks;
    }

    public PositionedTrack[] getItems() {
        return tracks;
    }
}
