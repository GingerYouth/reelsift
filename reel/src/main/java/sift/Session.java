package main.java.sift;

import java.time.LocalTime;

/** Session DTO. */
public record Session(
    LocalTime dateTime, String name, String description, String verdict,
    String cinema, String address, int price, String link) {
}

