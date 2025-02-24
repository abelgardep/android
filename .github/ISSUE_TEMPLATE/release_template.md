---
name: Release
about: List of checklist to accomplish for the ownCloud team to finish the release process
title: "[RELEASE]"
labels: Release
assignees: ''
---

<!--
Another release for the ownCloud Android client!
For Open releases, keep the Open Release template and remove the OEM Release one
For OEM releases, keep the OEM Release template and remove the Open Release one
-->

## Open Release

### TASKS:

 - [ ] [DOC] Ping in #documentation-internal about the new release
 - [ ] [GIT] Create branch `release/M.m.p` in owncloud/android from master
 - [ ] [DEV] Update version number and name in build.gradle in owncloudApp module
 - [ ] [DEV] Update [SBOM](https://cloud.owncloud.com/f/6072870)
 - [ ] [DIS] Create a folder for the new version like `M.m.p_YYYY-MM-DD` inside the `changelog` folder
 - [ ] [DIS] Move all changelog files from the unreleased folder to the new version folder
 - [ ] [DIS] Update screenshots, if needed, in README.md
 - [ ] [DIS] Add ReleaseNotes replacing `emptyList` with `listOf` and adding inside `ReleaseNote()` with String resources
 - [ ] [QA] Design Test plan
 - [ ] [QA] Regression Test execution
 - [ ] [GIT] Create and sign tag `oc-android-M.m.p` in HEAD commit of release branch, in owncloud/android
 - [ ] [GIT] Create and sign tag `x.y.z` in HEAD commit of release branch, in owncloud/android-library
 - [ ] [DIS] Generate final bundle from signed commit in owncloud/android
 - [ ] [DIS] Upload & publish release bundle and changelog in Play Store
 - [ ] [DIS] Update screenshots and store listing, if needed, in Play Store
 - [ ] [GIT] Publish a new release in [owncloud/android](https://github.com/owncloud/android/releases)
 - [ ] [DIS] Create post in central.owncloud.org ([`Category:News + Tag:android`](https://central.owncloud.org/tags/c/news/5/android))
 - [ ] [COM] Inform `#updates` and `#marketing` in internal chat
 - [ ] [DIS] Upload release APK and bundle to internal owncloud instance
 - [ ] [GIT] Merge `release/M.m.p` branch into `stable`, in owncloud/android-library
 - [ ] [GIT] Merge `release/M.m.p` branch into `stable`, in owncloud/android
 - [ ] [GIT] Merge `release/M.m.p` branch into `master`, in owncloud/android-library
 - [ ] [GIT] Merge `release/M.m.p` branch into `master`, in owncloud/android
 - [ ] [DOC] Update documentation with new stuff by creating [issue](https://github.com/owncloud/docs-client-android/issues)


### BUGS & IMPROVEMENTS

_____

## OEM Release

### TASKS:

- [ ] [GIT] Create a new branch `release/M.m.p_oem` (optional)
- [ ] [DIS] Update release notes in app with the proper content for oem release
- [ ] [GIT] Create and sign tag `oc-android-M.m.p_oem` in HEAD commit of `release/M.m.p_oem` branch
- [ ] [DEV] Approve and merge changes in ownBrander
  - [ ] Feature 1 oB https://github.com/owncloud/ownbrander/pull/
  - [ ] Feature 2 oB https://github.com/owncloud/ownbrander/pull/
  - [ ] Update version number in ownBrander
- [ ] [OPS] Block oB button
- [ ] [OPS] Deploy oB
- [ ] [QA] Generate final APKs files from signed commit in builder machine and perform some basic checks:
    - [ ] Installation of apk/aab generated by builder machine
    - [ ] Check Feature 1 oB
    - [ ] Check Feature 2 oB
    - [ ] App update from previous version (generated in advance)
- [ ] [QA] Notify result in #ownbrander
- [ ] [OPS] Enable button
