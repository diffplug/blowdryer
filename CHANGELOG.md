# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2021-05-30
### Added
- Support for local JAR file ([#20](https://github.com/diffplug/blowdryer/pull/20))

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
