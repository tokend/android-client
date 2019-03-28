# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Please check our [developers guide](https://gitlab.com/tokend/developers-guide)
for further information about branching and tagging conventions.

## [1.6.0] 2019-03-28

### Added
- Labels for operations in history
- Sale cancellation balance change cause
- Asset pair update balance change cause
- Ability to select text on QR share screen
- `isInvestment` property for `BalanceChangeCause.Offer`
- 

### Changed
- Default environment to `demo.tokend.io`
- Default fragment ID on `MainActivity` to constant value
- Format for URLs in config, no more string in string required
- Methods for effect name and amount display are now available for all
 `BalanceChangeDetailsActivity` inheritors, were in `UnknownDetailsActivity` 
- Icons in history

### Removed
- Favorites functionality for sales

### Fixed 
- Wrong fee direction on balance change details screen for `Unlocked` effect

## [1.5.1] 2019-03-06

### Fixed

- Error on sign in when user has balances with unknown asset details

[1.6.0]: https://github.com/tokend/android-client/compare/1.5.1(6)...1.6.0(7)
[1.5.1]: https://github.com/tokend/android-client/compare/1.5.0(5)...1.5.1(6)
[Unreleased]: https://github.com/tokend/android-client/compare/1.6.0(7)...HEAD
