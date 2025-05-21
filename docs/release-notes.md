# Release Notes

### Version 0.0.59 (Released 2025-05-21)

- **Migrated to Java 17**
- Added `AltitudePath`, `VehiclePath`, and `PathPair`.
  - These classes are designed to be `Key` types used in [DistanceTrees](https://github.com/mitre-public/dist-tree)
- `YyyyMmDd` added `plus` and `minus` methods
- Added `CheckedRunnable` and `CheckedCallable` to automatically demote checked exceptions and allow using methods that
  throw Checked Exceptions to be used in standard java lambda places

### Version 0.0.58 (Released 2025-04-05)

- Added LeftMerger utility class
- Added `Time.enclosingTimeWindow(Collection<Instant> times)`
- `MapBuilder` has new convenience functions for certain `MapFeatures`
  - Multi-line Strings
  - draw circles (Supports `Collection<LatLong>`, `LatLongPath`, and `LatLong64Path`)
  - draw paths (Supports `Collection<LatLong>`, `LatLongPath`, and `LatLong64Path`)
- `ReservoirSampler` now implements the `Consumer` interface
- Patched a flawed constructor in `LatLong64Path`

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