package main.java.sift;

import java.time.LocalTime;
import java.util.List;

/** Session DTO. */
public record Session(
    LocalTime dateTime, String name, String description, String verdict,
    List<String> genres, String cinema, String address, int price, String link) {
}

