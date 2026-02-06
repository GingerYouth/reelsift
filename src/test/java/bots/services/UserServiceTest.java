package bots.services;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private UserService userService;
    private static final long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        this.userService = new UserService();
    }

    @Test
    void canUseAiFilterForNewUserReturnsTrue() {
        assertTrue(
            this.userService.canUseAiFilter(CHAT_ID),
            "A new user should be able to use the AI filter."
        );
    }

    @Test
    void canUseAiFilterUnderLimitReturnsTrue() {
        for (int i = 0; i < 5; i++) {
            this.userService.incrementAiUsageCount(CHAT_ID);
        }
        assertTrue(
            this.userService.canUseAiFilter(CHAT_ID),
            "A user under the limit should be able to use the AI filter."
        );
    }

    @Test
    void canUseAiFilterAtLimitReturnsFalse() {
        for (int i = 0; i < UserService.AI_USAGE_LIMIT; i++) {
            this.userService.incrementAiUsageCount(CHAT_ID);
        }
        assertFalse(
            this.userService.canUseAiFilter(CHAT_ID),
            "A user at the limit should not be able to use the AI filter."
        );
    }

    @Test
    void canUseAiFilterOverLimitReturnsFalse() {
        for (int i = 0; i < UserService.AI_USAGE_LIMIT + 1; i++) {
            this.userService.incrementAiUsageCount(CHAT_ID);
        }
        assertFalse(
            this.userService.canUseAiFilter(CHAT_ID),
            "A user over the limit should not be able to use the AI filter."
        );
    }

    @Test
    void canUseAiFilterLimitResetsNextDay() {
        long yesterday = LocalDate.now().minusDays(1).toEpochDay();
        this.userService.getDailyAiUsageCount().put(CHAT_ID, UserService.AI_USAGE_LIMIT);
        this.userService.getLastAiUsageDay().put(CHAT_ID, yesterday);
        assertTrue(
            this.userService.canUseAiFilter(CHAT_ID),
            "AI filter limit should reset on a new day."
        );
        this.userService.incrementAiUsageCount(CHAT_ID);
        for (int i = 0; i < UserService.AI_USAGE_LIMIT - 2; i++) {
            this.userService.incrementAiUsageCount(CHAT_ID);
        }
        assertTrue(
            this.userService.canUseAiFilter(CHAT_ID),
            String.format(
                "User should be able to use the filter %d times after a reset",
                UserService.AI_USAGE_LIMIT - 1
            )
        );
        this.userService.incrementAiUsageCount(CHAT_ID);
        assertFalse(
            this.userService.canUseAiFilter(CHAT_ID),
            String.format(
                "User should not be able to use the filter after %d uses on the new day",
                UserService.AI_USAGE_LIMIT
            )
        );
    }
}
