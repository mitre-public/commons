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

- [ ] Add "CONTRIBUTION.md" rules
- [ ] Add Roadmap
    - [ ] Then pivoting to Java 17
    - [ ] Removing pair and triple (only in Java 17+)
- [ ] (Re)Add code coverage

---

## Adopting!

All official releases are available at [Maven Central](https://central.sonatype.com/artifact/org.mitre/commons).
The latest official release is version: `0.0.58`

#### Gradle

```
dependencies {
  implementation("org.mitre:commons:0.0.58")
}
```

#### Maven

```
<dependency>
    <groupId>org.mitre</groupId>
    <artifactId>commons</artifactId>
    <version>0.058</version>
</dependency>
```

#### Accessing pre-release builds

Users who want to access the unreleased features within a SNAPSHOT build must:
1. **Clone:** this repo with: `git clone git@github.com:mitre-public/commons.git`
2. **Build:** this project with: `./gradlew build publishToMavenLocal`


---

## Contributing

Contributions are welcomed and encouraged. But please keep in mind the project's goals and our philosophy on
dependencies.

### Code Formatting

To keep source code formatting consistent this project uses:

- The [spotless](https://github.com/diffplug/spotless) build plugin
    - This plugin enforces a global code format during the build step (e.g. incorrect formatting breaks the build).
    - Auto-format your code using: `./gradlew :spotlessApply`
- A [checkstyle.xml](docs%2Fcode-style%2Fcheckstyle.xml) style sheet.
    - The spotless plugin has priority,
    - The goal of the style sheet is to configure your IDE so the spotless plugin and your IDE mostly agree on
      formatting

### About Dependencies

- We want the `commons` library to be hassle-free to use in **YOUR** java project.
- Therefore `commons`:
    - **Cannot** encumber users by polluting their classpath.
    - Must not leak its dependencies onto downstream class paths.
    - Must not let its dependencies appear as part of the public API.
- If you notice a violation of this aspiration, please submit an issue. Dependency wrangling is a terrible time sink. We
  don't want `commons` contributing to this headache.

---

## Release Notes

See [here](./docs/release-notes.md) for a summary of the changes and features included in each release. 

---

## Legal Statements

- **Copyright:** The contents of this project is copyright `The MITRE Corporation`. See details [here](COPYRIGHT) .
- **Open Source License:** This project is released under the Apache License Version 2.0. See details [here](LICENSE).

---

## Publishing Official Releases Maven Central

See [here](./docs/publish-to-maven-central.md) for notes on "how to publish official releases".
