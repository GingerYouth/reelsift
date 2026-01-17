package bots.enums;

/** User state enum. */
public enum UserState {
    AWAITING_DATE,
    AWAITING_TIME,
    AWAITING_EXCLUDED,
    AWAITING_MANDATORY,
    AWAITING_AI,
    EDITING_FILTERS,
    DELETING_FILTERS,
    SEARCHING,
    IDLE
}
