package parser;

/** City enum. */
public enum City {
    MOSCOW(),
    SPB(),
    BALASHIHA();

    public String asCode() {
        return switch (this) {
            case MOSCOW -> "msk";
            case SPB -> "spb";
            case BALASHIHA -> "balashiha";
        };
    }

    public String asPrefix() {
        return this.name() + ':';
    }
}
