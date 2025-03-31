# Changelog

## [1.3.3](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.3.2...v1.3.3) (2025-03-31)


### Bug Fixes

* replaced base64 decoder for URL-safe JWT decoding ([ce36bcc](https://github.com/OKDP/okdp-spark-auth-filter/commit/ce36bcc2d3ed56e3700da03259476aca9d29a6ad))

## [1.3.2](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.3.1...v1.3.2) (2025-02-04)


### Bug Fixes

* Allow ignoring refresh token storage in the cookie [#40](https://github.com/OKDP/okdp-spark-auth-filter/issues/40) ([ec3f9e2](https://github.com/OKDP/okdp-spark-auth-filter/commit/ec3f9e232c8adf726204486c3c54cc8e43865f70))

## [1.3.1](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.3.0...v1.3.1) (2025-01-22)


### Bug Fixes

* ensure java 8 compatibility by switching cache from caffeine to guava. Update for spark 2.4.8 compilation compatibility ([378deb9](https://github.com/OKDP/okdp-spark-auth-filter/commit/378deb94a2675cf17a2f257e53d9ff1296d41dc6))
* remove log4j dependencies in PersistedToken class ([77fdce9](https://github.com/OKDP/okdp-spark-auth-filter/commit/77fdce94d43623503c9977815f3e4f9aa0dcacab))

## [1.3.0](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.2.2...v1.3.0) (2024-12-11)


### Features

* retrieve information from a jwt header ([2e98420](https://github.com/OKDP/okdp-spark-auth-filter/commit/2e98420bbd48203b6cba0a69564d06bcf21e38f2))


### Bug Fixes

* Add log for debugging purpose ([808077b](https://github.com/OKDP/okdp-spark-auth-filter/commit/808077bd480724c8635a137e5f3b5e4c1ec07d2d))

## [1.2.2](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.2.1...v1.2.2) (2024-07-01)


### Bug Fixes

* Fix Base64 Encoding/Decoding issue happening on some jdk distributions (bitnami) [#24](https://github.com/OKDP/okdp-spark-auth-filter/issues/24) ([fac895c](https://github.com/OKDP/okdp-spark-auth-filter/commit/fac895ca78b65b5790ceb2257632836acc19432f))
* Fix serving behind reverse proxy [#26](https://github.com/OKDP/okdp-spark-auth-filter/issues/26) ([053f0b0](https://github.com/OKDP/okdp-spark-auth-filter/commit/053f0b0f2c301320e6889a83a053766a94f95a89))


### Miscellaneous Chores

* release 1.2.2 ([3605c5a](https://github.com/OKDP/okdp-spark-auth-filter/commit/3605c5a8ef18923e8dcf0c6e63d6c6eb65f697a4))

## [1.2.1](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.2.0...v1.2.1) (2024-06-06)


### Bug Fixes

* IDP supported scopes - Turn the exception into a warning message [#16](https://github.com/OKDP/okdp-spark-auth-filter/issues/16) ([e18881c](https://github.com/OKDP/okdp-spark-auth-filter/commit/e18881c822b0bf779c0a275e46ed1f2365c017a7))
* Re-authenticate the user against the IDP in case the offline_access is not supported [#16](https://github.com/OKDP/okdp-spark-auth-filter/issues/16) ([c7fc9f4](https://github.com/OKDP/okdp-spark-auth-filter/commit/c7fc9f4ff19cfbf81139ba265d253f212df2a9b0))


### Miscellaneous Chores

* release 1.2.1 ([c68e57c](https://github.com/OKDP/okdp-spark-auth-filter/commit/c68e57cb750a1ef4bb7c2793198bb908871dda17))

## [1.2.0](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.1.0...v1.2.0) (2024-04-11)


### Features

* Add support for Sub claim (default claim is email). Sub or email could be used as id for acls ([2d1283d](https://github.com/OKDP/okdp-spark-auth-filter/commit/2d1283dbf5416c3b73ee220ea317117e2d807c7d))

## [1.1.0](https://github.com/OKDP/okdp-spark-auth-filter/compare/v1.0.0...v1.1.0) (2024-03-18)


### Features

* Authentication - Add PKCE support for confidential clients ([97de4d9](https://github.com/OKDP/okdp-spark-auth-filter/commit/97de4d968be347abab58b462c4af250e214ff542))
* Authentication - Add PKCE support for public clients ([ed09307](https://github.com/OKDP/okdp-spark-auth-filter/commit/ed09307e9d1ed320381ff3e226ed15a63d774295))

## 1.0.0 (2024-03-14)


### Features

* Authentication - Compress the access token (4KB cookie limit) ([fec1d89](https://github.com/OKDP/okdp-spark-auth-filter/commit/fec1d89453063ea722a0c8ad9b86a427b0fd000c))
* Authentication - Encrypt the access token ([644224c](https://github.com/OKDP/okdp-spark-auth-filter/commit/644224c12dab995a9f4dc178ad37f9a9fe464da8))
* Authentication - Store the user access token in a cookie ([87567e0](https://github.com/OKDP/okdp-spark-auth-filter/commit/87567e09734ff6b2729fd0604c6463463caf658e))
* Authorization - Authorize access to spark apps based on user oidc roles/groups claims ([3b1836a](https://github.com/OKDP/okdp-spark-auth-filter/commit/3b1836af4b371eb3438f6ee08f3d97b2c8159039))


### Miscellaneous Chores

* release 1.0.0 ([2e3042b](https://github.com/OKDP/okdp-spark-auth-filter/commit/2e3042b471f26be93fda55887876a75fd1651a4c))
