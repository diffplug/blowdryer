# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- New `BlowdryerSetup.setPluginsBlockTo` for setting plugin versions ([docs](https://github.com/diffplug/blowdryer#plugin-versions)). ([#32](https://github.com/diffplug/blowdryer/pull/32) implements [#10](https://github.com/diffplug/blowdryer/issues/10))
### Fixed
- Fix `BlowdryerSetup.localJar` on Windows. ([#31](https://github.com/diffplug/blowdryer/pull/31))

## [1.6.0] - 2022-05-20
### Added
- Add retry on (gitlab) rateLimit ([#30](https://github.com/diffplug/blowdryer/pull/30))

## [1.5.1] - 2022-01-31
### Fixed
- Fix `StackOverflowError` on `干.projOptional`

## [1.5.0] - 2022-01-31
### Added
- `干.projOptional('JRE_TARGET', 'Sets the target JRE')` which returns null if it's not present. ([#28](https://github.com/diffplug/blowdryer/pull/28)).

## [1.4.1] - 2021-09-03
### Fixed
- No longer breaks the Gradle configuration cache ([#26](https://github.com/diffplug/blowdryer/pull/26)).

## [1.4.0] - 2021-06-23
### Added
- `干.file('blah.foo')` now preserves `.foo` extension in the returned file ([#23](https://github.com/diffplug/blowdryer/pull/23)).
  - also, `干.immutableUrl(String url)` can take an optional second argument for specifying the file extension, e.g.
    - `干.immutableUrl('https://foo.org/?file=blah.foo&rev=7')` returns a file which ends in `.foo-rev-7`
    - `干.immutableUrl('https://foo.org/?file=blah.foo&rev=7', '.foo')` returns a file which ends in `.foo`

## [1.3.0] - 2021-06-23
### Added
- Support for Bitbucket Cloud and Server ([#23](https://github.com/diffplug/blowdryer/pull/23)).

## [1.2.1] - 2021-06-01
### Fixed
- `repoSubfolder` doesn't do anything in `localJar` mode, so setting `repoSubfolder` ought to be an error, and now it is ([#22](https://github.com/diffplug/blowdryer/pull/22)).

## [1.2.0] - 2021-05-30
### Added
- Support for local JAR file ([#20](https://github.com/diffplug/blowdryer/pull/20)).

## [1.1.1] - 2021-02-12
### Fixed
- Occasionally a file would be deleted from temp storage while a long-lived gradle daemon kept it in cache ([#11](https://github.com/diffplug/blowdryer/pull/18)).

## [1.1.0] - 2021-02-12
### Added
- Support for GitLab, self-hosted and `gitlab.com` ([#18](https://github.com/diffplug/blowdryer/pull/18)).
- Support for private GitHub and GitLab script repositories via auth tokens ([#18](https://github.com/diffplug/blowdryer/pull/18)).

## [1.0.0] - 2020-01-09
Same as `0.2.0`, just committing to API back-compat from here.

## [0.2.0] - 2020-01-09
### Added
- Support for Gradle pre-4.9 (task configuration avoidance).

## [0.1.1] - 2019-12-10
### Fixed
- Minor javadoc improvements.

## [0.1.0] - 2019-12-08
First release!
