# quickPR

![Build](https://github.com/jacob-lu-mongodb/quickPR/workflows/Build/badge.svg)

<!-- Plugin description -->
Creates pull requests and pre-populates the title and description

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file
which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections.
<!-- Plugin description end -->

## Installation

Mkae sure that your Intellij's runtime JDK version is >= 11.
See https://intellij-support.jetbrains.com/hc/en-us/articles/206544879-Selecting-the-JDK-version-the-IDE-will-run-under

- Manually:

  Download the [latest release](https://github.com/jacob-lu-mongodb/quickPR/releases/latest) and
  install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
  disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
