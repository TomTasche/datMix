package at.tomtasche.datmix;

import com.orm.SugarRecord;

public class TrackHistory extends SugarRecord<TrackHistory> {

	private String spotifyUri;
	private int playCount;

	public TrackHistory() {
	}

	public TrackHistory(String spotifyUri) {
		this.spotifyUri = spotifyUri;

		playCount = 0;
	}

	public synchronized void increasePlayCount() {
		playCount++;
	}

	public int getPlayCount() {
		return playCount;
	}

	public String getSpotifyUri() {
		return spotifyUri;
	}
}
