# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Please check our [developers guide](https://gitlab.com/tokend/developers-guide)
for further information about branching and tagging conventions.

## [1.15.3] 2021-10-04

### Changed
- Replace FingerprintAuthManager with standard BiometricPrompt

### Fixed
- Incorrect compact date format output
- Redemptions causing the history not to be displayed
- Assets with faulty logos causing a balance not to be displayed
- Aggressive validation on Sign up screen

### Internal
- Refactor repositories to the latest version from Template
- Simplify `SimpleTextWatcher`

## [1.15.2] 2021-03-30

### Added
- Force `Cache-Control` header for images for offline work
- Firebase Crashlytics
- Google services config to Gradle

### Changed
- Make `SingleItemRepository` work with `Single` instead of `Maybe`
- Separated date formats, see `DateFormatters`
- Separate `CredentialsPersistence` into `WalletInfoPersistence` and `CredentialsPersistence`

### Fixed
- Picasso circle transform to avoid usage of recycled bitmaps
- Data duplication issues in paging repos

### Removed
- Outdated `BottomNavigationView` shifting hack
- Fabric Crashlytics

### Internal
- Migrate to AndroidX

## [1.15.1] 2020-04-23

### Added
- Ability to decode inverted QR codes

### Fixed
- Data display issues on asset explorer fragment
- Missing balance conversion prices when loaded from cache
- Incorrect local converted balance amounts recalculation

### Removed
- Outdated TokenD Authenticator sign in method

### Internal
- Do not load payment counterparty on details screen if it's already set
- Restructured `Navigator` inner calls
- Added error handlers composing
- Fixed possible concurrent modifications of activity request bags
- Adapted amount input screen for multiple balances per asset
- Adapted payment and withdrawal use cases for multiple balances per asset

## [1.15.0] 2020-02-21

### Added
- Ability to unlock the app in offline: balances, assets and movements are cached now
- Clear button for payment recipient input
- Binding deposit address for Coinpayments

### Changed
- Speed up payments a little bit
- Speed up Dashboard a little bit
- Improved QR codes readability

### Fixed
- Google Authenticator market URL
- Display of non-approved KYC
- Display of cancelled polls
- Empty asset polls screen when user has no balance
- Incorrect asset code on asset buy amount input screen

### Internal
- Asset model is now used in `AmountInputScreen` instead of code
- Fixed impossible build without Fabric key
- Added Room storage
- Refactored repository and cache for paged data
- Added `ObjectPersistence` interface for single-object storage and it's implementation for 
`SharedPreferences`, it is also now a cache for `SingleItemRepository`
- Fixed styling issues: now it's easy to create a dark theme
- Separated active and request KYC state repositories
- **Refactored calling activities for result: created `ActivityRequest` helper that allows
setting callback for `RESULT_OK` without overriding `onActivityResult`**
- Made `QrScannerUtil` work with `ActivityRequest`
- Moved `Navigator`-related things to `navigation` package
- **Made grand `data` package cleanup**

## [1.14.0] 2019-12-09

### Added
- Ability to sign in with local key
- Local balances update after performing financial operations (based on tx result decoding)

### Changed
- Display only active assets (assets removal support)

### Fixed
- Missing balance converted amount local update
- Crash on negative remained time (sales)

### Removed
- Sale chart tab

### Internal
- Created fragment displayer for user flow screens
- Got rid of FragmentFactory
- Changed the way of height calculation on amount screen
- Moved hide FAB scroll listener to specific class

## [1.13.0] 2019-09-25

### Added
- Auto sign in after sign up if verification isn't needed
- Conterparty emails display in history
- QR code button to the corner of the avatar
- Clipboard content suggestion on payment recipient screen
- Ability to share a sale
- Ability to initiate KYC recovery
- Ability to disable app locking in the background

### Changed
- History and balance are now updated immediately after sending a payment
- Improved look of contacts list on payment recipient screen

### Fixed
- Deposit addresses parsing in some cases
- Ability to open a new screen twice if you're a fast clicker
- Wrong font size of auto-fit labels in some cases
- Missing elevation on balances screen when there are no balances
- Not working email confirmation
- Locale change issues
- App reload on entering split-screen mode
- Typos in some messages and labels
- Content overlap on amount enter screen in some cases
- Unwanted keyboard display on unlock screen

### Removed 
- Seed-based recovery

### Internal
- Removed repository packages with only one class
- Replaced BlobManager with BlobsRepository with cache
- Removed unused small icon attribute from history-related adapters
- Removed KYC and terms URLs from URL config
- 2FA factors are now loading only on the settings screen
- Cleaned up resources
- Simplified FAB actions setup
- Reorganized KYC form hierarchy
- Renamed aswap asks repository
- Got rid of 'getAssetsToDisplay' in amount input fragment
- Create separated package for dialogs
- Cleaned up fragment instance creation methods
- Cleaned up and optimized utils for logos generation
- Refactored helper interfaces for records
- Added lazy inflate of collapsing content on balances screen to avoid framerate drop

## [1.12.0] 2019-08-20

### Added
- Support of deposit addresses with payload
- Empty view to balances screen
- End date countdown for polls
- Ukrainian and Russian translations
- Handling of poll re-vote rule

### Changed
- Reduced QR generator error correction level
- Made space in amount-asset string non-breakable
- Use account-sales endpoint to load sales
- Use V3 transactions endpoint to submit transactions
- Display active polls on the top

### Removed
- Sales filter by name :(
- Recovery seed confirmation and password recovery (temporarily)

### Fixed
- Non-working swipe refresh on asset buy screen
- Closing action menu on touch outside
- Broken `JsonCreator` because of ProGuard

## [1.11.0] 2019-07.11

### Added
- Partial support of custom asset trailing digits count
- Polls
- Direct asset buy (atomic swap)
- Forcing Android to allow cleartext communication

### Changed
- Increased percents display accuracy on distribution chart

### Fixed
- Display raw amount instead of total on balance change details screens
- Password change handling on fingerprint app unlock
- Missing keyboard resize on order creation screen
- Missing data on investment confirmation screen
- Visible balance change actions when they are disabled in config

## [1.10.0] 2019-06-04

### Added
- Send and Receive actions to Dashboard
- Highlight of selected legend entry on assets distribution chart
- Grid layout for balances and asset pairs for large-width screen
- Error logger into error handler
- Fees loading on payment amount screen
- Ability to open asset details screen from balance details screen

### Changed
- Applied clean white theme
- Redesigned balance details screen
- Redesigned cards
- Redesigned balance change details screens
- Redesigned confirmation screens
- Account ID in history is displayed truncated
- Replaced tablet navigation shadow with stroke
- Adapted Send and Withdraw amount screens for small screen size
- Made sale progress bar rounded and thicker
- Moved sale video below the progress
- Total amount is now displayed in balance changes history

### Fixed
- QR scanner screen rotation
- First click on avatar did nothing
- Default input focus on sign in screen
- Incorrect movements list width after device rotation
- Missing overall movements update after balance change
- Wrong icon for items on pending investments screen
- Asset order on withdraw and payment screens

## [1.9.0] 2019-05-31

### Added
- Ability to open detailed fee information from payment confirmation
- Balances conversion
- Balances sort by converted amount
- Eye toggle for password fields (on Sign up, Recovery and Password change)
- Display total amount on balance change details screens if there are fees
- Lock screen

### Changed
- Redesigned Dashboard
- Moved sale investing form to the separate screen
- Increased number of points to display on chart
- Removed purple hue of the text and icons
- Replaced condensed typeface with the regular one on empty views
- Updated picture on wallet empty view
- Recovery seed copy screen is now displayed before the account creation
- Updated recovery seed confirmation screen text to match web client
- Renamed 'Create balance' action on assets explorer to 'Add to balances'
- Renamed 'Offer' to 'Order' in localization
- Unified successful action result messages
- Replaced deposit address action rows with the regular buttons
- Simplified payment counterparty display: show only email, show both email and account ID on click
- Replaced offer fee with a separate investment fee for investing offers

### Removed
- White placeholder for balance asset logo

### Fixed
- Wrong chart data for some periods
- Wrong focus on offer creation screen when price is not specified
- Vertical centering of error/empty view message
- Typo in `op_no_entry` error message
- 'Are you sure...' messages grammar
- Stuck soft input after navigating to another screen through a navigation menu

## [1.8.0] 2019-05-13

### Added
- Display important asset policies on asset details screen
- Display user's avatar and account type in navigation header
- User's avatar placeholder background color to the app config
- Display volume indicators in order book
- Repository and models for asset chart data
- LRU cache to the default `RepositoryProvider` implementation

### Changed
- Redesigned sale screen
- Account ID on payment details screen is now displayed truncated
with ability to copy
- Redesigned confirmation screens (payment, withdrawal, offer)
- Redesigned limits display
- Improved fees display
- Redesigned send flow
- Redesigned withdraw flow
- Redesigned deposit screen
- Changed amount in order book to a volume (cumulative amount)
- Switched to new V3 order books API
- Refactored offer creation and confirmation
- Updated `Navigator` component structure

### Fixed
- Vertical scroll on fees screen
- Password change and recovery
- Withdrawal integration tests (added account verification)

## [1.7.0] 2019-04-17

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

[Unreleased]: https://github.com/tokend/android-client/compare/1.15.3(26)...HEAD
[1.15.3]: https://github.com/tokend/android-client/compare/1.15.2(25)...1.15.3(26)
[1.15.2]: https://github.com/tokend/android-client/compare/1.15.1(24)...1.15.2(25)
[1.15.1]: https://github.com/tokend/android-client/compare/1.15.0(23)...1.15.1(24)
[1.15.0]: https://github.com/tokend/android-client/compare/1.14.0(22)...1.15.0(23)
[1.14.0]: https://github.com/tokend/android-client/compare/1.13.0(21)...1.14.0(22)
[1.13.0]: https://github.com/tokend/android-client/compare/1.12.0(20)...1.13.0(21)
[1.12.0]: https://github.com/tokend/android-client/compare/1.11.0(16)...1.12.0(20)
[1.11.0]: https://github.com/tokend/android-client/compare/1.10.0(13)...1.11.0(16)
[1.10.0]: https://github.com/tokend/android-client/compare/1.9.0(12)...1.10.0(13)
[1.9.0]: https://github.com/tokend/android-client/compare/1.8.0(10)...1.9.0(12)
[1.8.0]: https://github.com/tokend/android-client/compare/1.7.0(8)...1.8.0(10)
[1.7.0]: https://github.com/tokend/android-client/compare/1.6.0(7)...1.7.0(8)
[1.6.0]: https://github.com/tokend/android-client/compare/1.5.1(6)...1.6.0(7)
[1.5.1]: https://github.com/tokend/android-client/compare/1.5.0(5)...1.5.1(6)
