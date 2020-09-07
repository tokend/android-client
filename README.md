# TokenD Android wallet

This is a template wallet app that provides access to any TokenD-based system. Read more about TokenD platform on <a href="http://tokend.io/" target="_blank">tokend.io</a>.

<a href='https://play.google.com/store/apps/details?id=org.tokend.template&utm_source=git&utm_campaign=git&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height=64px/></a>

## Supported features

* Account creation & recovery
* Transfers
* Deposit & withdrawal
* Investments
* Trades
* Token explorer
* Security preferences management
* Limits view
* Fees view
* Polls
* Direct asset buy

## Customization
The app configuration is placed in `app_config.gradle` file. There you can change application ID, version info, network params, colors, and features availability.

Besides the main configuration file there are also `stage_app_config.gradle` for the staging build type and `release_app_config.gradle` for the release one. There you can override particular properties from the main configuration.
You can also write a configuration for the debug build type into `dev_app_config.gradle` file which is ignored by Git.

### Network config
The configuration contains 5 fields that represent network params of a specific TokenD-based system:
 `api_url`, `storage_url`, `web_client_url`, `terms_url`, `kyc_url` and `app_host`.

The app allows user to specify a TokenD-based system to work with by scanning a QR code with network params. In this case, network params from the configuration will be used and displayed by default.

To disable system switching you have to set `network_specified_by_user` flag to false. With fixed network config you can also enable opening links from emails in the app by specifying system domain in `app_host` field and uncommenting intent filters in manifest.

### Analytics
The application is ready for Crashlytics integration. To enable it you have to create `keystore.properties` file (it is already gitignored) in `app/` directory and specify your organization API key inside as follows:
`FabricApiKey=xxx`.

To enable or disable analytics use `enable_analytics` flag. In the default configuration this flag is enabled for staging and release builds.

### Branding
In order to change the application branding you have to update following resources:

* `product_logo` in `mipmap` – logo used on the splash screen and in the navigation header
* `ic_launcher` in `mipmap` – legacy application icon
* `ic_launcher_background` and `ic_launcher_foreground` in `drawable-v24` – parts of the adaptive icon displayed on Android 7.0 and higher
* `app_name` field in `strings.xml` – displayed application name

## Testing
There are integration tests for use cases in `src/test/java/org/tokend/template/test`.
You can specify the environment to run tests on inside `Config.kt` file. 

It is recommended to use local [TokenD Developer edition](https://github.com/tokend/developer-edition)
for tests because they create a lot of users, assets, asset pairs, etc.

## Connecting to Developer edition
In order to connect the wallet to your [TokenD Developer edition](https://github.com/tokend/developer-edition) instance follow [this guide](https://mobile-qr.tokend.services/).

## Credits
⛏ <a href="https://distributedlab.com/" target="_blank">Distributed Lab</a>, 2020
