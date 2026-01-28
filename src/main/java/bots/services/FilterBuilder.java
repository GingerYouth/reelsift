package bots.services;

import filters.ExcludedGenres;
import filters.Filters;
import filters.Genre;
import filters.LlmFilter;
import filters.MandatoryGenres;
import filters.TimeFilter;

import java.util.Set;
import java.util.stream.Collectors;

/** Builds filter chains based on user preferences. */
public class FilterBuilder {

    private final UserService userService;

    public FilterBuilder(final UserService userService) {
        this.userService = userService;
    }

    /**
     * Build filters for the given chat.
     *
     * @param chatId The chat identifier
     * @return Configured filters
     */
    public Filters buildFilters(final long chatId) {
        final Filters filters = new Filters();
        addTimeFilter(filters, chatId);
        addMandatoryGenresFilter(filters, chatId);
        addExcludedGenresFilter(filters, chatId);
        addAiFilter(filters, chatId);
        return filters;
    }

    private void addTimeFilter(final Filters filters, final long chatId) {
        final String timeFilter = this.userService.getTimeFilter(chatId);
        if (timeFilter != null) {
            final String[] parts = timeFilter.split("-");
            filters.addFilter(new TimeFilter(parts[0], parts[1]));
        }
    }

    private void addMandatoryGenresFilter(final Filters filters, final long chatId) {
        final Set<Genre> mandatoryGenres = this.userService.getMandatoryGenres(chatId);
        if (mandatoryGenres != null) {
            filters.addFilter(
                new MandatoryGenres(
                    mandatoryGenres.stream()
                        .map(Genre::getDisplayName)
                        .collect(Collectors.toList())
                )
            );
        }
    }

    private void addExcludedGenresFilter(final Filters filters, final long chatId) {
        final Set<Genre> excludedGenres = this.userService.getExcludedGenres(chatId);
        if (excludedGenres != null) {
            filters.addFilter(
                new ExcludedGenres(
                    excludedGenres.stream()
                        .map(Genre::getDisplayName)
                        .collect(Collectors.toList())
                )
            );
        }
    }

    private void addAiFilter(final Filters filters, final long chatId) {
        final String aiPrompt = this.userService.getAiPrompt(chatId);
        if (aiPrompt != null) {
            filters.addFilter(new LlmFilter(aiPrompt));
        }
    }
}
