package at.tomtasche.datmix.spotify.rest;

public class Me {
    private final String display_name;
    private final String email;
    private final String id;

    public Me(final String displayName, final String email, final String id) {
        this.display_name = displayName;
        this.email = email;
        this.id = id;
    }

    public String getDisplayName() {
        return display_name;
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }
}
