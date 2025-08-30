package main.java.sift;

/** City enum. */
public enum City {
    MOSCOW("msk"),
    SAINT_PETERSBURG("spb");

    City(final String name) {}

    public String asCode() {
        return switch (this) {
            case MOSCOW -> "msk";
            case SAINT_PETERSBURG -> "spb";
        };
    }

    public String asPrefix() {
        return this.name() + ':';
    }
}
