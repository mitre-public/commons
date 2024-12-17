# Publishing to Maven Central

This document contains notes on how to manually publish releases to maven central. This official release process is
intentionally un-automated to ensure releases are always highly intentional actions.

## Pre-Requisite Checklist

- [Setup GPG](https://central.sonatype.org/publish/requirements/gpg/) so you can sign your artifacts
    - [Official documentation](https://central.sonatype.org/publish/requirements/gpg/)
    - [Unofficial documentation](./Setup_GPG.md)

- Project is built locally using a full release version (i.e., not a SNAPSHOT release)
    - Use: `./gradlew build publishToMavenLocal`

## Publishing

**The Goal: Manually create an "asset bundle" we can upload to maven central**

- Official "how-to" documentation is [here](https://central.sonatype.org/publish/publish-manual/)
- The final `bundle.jar` must be suitable for `maven central` (Developer list, SCM Urls, Project Description, etc.)
- Make build must create these resource:
    - `software-libary.jar`
    - `software-libary.pom`
    - `software-libary-source.jar`
    - `software-libary-javadoc.jar`
- **Action:** Sign all artifacts with gpg
    - **Run:** `gpg -ab commons-X.Y.Z.jar`
    - **Run:** `gpg -ab commons-X.Y.Z.pom`
    - **Run:** `gpg -ab commons-X.Y.Z-sources.jar`
    - **Run:** `gpg -ab commons-X.Y.Z-javadoc.jar`
- **Action:** Bundle all the artifacts into one jar.
    - **Run:**
      `jar -cvf bundle.jar commons-X.Y.Z-javadoc.jar commons-X.Y.Z-javadoc.jar.asc commons-X.Y.Z-sources.jar commons-X.Y.Z-sources.jar.asc commons-X.Y.Z.jar commons-X.Y.Z.jar.asc commons-X.Y.Z.pom commons-X.Y.Z.pom.asc`
    - This creates the artifact bundle: `bundle.jar`
- Log into `oss.sonatype.org`
    - Select `Staging Upload` and provide your `bundle.jar`
        - **ALERT:** Sometimes this upload produces an "upload failed" dialog even when the upload was successful
    - Select `Staging Repositories` and find the bundle you just uploaded. The artifact repo should list all the checks
      the bundle went through. 
