# CLAUDE.md

## Branch Policy

- **`main` is protected.** No agent may push, commit, or create pull requests targeting `main`. Only the board (repo owner) may create PRs into `main`.
- **All agent work targets `development`.** Feature branches must be created from `development` and PRs must target `development`.
- **Direct commits to `main` are blocked** by GitHub branch protection (requires PR + review + admin enforcement).

## CI Verification (Required)

- **Never merge a PR if CI is red.** Before merging any pull request into `development`, verify that the CI workflow ("CI - Test & Build") has passed on the PR branch.
- **Build locally before pushing.** Before pushing a feature branch or creating a PR, run `gradle assembleDebug` to confirm the project compiles. Do not push code with compilation errors.
- **Run tests before creating a PR.** Run `gradle testDebugUnitTest` locally and ensure all tests pass before opening a pull request.
- **If CI fails after push,** fix the issue immediately on the same branch. Do not leave `development` in a broken state.
- **Dependency changes require build verification.** When adding, removing, or changing dependencies (in `build.gradle.kts` or `libs.versions.toml`), always verify the full build compiles and tests pass before pushing.
