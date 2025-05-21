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

- Official "how-to" documentation is [here](https://central.sonatype.org/publish/publish-portal-upload/)
  - Older, partially out-dated (but still useful) "how-to" documentation is [here](https://central.sonatype.org/publish/publish-manual/)
- The final `.pom` must be suitable for `maven central` (Developer list, SCM Urls, Project Description, etc.)
- Make build must create these resource:
    - `software-libary.jar`
    - `software-libary.pom`
    - `software-libary-source.jar`
    - `software-libary-javadoc.jar`
- **Action:** Sign all 4 artifacts with gpg 
    - **Run:** `gpg -ab commons-X.Y.Z.jar`
    - **Run:** `gpg -ab commons-X.Y.Z.pom`
    - **Run:** `gpg -ab commons-X.Y.Z-sources.jar`
    - **Run:** `gpg -ab commons-X.Y.Z-javadoc.jar`
- **Action:** Generate the MD5 checksum of all 4 artifacts
    - **Run:** `md5sum commons-X.Y.Z.jar | cut -d " " -f 1 > commons-X.Y.Z.jar.md5`
    - **Run:** `md5sum commons-X.Y.Z.pom | cut -d " " -f 1 > commons-X.Y.Z.jar.md5`
    - **Run:** `md5sum commons-X.Y.Z-sources.jar | cut -d " " -f 1 > commons-X.Y.Z-sources.jar.md5`
    - **Run:** `md5sum commons-X.Y.Z-javadoc.jar | cut -d " " -f 1 > commons-X.Y.Z-javadoc.jar.md5`
- **Action:** Generate the SHA1 checksum of all 4 artifacts
    - **Run:** `sha1sum commons-X.Y.Z.jar | cut -d " " -f 1 > commons-X.Y.Z.jar.sha1`
    - **Run:** `sha1sum commons-X.Y.Z.pom | cut -d " " -f 1 > commons-X.Y.Z.jar.sha1`
    - **Run:** `sha1sum commons-X.Y.Z-sources.jar | cut -d " " -f 1 > commons-X.Y.Z-sources.jar.sha1`
    - **Run:** `sha1sum commons-X.Y.Z-javadoc.jar | cut -d " " -f 1 > commons-X.Y.Z-javadoc.jar.sha1`
- **Action:** Bundle all the artifacts into one .zip file.
    - `mkdir -p org/mitre/commons/X.Y.Z`
    - `cp commons-X.Y.Z* org/mitre/commons/X.Y.Z`
    - create the `.zip` file 
    - Name the `.zip` whatever you like.  This is the file that gets uploaded to the portal
    - **Run:**
      `jar -cvf bundle.jar commons-X.Y.Z-javadoc.jar commons-X.Y.Z-javadoc.jar.asc commons-X.Y.Z-sources.jar commons-X.Y.Z-sources.jar.asc commons-X.Y.Z.jar commons-X.Y.Z.jar.asc commons-X.Y.Z.pom commons-X.Y.Z.pom.asc`
    - This creates the artifact bundle: `bundle.jar`
- Log into `https://central.sonatype.com/`
    - Select `Publish`
    - Upload your .zip
    - Wait for system to verify your upload. It will require jar, pom, sources, docs, md5 checksums, sha1 checksums, pgp signatures.
