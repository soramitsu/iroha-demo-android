# AGENT NOTES – ExamplePoint (Android UAID Wallet Modernization)

Welcome! These pointers capture the repo conventions, common commands, and current architectural decisions so you can jump in quickly.

## Environment & Commands

- Build / test: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew assembleDebug`
- Android Gradle Plugin 8.5 w/ `compileSdk=35`; expect the warning about AGP not yet validated for 35.
- Modules: single `app/` module plus local `:iroha-sdk` (sourced from `/Users/takemiyamakoto/dev/i23` via `settings.gradle`). Do **not** bump SDK refs without confirming with the user.
- UI uses AndroidX + Material components; data binding is enabled (see `app/build.gradle`).

## Key Concepts

- Repo is being modernized to speak directly to Sora Nexus Torii (UAID portfolio/bindings/manifests). All ledger calls go through `io.soramitsu.examplepoint.network.ToriiClient`.
- UAID identity manifests are derived locally and cached (`sdk/identity/*`, `sdk/UaidCache.java`). Never remove the cache without providing an alternative offline strategy.
- Wallet uses MVP (presenters under `io.soramitsu.examplepoint.presenter`, views/fragments under `io.soramitsu.examplepoint.view.*`).
- New UAID-specific models live in `app/src/main/java/io/soramitsu/examplepoint/sdk/model/*`.
- Wallet UI now has three sections: holdings (portfolio), bindings, manifests. Rows launch `UaidDetailBottomSheet` for copy/share operations.

## File Map

- `app/src/main/java/io/soramitsu/examplepoint/sdk/IrohaRepository.java`: main data entry point. Adds UAID cache + futures.
- `app/src/main/java/io/soramitsu/examplepoint/network/ToriiClient.java`: REST integration. When adding endpoints, follow the DTO pattern already there.
- `app/src/main/java/io/soramitsu/examplepoint/view/adapter/WalletAdapter.java`: renders wallet recycler; pass row taps back through `WalletRowListener`.
- `app/src/main/java/io/soramitsu/examplepoint/view/fragment/WalletFragment.java`: orchestrates presenters + the detail sheet.
- `app/src/main/java/io/soramitsu/examplepoint/view/dialog/UaidDetailBottomSheet.java`: new bottom sheet for UAID details.

## Conventions / Gotchas

- Keep commits/changes ASCII unless strings already contain Unicode.
- Use `apply_patch` for manual edits; avoid IDE bulk formatting.
- When adding network calls, bubble `IOException | ToriiException`.
- The repo intentionally caches UAID data to tolerate Torii downtime; re-use `RepositoryResult<T>` so the UI can flag cached data.
- Clipboard actions should originate from fragments/dialogs (not adapters) to avoid context leaks.
- **Testing:** Whenever you add a new function, also add at least one unit test covering it. For complex logic (branching, data transforms, caches) add multiple tests to exercise happy-path, edge conditions, and failure cases. Keep tests colocated with the module (e.g., `app/src/test/java/...`).

Happy hacking! If you discover new recurring workflows, extend this file so the next agent benefits. 
