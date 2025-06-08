package main.java.sift;

import java.net.URL;
import java.util.Date;

/** Show class. */
public class Show {
    private Date dateTime;
    private String name;
    private String description;
    private URL link;

    public Show(final Date dateTime, final String name, final String description, final URL link) {
        this.dateTime = dateTime;
        this.name = name;
        this.description = description;
        this.link = link;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public URL getLink() {
        return link;
    }
}

