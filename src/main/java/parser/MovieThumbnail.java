package parser;

import java.util.Objects;

/**
 * Movie thumbnail DTO.
 */
public final class MovieThumbnail {
    private final String name;
    private final String sessionsLink;
    private final String imageLink;

    /** Constructor. */
    public MovieThumbnail(
        final String name,
        final String sessionsLink,
        final String imageLink
    ) {
        this.name = name;
        this.sessionsLink = sessionsLink;
        this.imageLink = imageLink;
    }

    public String name() {
        return this.name;
    }

    public String sessionsLink() {
        return this.sessionsLink;
    }

    public String imageLink() {
        return this.imageLink;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final MovieThumbnail that = (MovieThumbnail) obj;
        return Objects.equals(this.name, that.name) &&
            Objects.equals(this.sessionsLink, that.sessionsLink) &&
            Objects.equals(this.imageLink, that.imageLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.sessionsLink, this.imageLink);
    }

    @Override
    public String toString() {
        return "MovieThumbnail[" +
            "name=" + this.name + ", " +
            "sessionsLink=" + this.sessionsLink + ", " +
            "imageLink=" + this.imageLink + ']';
    }

}
