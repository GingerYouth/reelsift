package main.java.sift;

/** City enum. */
public enum City {
    MOSCOW(),
    SPB();

    public String asCode() {
        return switch (this) {
            case MOSCOW -> "msk";
            case SPB -> "spb";
        };
    }

    public String asPrefix() {
        return this.name() + ':';
    }
}
