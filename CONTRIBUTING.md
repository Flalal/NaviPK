# Contributing to NaviPK

Thanks for your interest in contributing! Here's how to get started.

## How to Contribute

1. **Fork** the repository
2. **Create a branch** from `main` (`git checkout -b feature/my-feature`)
3. **Make your changes** and commit them
4. **Push** to your fork and open a **Pull Request**

Please keep PRs focused — one feature or fix per PR.

## Development Setup

- **Android Studio** (latest stable)
- **Android SDK 26+** (min SDK) / **SDK 36** (target)
- A running **Navidrome** server (or any Subsonic-compatible server) for testing

```bash
git clone https://github.com/<your-fork>/NaviPK.git
cd NaviPK
# Open in Android Studio → Gradle Sync → Run
```

## Code Style

- **Kotlin** — follow standard [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Jetpack Compose** — prefer stateless composables, hoist state up
- **UI labels are in French** — keep all user-facing strings in French to match the existing UI
- **Singletons** — `PlayerManager`, `CacheManager`, `SubsonicClient`, `YouTubeLibraryManager`, `RadioManager` are Kotlin `object`s. Do not convert them to dependency-injected classes without discussion
- **Theme** — always dark. Use `MaterialTheme.colorScheme` for colors, not hardcoded values

## Commit Messages

Follow this style (based on the project's existing history):

```
Short summary of the change

Optional longer description if needed.
```

Examples from the repo:
- `Add local YouTube favorites and playlists support`
- `Fix fond transparent file d'attente + auto version bump CI`
- `v1.5.0 — Playlists avancées + Radio`

## Reporting Issues

Open an issue on GitHub with:

- **What you expected** vs. **what happened**
- **Steps to reproduce**
- **Device / Android version**
- Logs if available (`adb logcat`)

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
