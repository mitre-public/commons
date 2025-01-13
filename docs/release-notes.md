# Release Notes

### Version 0.0.57 (Released 2025-01-13)
- Fixed issue with interpolating LatLong data near the international date line.

### Version 0.0.56 (Released 2024-12-26)

- Added `LatLong64` and `LatLong64Path`. These lossy compressed editions of `LatLong` and `LatLongPath` reduce the number of
  bytes required to store lat/long data by 50%.
    -  See `LatLong.compress()` and `LatLongPath.compress()`

### Version 0.0.55 (Released 2024-11-14)

- Added `LatLongPath`
- Added `CheckedConsumer` and `CheckedSupplier`
- Deprecated `Pair` and `Triple` because we modern projects should be using java records
- Now formatting project code with The [Palantir Java Formatter](https://github.com/palantir/palantir-java-format)
- Fixed issue with interpolating LatLong data near the international date line

### Version 0.0.54 (Released 2023-10-12)

- The initial public release of the project.

### Pre-public GitHub versions

- Brief release notes from versions that pre-date public-release are
  available [here](./pre-github-version-history.md)