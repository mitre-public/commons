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

### Legal Statement

- **Copyright:** The contents of this project is copyright `The MITRE Corporation`. See details [here](COPYRIGHT) .
- **Open Source License:** This project is released under the Apache License Version 2.0. See details [here](LICENSE).


### Early Open Sourcing checklist
- [DONE] Move project to github
- [DONE] Reconsider groupId -- **Decision** change from org.mitre.caasd to org.mitre
- [DONE] Delete any methods that have better alternatives in either java or guava
- Validate/change the existing package-to-class organization
  - Will probably remove caasd from package path
- Add CI/CD in github repo (CI/CD after obvious code changes)
  - Initially publish to github
  - Add on maven central cloning of full releases (no SNAPSHOTS)
- Add "CONTRIBUTION.md" rules
- Decide how "top of file license" data will go.  Do we have one repo-wide license? Prefix every file with the license?  What is the correct license?
- Add Roadmap
  - Releasing current Java 8 version.
  - The pivoting to Java 17, 
  - Removing pair and triple (only in Java 17+)
  - [DONE] Add `LatLongPath`
  - Adding new `DynamoMetricTree` (requires Java 17)
- Reduce entire git history to 1 "Initial Commit"
- Add CI/CD pipeline that publishes to maven central
- VERIFY guava doesn't leak into classpath

### Tasks
- [DONE] TranslatingConsumer needs a getter for each component
- [DONE] FilteredConsumer needs a getter for each component
- Interpolator needs to work across the international date line (e.g. where longitudes have a cliff)

### Points of Contact

- [Jon Parker](jiparker@mitre.org)
- [Alex Cramer](acramer@mitre.org)

### Library Usage

Add this to your Gradle dependency list:

```
dependencies {
  implementation("org.mitre.caasd:commons:0.0.53")
}
```

Add this to your Maven dependency list:

```
<dependency>
    <groupId>org.mitre.caasd</groupId>
    <artifactId>commons</artifactId>
    <version>0.053/version>
</dependency>
```


### Build Instructions
- On mac
  - use `./gradlew build`

### Users

This project is intended to assist anyone who codes in Java. However, this project was originally developed to "isolate"
general purpose Java utilities from the Aviation Risk Identification and Assessment (ARIA) codebase.

### Project Aspirations

* As few dependencies as possible.
* Literate code. Classes should be easy to use. Their behavior should be obvious.
* Well-tested code. Classes should be dependable.
* Powerful code. We want to natively support most use cases.

### Documentation by Topic

* [Drawing Maps](docs/mapping.md)
* [TimeId Design Choices and Implications](docs/timeIdDesign.md)

### Project Branching Conventions

* Trunk-based development. Long-lived branches are not permitted.

### Version History

##### Version 0.0.55 (In the future...)

* Great things....Java 17 adoption?
* TODO -- Add ability to augment the undecoratedImage of a MapImage (important for speed)

##### Version 0.0.54 (Last version before open sourcing)

* Added `CollectionUtils.binarySearch(List<T> data, Function<T,R>, sortValGetter, R searchKey)`
* Added `HasTime.binarySearch(List<HasTime>, Instant)`
* Added 3 search methods to `HasTime` -- they are `floor`, `ceil`, and `closest`. These methods rely on the
  new `HasTime.binarySearch` method and efficiently find an item within a time-sorted List of items.
* `PositionInterpolator` has been reworked
  * The change simplifies some use cases because `PositionRecord<T>` objects can be replaced with mere `Position`
    objects.
  * `PositionInterpolator` is no longer a generic class.
  * The class's core method now takes in a `List<Position>` and produces an `Optional<KineticPosition>` (notice no
    generic here)
  * A generic default method was added to replace the prior functionality (e.g., `List<PositionRecord<T>>`
    to `Optional<KineticRecord<T>>`)
* Added `MapFeatures.compose(Collection<MapFeature> many)` to improve support for creating maps with datasets. This
  method allows us to render a "scatter plot of circles" as one `MapFeature` rather than a `List<MapFeature>`. This can
  simplify drawing maps with multiple "layers"
* `TimeId.asBase64()` now returns an unpadded encoding (this is **breaking change** as compared to the behavior in version 0.0.50) 

##### Version 0.0.53 (Released 2022-11-28)

* Added `ParallelismDetector`
* Added `LatLong.toBytes()`, `LatLong.toBase64()`, `LatLong.fromBytes(byte[])`, and `LatLong.fromBase64Str(String)`
* TODO -- Add ability to augment the undecoratedImage of a MapImage (important for speed)

##### Version 0.0.52 (Released 2022-09-26)
* Moved "closed-source capabilities" to the new `commons-closed` project.  See `org.mitre.caasd:commons-closed:1.0.0`
* Spherical no longer uses `Doubles.constrainToRange` from Guava 21. This is a temporary fix until better Guava shading is implemented.

##### Version 0.0.51 (Released 2022-09-16)
* TDP-7280 fix floating point error for Spherical.alongTrackDistanceNM
* Bump GSON to version 2.8.9

##### Version 0.0.50 (Released 2022-08-23)
* Added 2 classes that support "uuid-like timestamps"
* The `TimeId` class is a 128-bit timestamp that also acts like a UUID.
    * This class uses 42 bits to embed a timestamp.
    * The remaining 86 bits store a pseudo-random hash that give UUID-like uniqueness behavior.
    * When generated from "scratch" no two `TimeIds` will ever have the same 128-bits
* The `SmallTimeId` class is a 64-bit timestamp that also acts like a UUID.
    * This class uses 42 bits to embed a timestamp.
    * The remaining 21 bits store an incrementing bit pattern that gives UUID-like uniqueness behavior.
    * This is a SIGNIFICANT difference from `TimeId`!
    * The "non-time bits" in a `SmallTimeId` cannot be assigned randomly without risk hash-collisions and
      non-uniqueness. Therefore, `SmallTimeIds` should be built using an `IdFactoryShard`. See the various static
      factory methods in `TimeIds`.
    * See more about the technical choices [here](docs/timeIdDesign.md)
* Added the `Acceleration` class.
    * `KineticPosition` was augmented to contain `Acceleration` data
    * `LocalPolyInterpolator` was updated to deduce `Acceleration` values for all its interpolated `KineticPosition`
      outputs
    * The goal of this work is to make `Acceleration` available as a feature for Machine Learning. We are assuming
      that *when, where, and how much* objects accelerate will be a powerful piece of information that ML models can "
      quickly grasp" to make progress during model training.
* Add ability to specify map size in pixels & map zoom level. Previously, all maps were forced to be 2 or 3 map tiles
  across. That prevented making big or small maps.

##### Version 0.0.49 (Released 2022-04-19)
* Added several classes for map drawing including: `MapImage`, `MapBuilder`, `FeatureSetBuilder`, `TileServer`
  , `TileAddress`
* Creating Maps backed by the MapBoxApi requires adding a MapBox Access Token to:
    * The environment variables (key="MAPBOX_ACCESS_TOKEN")
    * The Java System Properties (key="MAPBOX_ACCESS_TOKEN")
    * The working directory in a `mapbox.token`
* See more [here](docs/mapping.md)

##### Version 0.0.48 (Released 2022-02-14)
* Transitioned to Gradle
* Fixed bug in Spherical.arcLength
* Added more UAV designations to `AircraftTypeMapping`
* Improved `OutputSink`, this interface is now flushable and it no longer requires `JsonWritable`

##### Version 0.0.47 (Released 2022-01-24)
* Added `turnRadius` to `KineticPosition`
* `LocalPolyInterpolator` can now ignore altitude data
* A `Position` can now be defined without an altitude component
* Added a constructor and static factory methods to `PositionRecord`
* Added `TranslatingConsumer<BEFORE, AFTER>`, an adapter that enables a `Consumer<AFTER>` to masquerade as
  a `Consumer<BEFORE>` (requires a `Function` that convert a `BEFORE` to an `AFTER`)
* Added `NeighborIterator` and deprecated `MemoryIterator`
* Added `AutoCloseableIterator`

##### Version 0.0.46 (Released 2021-12-15)
* Added `Position` and `KineticPosition` to represent static location data and dynamic location data
* Added `PositionRecord<T>` and `KineticRecord<T>` to enable composing a piece of data (e.g., an instance of`<T>`) with
  fixed, or dynamic, location data.
* Added the interface `PositionInterpolator` that computes dynamic location data from a sequence of fixed location data
  samples
* Added `LocalPolyInterpolator`, an implementation of `PositionInterpolator` that uses weighted polynomial fitting under
  the hood
* Added `CurveFitters` for conveniently fitting polynomials to x-y data
* Added static factory methods to `Speed` class (e.g., `Speed.ofKnots(double)`)

##### Version 0.0.45

* Added no-arg constructors to `LatLong`, `Speed`, `Distance`, and `Course` to enable Avro serialization.
* Added `checkAllTrue` and `checkAllFalse` to `Preconditions`
* Added `checkAllMatch` to `Preconditions`

##### Version 0.0.44

* Migrated to JUnit 5
* SonarQube quality improvements
* Added map-based constructor to `ImmutableConfig`

##### Version 0.0.43

* Added `ImmutableConfig` and deprecated `QuickProperties`

##### version 0.0.42

* Added `HasTime.timeAsEpochMs()`

##### Version 0.0.41

* Added functionality to `HasTime` and `Time`
* Fixed issue in `SequentialFileWriter` where the full stack trace was not written to the target file
* Added `YyyyMdDay.of(String date)`
* `GzFileSink` and `JsonGzFileSink` are now public.

##### Version 0.0.40

* Reinstatied `TimeWindow.length()` (which is deprecated in favor of `TimeWindow.duration()`)

##### Version 0.0.39

* Added `GzFileSink` and `JsonGzFileSink` these two consumers send streams of data to GZIP files.
* Added `FileUtils.buildGzWriter()`
* Added convenience functions to `FileUtils` including: `fileLines(File f)`, `gzFileLines(File f)`
  , `fileLineIter(File f)`, and `gzFileLineIter(File f)`
* Updated `FileLineIterator` to directly support GZIP files
* Added `dayOfWeek()`, `asInstant()`, `tomorrow()`, and `yesterday()` to `YyyyMdDd`.

##### Version 0.0.38

* Added `YyyyMdDd` to help bucket time-stamped data by date
* Added `Triple`, `zip(Iterable a, Iterable b)`, and `zip(Iterable a, Iterable b, Iterable c)`
* Improved toString method of `Pair`
* Added `TimeWindow.pad(Duration padding)`
* Migrated `Interval` from CDA (which is very similar to TimeWindow but has a different "contains" behavior at the end)

##### Version 0.0.37

* Added `FileSink` to the `org.mitre.caasd.commons.out` package. This is a `Consumer<String>`
* Added `Partitioners` utility class

##### Version 0.0.36

* `OutputSink` and its implementations are now all Generic classes

##### Version 0.0.35

* Added the `org.mitre.caasd.commons.out` package
* This package contains `JsonWritable`, `OutputSink`, `JsonFileSink`, and `PrintStreamSink`

##### Version 0.0.34

* Shortened names of methods in `PropertyUtils`
* Added `tokenizeAndValidate` method to `PropertyUtils`

##### Version 0.0.33

* Add Consolidated Wake Turbulence aircraft wake categories to `AircraftType`

##### Version 0.0.32

* Add multi-directory parsing capability in `SwimAsdexParser` and `SwimMessageIterator`
* Handle null Facility strings
* Add `Exceptions.stackTraceOf(Throwable)`

##### Version 0.0.31

* `SwimAsdexParser` and `SwimMessageIterator` were made public

##### Version 0.0.30

* The abstract class `NopRadarHit` will now reject any input that does not contain a proper latitude or longitude value

##### Version 0.0.29

* Added Facility `AZA`

##### Version 0.0.28

* Fixed minor flaw with Aircraft meta data (base version of Apache Helicopter was not tagged as a Military aircraft)

##### Version 0.0.27

* Added Various `verifyPropertyIsSet` and `verifyPropertiesAreSet` nethods to `PropertyUtils`
* Added `TwoWayConsumer` that combines a Predicate and two Consumers
* Added an implementation of `EditDistance`

##### Version 0.0.26

* Added Support for NOP-STARS Traffic Count (TC) and Instrument Approach (IA) messages
* Various improvements to `HasPosition`, `LatLong`, and `Spherical`

##### Version 0.0.25

* Removed the the commons-lang3 dependency (to easy FAA code audits)

##### Version 0.0.24

* Added the `HasTime` interface
* Added `AircraftType`, `AircraftModel`, `AircraftClass`, and several other aircraft taxonomy related classes

##### Version 0.0.23

* Added the `MetricSet` Data structure. A `MetricSet` is similar to `MetricTree` but it mimics a `Set` instead of
  a `Map`
* Fixed a Bug in `MetricTree` that can lead to StackOverflowErrors when very large datasets were searched
* Added NOP Facilities `ZAN`, `ZUA`, `ZHN`

##### Version 0.0.22

* Added `LiterateDuration theDurationBtw(Instant... times)` for more readable if statements
* Replaced `Aggregator` interface with various `Collections` that implement `Consumer` (e.g. `ConsumingArrayList`)

##### Version 0.0.21

* Fixed package naming issue with FilteredIterator

##### Version 0.0.20

* Attempted to add FilteredIterator
* Added `Speed.ZERO`, `Distance.ZERO`, and `Course.ZERO`

##### Version 0.0.19

* Added Support for ASDEX parsing
* Added a new methods to `Histogram`, `Speed`, `Distance`, and `LatLong`
* Added the `org.mitre.caasd.commons.geometry` package. This is early support for GeoSpatial Geometry querying

##### Versions 0.0.16, 0.0.17, and 0.0.18

* Added several static one-liner convenience methods to `PropertyUtils`. These methods make it easy to extract content
  from a Property file while simultaneously validating the content of the Property file. This should provide most of the
  benefit of the QuickProperties class without requiring a custom extension of the QuickProperties class.
* Added several methods like `getRequiredInt`, `getRequiredDouble`, `getRequiredBoolean` to the `PropertyUtils` class.
* Added several methods like `getOptionalInt`, `getOptionalInt`, and `getOptionalProperty` to the `PropertyUtils` class.

##### Version 0.0.15

* Added `Preconditions` class that is very similar to Guava's Preconditions class
* Added `CombinedPredicate` to aide quickly and easily combining Predicates while also maintaining access to the "
  components"

##### Version 0.0.14

* Simplified the `ExceptionHandler` interface, `handle(Exception)` is now a default method rather than a member of the
  interface. Consequently, implementing custom ExceptionHandlers now requires less code.
* Added `BasicExceptionHandler` to aide writing unit tests that verify Warnings and Exception occur and are handled
  correctly.

##### Version 0.0.13

* `Speed` and `Distance` now include String parsing methods that reflect unit types
* Added a `DailySequentialFileWriter` class

##### Version 0.0.12

* Improved the implementation of Speed. Two Speed object will now be equal if they are perfect multiples of each other (
  i.e. twice as much distance in twice as much time)
* Added a `FeetPerMinute` unit to Speed.
* Made it easier to compute `Speed`, `Duration`, or `Distance` when you have two of the three.

##### Version 0.0.11

* Added the `MetricTree` data structure.
* Added the `Course` class.

##### Version 0.0.10

* Added the `Translator` interface.

##### Version 0.0.9

* Minor improvements to `FastLinearApproximation`

##### Version 0.0.8

* Added convenience classes Dataset and XyPoint.
* Added the DataSplitter interface (and several default methods) to provide a "common way" to partition datasets.
* Added several implementation of DataSplitter including: PiecewiseLinearSplitter, QuasiOptimalSplitter, and
  VisvalingamSplitter
* Added an implementation of Visvalingam's algorithm. This is an algorithm that simplifies 2-d datasets by removing data
  points that can be removed without dramatically impacting the line that connects all the remaining points.
* Added the org.mitre.caasd.commons.collect package
* Added HashLinkedSequence to the new collection package. This data structure combines aspects of HashSet and LinkedList

##### Version 0.0.7

* Added MemoryIterator. This class makes it easy to access two consecutive elements in an Iteration.
* Added the org.mitre.caasd.commons.math package.
* Added the Vector class. This class enables some very basic linear algebra

##### Version 0.0.6

* The static method FileUtils.getProperties(File) now rejects flat files that would set the same property twice.
* QuickProperties constructors now reject flat files that would set the same property twice.

These changes were implemented to reduce the likelihood of making subtle errors when using Properties files.

##### Version 0.0.5

* Added Distance.mean
* Added Distance.sum

##### Version 0.0.4

* Renamed ErrorHandler to ExceptionHandler
* Renamed ErrorCatchingCleaner to ExceptionCatchingCleaner
* Added the ErrorCatchingTask.ErrorHandlingPolicy interface

##### Version 0.0.3

* First non-snapshot release

### Known Issues

1. Histogram is an immutable class. This makes it difficult to use in a streaming environment.

### Possible Future Development Goals

* Fix all Known Issues
