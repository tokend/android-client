# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Please check our [developers guide](https://gitlab.com/tokend/developers-guide)
for further information about branching and tagging conventions.

## [1.7.0-rc.0] 2019-04-17

### Added
- Asset pairs screen on trade
- Trades history display for particular asset pair
- Pending offers display for particular asset pair
- "Hour" asset chart scale
- Price display in pending offers history
- Offer cancellation balance change cause
- Payment counterparty email loading on the details screen
- Multi-action (Send, Receive, Deposit, Withdraw) floating button on balance history
- Bottom modal balance picker with search (check out on Send, Withdraw and Invest)
- Fees grouping by type and subtype on fees screen
- Sale photo display on sale screen

### Changed
- Redesigned balance change details screens 
- Simplified order book design
- Transformed offer creation dialog into a screen with more functionality
- Moved "Fees", "Limits" and "Sign out" navigation items to the Settings
- Changed cancellation icon from "X" to trash
- Renamed "Fund" to "Sale"
- Renamed "Token" to "Asset"
- Renamed "Withdrawal" balance change cause to "Withdrawal request"
- Renamed "Issuance/deposit requested" balance change cause to just "Issuance/deposit"
- Renamed "Offer match" balance change cause to just "Offer"
- Sale overview is now displayed on the main sale screen
- Sale metrics are now displayed below the progress bar

### Removed
- "Week" asset chart scale
- Hardcoded list of fiat currencies for amount formatting
- Sections from navigation menu

### Fixed
- "Expired session" error on network switching with the same credentials
- Unfriendly message for `op_no_entry` transaction error
- Ugly alert dialog on deposit screen on old Android versions
- Sale screen crash on old Android versions

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

[1.7.0-rc.0]: https://github.com/tokend/android-client/compare/1.6.0(7)...1.7.0-rc.0(8)
[1.6.0]: https://github.com/tokend/android-client/compare/1.5.1(6)...1.6.0(7)
[1.5.1]: https://github.com/tokend/android-client/compare/1.5.0(5)...1.5.1(6)
[Unreleased]: https://github.com/tokend/android-client/compare/1.6.0(7)...HEAD
