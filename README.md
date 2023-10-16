[![Java CI with Gradle](https://github.com/mitre-public/commons/actions/workflows/ci.yml/badge.svg)](https://github.com/mitre-public/commons/actions/workflows/ci.yml)

# Introduction to this Commons Project

This project contains general purpose Java utilities for common CAASD's project needs. For example, this project
contains:

1. Literate classes for aviation concepts like: `LatLong`, `Course`, `Speed`, and `Distance`.
2. Convenient map drawing capabilities using `MapBuilder` and `MapImage` (see more [here](docs/mapping.md))
3. Useful "UUID-like" behaviors in `TimeId` and `SmallTimeId`
4. Curve fitting tools for location data in: `Position`, `KineticPosition`, `PositionRecord`, `KineticRecord`,
   and `PositionInterpolator`
5. Useful Data-structures: `HashedLinkedSequence` and  `MetricTree`
6. Convenience methods for working with flat config files and Properties objects in: `PropertyUtils`
7. Classes for streaming data processing via functional programming: `DataCleaner`, `FilteredConsumer`,
   and `CompositeConsumer`.
8. Classes for convenient and powerful Exception handling: `ExceptionHandler`, `ErrorCatchingTask`,
   and `SequentialFileWriter`
9. Classes for powerful parallelism: `Parallelizer`
10. Other general purpose utilities like: `Histogram`, `FileLineIterator`, and `SingleUseTimer`

## Documentation by Topic

* [Drawing Maps](docs/mapping.md)
* [TimeId Design Choices and Implications](docs/timeIdDesign.md)

## Users

This project is intended to assist anyone who codes in Java. However, this project was originally developed to "isolate"
general purpose Java utilities from the Aviation Risk Identification and Assessment (ARIA) codebase.

## Project Aspirations

* Literate code. Classes should be easy to use. Their behavior should be obvious.
* Well-tested code. Classes should be dependable.
* Powerful code. We want to natively support most use cases.
* As few dependencies as possible.

---

### Early Open Sourcing checklist

- [x] Reconsider groupId -- **Decision** change from `org.mitre.caasd` to `org.mitre`
    - [ ] Execute change!
- [ ] Add "CONTRIBUTION.md" rules
- [ ] Add Roadmap
    - [ ] Releasing current Java 8 version
    - [ ] Then pivoting to Java 17
    - [ ] Removing pair and triple (only in Java 17+)
    - [x] Add `LatLongPath`
- [ ] Document CI/CD pipeline & strategy for (manually) publishing to maven central
- [ ] (Re)Add code coverage

### Tasks
- [ ] Interpolator needs to work across the international date line (e.g. where longitudes have a cliff)

---

## Adopting!

All official releases are available at [Maven Central](https://central.sonatype.com/artifact/org.mitre/commons).
The latest official release is version: `0.0.54`


#### Gradle

```
dependencies {
  implementation("org.mitre:commons:0.0.54")
}
```
#### Maven

```
<dependency>
    <groupId>org.mitre</groupId>
    <artifactId>commons</artifactId>
    <version>0.054/version>
</dependency>
```

#### Accessing pre-release builds
Users who want to access the unreleased features within a SNAPSHOT build must download and build the source themselves.  The build command is:
```
./gradlew build publishToMavenLocal
```

## About Our Dependencies

- We want the `commons` library to be hassle-free to use in **YOUR** java project.
- Therefore `commons`:
    - **Cannot** encumber users by polluting their classpath.
    - Must not leak its dependencies onto downstream class paths.
    - Must not let its dependencies appear as part of the public API.
- If you notice a violation of this aspiration, please submit an issue. Dependency wrangling is a terrible time sink. We
  don't want `commons` contributing to this headache.

## Version History

### Version 0.0.54

- The initial public release of the project.

### Older Versions
- Brief release notes from versions that pre-date public-release are available [here](./docs/pre-github-version-history.md)


### Legal Statement

- **Copyright:** The contents of this project is copyright `The MITRE Corporation`. See details [here](COPYRIGHT) .
- **Open Source License:** This project is released under the Apache License Version 2.0. See details [here](LICENSE).
