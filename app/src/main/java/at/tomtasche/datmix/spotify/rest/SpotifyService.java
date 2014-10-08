package at.tomtasche.datmix.spotify.rest;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface SpotifyService {

    @GET("/artists/{album_id}/albums")
    Paged<Album[]> getAlbums(@Path("album_id") String artistId);

    @GET("/me")
    Me getMe();

    @GET("/users/{user_id}/playlists")
    Paged<Playlist[]> getPlaylists(@Path("user_id") String userId);

    @GET("/users/{user_id}/playlists/{playlist_id}/tracks")
    Paged<PlaylistTrack[]> getTracks(@Path("user_id") String userId, @Path("playlist_id") String playlistId);

    @Headers("Content-Type: application/json")
    @BODYDELETE("/users/{user_id}/playlists/{playlist_id}/tracks")
    Response removeTrack(@Path("user_id") String userId, @Path("playlist_id") String playlistId, @Body PositionedTracksContainer removeTracks);

    @POST("/users/{user_id}/playlists/{playlist_id}/tracks")
    Response addTrack(@Path("user_id") String userId, @Path("playlist_id") String playlistId, @Query("uris") List<String> uris, @Query("position") int position);
}
