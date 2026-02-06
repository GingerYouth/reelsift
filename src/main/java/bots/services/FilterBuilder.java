package bots.services;

import filters.ExcludedGenres;
import filters.Filters;
import filters.Genre;
import filters.LlmFilter;
import filters.MandatoryGenres;
import filters.SubsFilter;
import filters.TimeFilter;

import java.util.Set;
import java.util.stream.Collectors;

/** Builds filter chains based on user preferences. */
public class FilterBuilder {

    private final UserService userService;
    private final MessageSender messageSender;

    public FilterBuilder(final UserService userService, final MessageSender messageSender) {
        this.userService = userService;
        this.messageSender = messageSender;
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
        addSubsFilter(filters, chatId);
        return filters;
    }

    private void addSubsFilter(final Filters filters, final long chatId) {
        if (this.userService.hasSubsFilter(chatId)) {
            filters.addFilter(new SubsFilter());
        }
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
            if (this.userService.canUseAiFilter(chatId)) {
                this.userService.incrementAiFilterCount(chatId);
                filters.addFilter(new LlmFilter(aiPrompt));
            } else {
                this.messageSender.sendMessage(
                    String.valueOf(chatId),
                    "Вы достигли дневного лимита на использование AI-фильтра."
                );
            }
        }
    }
}
