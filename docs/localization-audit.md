# Localization audit

Launch languages: en, hi, ta, id, ar, fr, es, pt, tr, de.

## UI strings

103 shared UI strings use Android localized resources. App-selected language is used for shared labels, accessibility descriptions, messages, system states and formatted Reels actions.

| Resource directory | Default keys |
| --- | ---: |
| app/src/main/res | 10 |
| core/ads/src/main/res | 1 |
| core/designsystem/src/main/res | 103 |
| feature/browse/src/main/res | 11 |
| feature/home/src/main/res | 22 |
| feature/library/src/main/res | 32 |
| feature/onboarding/src/main/res | 57 |
| feature/playback/src/main/res | 15 |

Coverage and format checks: PASS. All translatable resource keys have counterparts in all launch languages.

## Catalog content still requiring translations

These are content fields supplied by the catalog, separate from UI resources. The app retains its default-language fallback.

| Language | Missing show titles | Missing synopses | Missing genre/rail labels |
| --- | ---: | ---: | ---: |
| en | 0 | 0 | 0 |
| hi | 0 | 0 | 4 |
| ta | 0 | 0 | 4 |
| id | 0 | 0 | 4 |
| ar | 0 | 0 | 4 |
| fr | 7 | 7 | 18 |
| es | 7 | 7 | 18 |
| pt | 7 | 7 | 18 |
| tr | 7 | 7 | 18 |
| de | 7 | 7 | 18 |

The seven show hooks are empty in every language. Episode titles are currently a single string in the API/model, so they cannot select a localized title. Completing these fields requires translated catalog data and an API/model extension for episode titles. Brand names, SDK names, language endonyms, quality values and playback-speed notation remain unchanged.

Runtime verification: build, lint, app unit tests, and two LocalizationTest instrumentation tests passed on Android 16. The device tests cover all ten selected app languages and German Settings rendering.

Validation covers resource completeness and format placeholders. Translation wording and truncation on every screen still benefit from native-speaker/device review.
