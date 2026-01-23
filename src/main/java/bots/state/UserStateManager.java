package bots.state;

import bots.enums.UserState;
import filters.DateInterval;
import filters.Genre;
import parser.City;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user state and filter preferences. Encapsulates all user-related data storage
 * and provides typed access to user settings.
 */
public class UserStateManager {

    private final Map<Long, City> userCities = new ConcurrentHashMap<>();
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, DateInterval> dateFilters = new ConcurrentHashMap<>();
    private final Map<Long, String> timeFilters = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> excludedGenres = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> mandatoryGenres = new ConcurrentHashMap<>();
    private final Map<Long, String> aiPrompts = new ConcurrentHashMap<>();
    private final Set<Long> subFilters = ConcurrentHashMap.newKeySet();

    /**
     * Initializes a new user with default city (Moscow).
     *
     * @param chatId the chat ID
     */
    public void initializeUser(final long chatId) {
        this.userCities.putIfAbsent(chatId, City.MOSCOW);
        this.userStates.putIfAbsent(chatId, UserState.IDLE);
    }

    /**
     * Checks if a user exists.
     *
     * @param chatId the chat ID
     * @return true if user exists
     */
    public boolean userExists(final long chatId) {
        return this.userCities.containsKey(chatId);
    }

    // City management
    public City getCity(final long chatId) {
        return this.userCities.getOrDefault(chatId, City.MOSCOW);
    }

    public void setCity(final long chatId, final City city) {
        this.userCities.put(chatId, city);
    }

    // State management
    public UserState getState(final long chatId) {
        return this.userStates.getOrDefault(chatId, UserState.IDLE);
    }

    public void setState(final long chatId, final UserState state) {
        this.userStates.put(chatId, state);
    }

    // Date filter management
    public DateInterval getDateFilter(final long chatId) {
        return this.dateFilters.get(chatId);
    }

    public void setDateFilter(final long chatId, final DateInterval interval) {
        this.dateFilters.put(chatId, interval);
    }

    public boolean hasDateFilter(final long chatId) {
        return this.dateFilters.containsKey(chatId);
    }

    public void removeDateFilter(final long chatId) {
        this.dateFilters.remove(chatId);
    }

    // Time filter management
    public String getTimeFilter(final long chatId) {
        return this.timeFilters.get(chatId);
    }

    public void setTimeFilter(final long chatId, final String timeFilter) {
        this.timeFilters.put(chatId, timeFilter);
    }

    public boolean hasTimeFilter(final long chatId) {
        return this.timeFilters.containsKey(chatId);
    }

    public void removeTimeFilter(final long chatId) {
        this.timeFilters.remove(chatId);
    }

    // Excluded genres management
    public Set<Genre> getExcludedGenres(final long chatId) {
        return this.excludedGenres.getOrDefault(chatId, Collections.emptySet());
    }

    public void setExcludedGenres(final long chatId, final Set<Genre> genres) {
        this.excludedGenres.put(chatId, genres);
    }

    public boolean hasExcludedGenres(final long chatId) {
        return this.excludedGenres.containsKey(chatId);
    }

    public void removeExcludedGenres(final long chatId) {
        this.excludedGenres.remove(chatId);
    }

    // Mandatory genres management
    public Set<Genre> getMandatoryGenres(final long chatId) {
        return this.mandatoryGenres.getOrDefault(chatId, Collections.emptySet());
    }

    public void setMandatoryGenres(final long chatId, final Set<Genre> genres) {
        this.mandatoryGenres.put(chatId, genres);
    }

    public boolean hasMandatoryGenres(final long chatId) {
        return this.mandatoryGenres.containsKey(chatId);
    }

    public void removeMandatoryGenres(final long chatId) {
        this.mandatoryGenres.remove(chatId);
    }

    // AI prompt management
    public String getAiPrompt(final long chatId) {
        return this.aiPrompts.get(chatId);
    }

    public void setAiPrompt(final long chatId, final String prompt) {
        this.aiPrompts.put(chatId, prompt);
    }

    public boolean hasAiPrompt(final long chatId) {
        return this.aiPrompts.containsKey(chatId);
    }

    public void removeAiPrompt(final long chatId) {
        this.aiPrompts.remove(chatId);
    }

    // Subtitle filter management
    public boolean hasSubtitleFilter(final long chatId) {
        return this.subFilters.contains(chatId);
    }

    public void enableSubtitleFilter(final long chatId) {
        this.subFilters.add(chatId);
    }

    public void disableSubtitleFilter(final long chatId) {
        this.subFilters.remove(chatId);
    }

    // Bulk operations
    public void clearAllFilters(final long chatId) {
        this.excludedGenres.remove(chatId);
        this.mandatoryGenres.remove(chatId);
        this.dateFilters.remove(chatId);
        this.timeFilters.remove(chatId);
        this.aiPrompts.remove(chatId);
        this.subFilters.remove(chatId);
    }
}
