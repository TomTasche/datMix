package at.tomtasche.datmix.spotify.rest;

public class PlaylistTrack {
    private final Track track;

    public PlaylistTrack(final Track track) {
        this.track = track;
    }

    public Track getTrack() {
        return track;
    }

    @Override
    public String toString() {
        return track.getName();
    }
}
