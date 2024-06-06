# Changelog

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
