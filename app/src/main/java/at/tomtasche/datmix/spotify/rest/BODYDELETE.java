package at.tomtasche.datmix.spotify.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import retrofit.http.RestMethod;

/**
 * Created by tom on 10/7/14.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RestMethod(value = "DELETE", hasBody = true)
public @interface BODYDELETE {
    String value();
}