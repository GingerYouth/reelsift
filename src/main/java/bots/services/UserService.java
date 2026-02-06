package bots.services;

import bots.enums.UserState;
import filters.DateInterval;
import filters.Genre;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import parser.City;

@SuppressWarnings("PMD.TooManyMethods")
public class UserService {
    private final Map<Long, City> userCities = new ConcurrentHashMap<>();
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, DateInterval> dateFilters = new ConcurrentHashMap<>();
    private final Map<Long, String> timeFilters = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> excludedGenres = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> mandatoryGenres = new ConcurrentHashMap<>();
    private final Map<Long, String> aiPrompts = new ConcurrentHashMap<>();
    private final Set<Long> subFilters = ConcurrentHashMap.newKeySet();
    private final Map<Long, Integer> dailyAiUsageCount = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastAiUsageDay = new ConcurrentHashMap<>();
    public static final int AI_USAGE_LIMIT = 20;

    public City getUserCity(final long chatId) {
        return this.userCities.getOrDefault(chatId, City.MOSCOW);
    }

    public void setUserCity(final long chatId, final City city) {
        this.userCities.put(chatId, city);
    }

    public boolean hasUserCity(final long chatId) {
        return this.userCities.containsKey(chatId);
    }

    public UserState getUserState(final long chatId) {
        return this.userStates.getOrDefault(chatId, UserState.IDLE);
    }

    public void setUserState(final long chatId, final UserState state) {
        this.userStates.put(chatId, state);
    }

    public DateInterval getDateFilter(final long chatId) {
        return this.dateFilters.get(chatId);
    }

    public void setDateFilter(final long chatId, final DateInterval dateInterval) {
        this.dateFilters.put(chatId, dateInterval);
    }

    public void removeDateFilter(final long chatId) {
        this.dateFilters.remove(chatId);
    }

    public String getTimeFilter(final long chatId) {
        return this.timeFilters.get(chatId);
    }

    public void setTimeFilter(final long chatId, final String timeFilter) {
        this.timeFilters.put(chatId, timeFilter);
    }

    public void removeTimeFilter(final long chatId) {
        this.timeFilters.remove(chatId);
    }

    public Set<Genre> getExcludedGenres(final long chatId) {
        return this.excludedGenres.get(chatId);
    }

    public void setExcludedGenres(final long chatId, final Set<Genre> genres) {
        this.excludedGenres.put(chatId, genres);
    }

    public void removeExcludedGenres(final long chatId) {
        this.excludedGenres.remove(chatId);
    }

    public Set<Genre> getMandatoryGenres(final long chatId) {
        return mandatoryGenres.get(chatId);
    }

    public void setMandatoryGenres(final long chatId, final Set<Genre> genres) {
        this.mandatoryGenres.put(chatId, genres);
    }

    public void removeMandatoryGenres(final long chatId) {
        this.mandatoryGenres.remove(chatId);
    }

    public String getAiPrompt(final long chatId) {
        return this.aiPrompts.get(chatId);
    }

    public void setAiPrompt(final long chatId, final String prompt) {
        this.aiPrompts.put(chatId, prompt);
    }

    public void removeAiPrompt(final long chatId) {
        this.aiPrompts.remove(chatId);
    }

    public boolean hasSubsFilter(final long chatId) {
        return this.subFilters.contains(chatId);
    }

    public void addSubsFilter(final long chatId) {
        this.subFilters.add(chatId);
    }

    public void removeSubsFilter(final long chatId) {
        this.subFilters.remove(chatId);
    }

    public void clearAllFilters(final long chatId) {
        this.excludedGenres.remove(chatId);
        this.mandatoryGenres.remove(chatId);
        this.dateFilters.remove(chatId);
        this.timeFilters.remove(chatId);
        this.aiPrompts.remove(chatId);
        this.subFilters.remove(chatId);
    }

    public Map<Long, Long> getLastAiUsageDay() {
        return this.lastAiUsageDay;
    }

    public Map<Long, Integer> getDailyAiUsageCount() {
        return this.dailyAiUsageCount;
    }

    public boolean canUseAiFilter(final long chatId) {
        final long today = LocalDate.now().toEpochDay();
        final long userLastAiUsageDay = this.lastAiUsageDay.getOrDefault(chatId, 0L);
        int currentCount = this.dailyAiUsageCount.getOrDefault(chatId, 0);
        if (today > userLastAiUsageDay) {
            this.dailyAiUsageCount.put(chatId, 0);
            this.lastAiUsageDay.put(chatId, today);
            currentCount = 0;
        }
        return currentCount < AI_USAGE_LIMIT;
    }

    public void incrementAiUsageCount(final long chatId) {
        final long today = LocalDate.now().toEpochDay();
        final long userLastAiUsageDay = this.lastAiUsageDay.getOrDefault(chatId, 0L);
        final int currentCount = this.dailyAiUsageCount.getOrDefault(chatId, 0);
        if (today > userLastAiUsageDay) {
            this.dailyAiUsageCount.put(chatId, 1);
            this.lastAiUsageDay.put(chatId, today);
        } else {
            this.dailyAiUsageCount.put(chatId, currentCount + 1);
            this.lastAiUsageDay.put(chatId, today);
        }
    }
}