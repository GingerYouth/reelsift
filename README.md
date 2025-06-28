[![Maintainability](https://qlty.sh/badges/898c8a2a-8894-4261-9f4a-0d8919ebdf1a/maintainability.svg)](https://qlty.sh/gh/GingerYouth/projects/reelsift)

### Overview
ReelSift is a Telegram bot and backend application that helps users find and filter movie showtimes using a combination of manual filters and AI-powered prompts. It scrapes Afisha.ru for today's films and enables users to specify their movie preferences directly via Telegram.

### How to use?
Write to @ReelSiftBot in telegram.

## Features
- **Telegram Bot Integration:** Interact with a Telegram bot to search for movies and set filters.
- **Schedule Parsing:** Automatically fetches and parses today's film schedule from Afisha.ru.
- **Flexible Filtering:**
    - **Time Filter:** Set desired time ranges for movie sessions.
    - **Excluded Genres:** Specify genres you don't want to see.
    - **Mandatory Genres:** Choose genres you want to see.
    - **AI Prompt Filter:** Use natural language prompts to further refine movie recommendations (e.g., "I want to see a movie about heroism").

### Technologies
- Java 24
- Mill build tool
- JSoup (web scraping)
- TelegramBots library