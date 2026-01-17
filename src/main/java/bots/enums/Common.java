package bots.enums;

import java.util.Optional;

/** Common enum. */
public enum Common {
    BACK("🔙 Назад"),
    NOT_SET_UP("не заданы");

    private final String name;

    Common(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<String> getEnumByString(final String string) {
        for (final Common common : values()) {
            if (common.name.equals(string)) {
                return Optional.of(common.name());
            }
        }
        return Optional.empty();
    }
}
