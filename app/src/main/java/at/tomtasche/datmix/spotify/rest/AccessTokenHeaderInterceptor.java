package at.tomtasche.datmix.spotify.rest;

import retrofit.RequestInterceptor;

/**
 * Created by tom on 10/10/14.
 */
public class AccessTokenHeaderInterceptor implements RequestInterceptor {

    private String accessToken;

    public AccessTokenHeaderInterceptor(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public void intercept(RequestFacade request) {
        request.addHeader("Authorization", "Bearer " + accessToken);
    }
}