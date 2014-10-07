package at.tomtasche.datmix.spotify.rest;

import retrofit.http.GET;
import retrofit.http.Path;

public interface SpotifyService {

    @GET("/artists/{album_id}/albums")
    Paged<Album[]> getAlbums(@Path("album_id") String artistId);

    @GET("/me")
    Me getMe();

    @GET("/users/{user_id}/playlists")
    Paged<Playlist[]> getPlaylists(@Path("user_id") String userId);

    @GET("/users/{user_id}/playlists/{playlist_id}/tracks")
    Paged<PlaylistTrack[]> getTracks(@Path("user_id") String userId, @Path("playlist_id") String playlistId);
}
