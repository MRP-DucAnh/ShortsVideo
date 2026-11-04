# System states

Reference: `flowdrama-android-mockup.html`, section 5 (SCR-0601–0605).

- SCR-0601: shared artwork skeleton keeps the image bounds; data loading uses skeletons for Home, episode grids, History and Reels. Shimmer is static when the system animation duration scale is zero. Image failures stop shimmer and retain a neutral placeholder.
- SCR-0602: empty Saved uses the heart illustration, watchlist invitation and Browse action. Filtered empty lists remain distinct from an empty watchlist.
- SCR-0603: Home replaces its content when the default network lacks validated Internet, including Wi-Fi without Internet. Navigation remains available. Retry checks the current system network state and opens the Android Internet panel automatically when still offline. Android Internet settings are also available explicitly. The settings panel falls back to wireless settings and then general settings on devices without the panel.
- Player rechecks the current network immediately when stalled, then every 500 ms while waiting, to avoid stale callback values after radios are disabled. Player uses SCR-0603 only when playback stalls or fails without Internet; buffered/cached playback can continue. Failed video loads re-prepare when Internet is validated again, retaining the playback position and the user's play/pause intent. Metadata failures have retry and reconnect recovery too.
- SCR-0604: buffering with Internet shows Loading, dims the timeline and disables seeking. After three seconds, Media3 is constrained to 480p and an available HTTPS source at 480p or below is selected without losing position. A fixed single-quality source cannot be transcoded; the message says a lower quality is being attempted unless the actual decoded format confirms it. Switch back to HD clears the cap and restores the original source, and is only shown inside the buffering overlay. It disappears with buffering so it cannot cover playing video.
- SCR-0605: action feedback uses success green above the tab bar and dismisses after two seconds.

## Verification

Automated domain checks cover connected buffering, disconnected stalls/errors, cached playback offline, reconnect transitions, and connected server/decoder failures. Compose checks cover the offline retry action, network-settings affordance, and exact skeleton bounds. Build and lint passed. The five playback-state unit tests and two SystemStatesTest Compose tests passed on the connected Android 16 device. Results are recorded in the task verification logs.

Manual scenarios for a network-controlled device: disconnect on Home; disconnect during playback and wait for cached bytes to drain; reconnect without leaving the screen; throttle Internet while it remains validated; switch back to HD; return from Internet settings; enable Reduce Motion and inspect the skeleton. Device network settings are not changed by the automated tests.

Once SCR-0603 has appeared, it remains visible and playback stays paused until a fresh network check confirms Internet has returned. A Media3 READY/IDLE transition alone cannot dismiss it.

Reels draws SCR-0603 at screen level above the video, captions and controls, with a dark translucent scrim that blocks background gestures. The offline description uses 13sp text, 19sp line height and an additional 10dp horizontal inset.
