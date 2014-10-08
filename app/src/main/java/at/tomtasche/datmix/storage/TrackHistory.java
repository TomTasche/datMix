package at.tomtasche.datmix.storage;

import com.orm.SugarRecord;

public class TrackHistory extends SugarRecord<TrackHistory> {

    private String spotifyUri;
    private int playCount;
    private int skipCount;

    public TrackHistory() {
    }

    public TrackHistory(String spotifyUri) {
        this.spotifyUri = spotifyUri;

        playCount = 0;
        skipCount = 0;
    }

    public synchronized void increasePlayCount() {
        playCount++;
    }

    public synchronized void increaseSkipCount() {
        skipCount++;
    }

    public String getSpotifyUri() {
        return spotifyUri;
    }

    public int getPlayCount() {
        return playCount;
    }

    public int getSkipCount() {
        return skipCount;
    }
}
