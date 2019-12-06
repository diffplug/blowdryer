# <img align="left" src="logo.png"> Blowdryer: keep your gradle builds dry

<!---freshmark shields
output = [
    link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.blowdryer', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.blowdryer'),
    link(shield('Maven central', 'mavencentral', 'com.diffplug:blowdryer', 'blue'), 'https://search.maven.org/search?q=g:com.diffplug%20AND%20a:blowdryer'),
    link(image('License Apache 2.0', 'https://img.shields.io/badge/apache--2.0-blue.svg'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
    '',
    link(image('Latest', 'https://jitpack.io/v/diffplug/blowdryer.svg'), 'https://jitpack.io/#diffplug/blowdryer'),
    link(shield('Changelog', 'keepachangelog', 'yes', 'brightgreen'), 'CHANGELOG.md'),
    link(shield('Javadoc', 'javadoc', '{{stable}}', 'brightgreen'), 'https://jitpack.io/com/github/diffplug/blowdryer/latest/javadoc/'),
    link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/diffplug/blowdryer'),
    link(image('JitCI', 'https://jitci.com/gh/diffplug/blowdryer/svg'), 'https://jitci.com/gh/diffplug/blowdryer')
    ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.blowdryer-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.blowdryer)
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug%3Ablowdryer-blue.svg)](https://search.maven.org/search?q=g:com.diffplug%20AND%20a:blowdryer)
[![License Apache 2.0](https://img.shields.io/badge/apache--2.0-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Latest](https://jitpack.io/v/diffplug/blowdryer.svg)](https://jitpack.io/#diffplug/blowdryer)
[![Changelog](https://img.shields.io/badge/keepachangelog-yes-brightgreen.svg)](CHANGELOG.md)
[![Javadoc](https://img.shields.io/badge/javadoc-unreleased-brightgreen.svg)](https://jitpack.io/com/github/diffplug/blowdryer/latest/javadoc/)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/blowdryer)
[![JitCI](https://jitci.com/gh/diffplug/blowdryer/svg)](https://jitci.com/gh/diffplug/blowdryer)
<!---freshmark /shields -->

If you have multiple loosely-related gradle projects in separate repositories, then you probably have these problems:

- challenging to keep build files consistent (copy-paste doesn't scale)
- frustrating to fix the same build upgrade problems over and over in multiple repositories
- a single "master plugin" which applies plugins for you is too restrictive
  - hard to debug
  - hard to experiment and innovate

Blowdryer lets you centralize your build scripts, config files, and properties into a single repository, with an easy workflow for pulling those resources into various projects that use them, improving them in-place, then cycling those improvements back across the other projects.

## How to use it

First, make a public github repository ([`diffplug/blowdryer-diffplug`](https://github.com/diffplug/blowdryer-diffplug) is a good example), and push the stuff that you want to centralize into the `src/main/resources` subdirectory of that repo.

Then, in the `settings.gradle` for the project that you want to suck these into, do this:

```gradle
plugins {
  id 'com.diffplug.blowdryerSetup' version '1.0.0'
}

blowdryerSetup {
  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
  //   or 'commit', '07f588e52eb0f31e596eab0228a5df7233a98a14'
  //   or 'tree', '07f588e52eb0f31e596eab0228a5df7233a98a14'
}
```

Now, in any `build.gradle` throughout your project you can do this:

```gradle
apply plugin: 'com.diffplug.blowdryer'

apply from: Blowdryer.file('someScript.gradle')
somePlugin {
  configFile Blowdryer.file('somePluginConfig.xml')
  configProp Blowdryer.prop('propfile', 'key') // key from propfile.properties
}
```

`Blowdryer.file()` returns a `File` which was downloaded to your system temp directory, from the `src/main/resources` folder of `acme/blowdryer-acme`, at the `v1.4.5` tag.  Only one download will ever happen for the entire machine, and it will cache it until your system temp directory is cleaned.  To force a clean, you can run `gradlew blowdryerWipeEntireCache` TODO implement.

`Blowdryer.prop()` parses a java `.properties` file which was downloaded using `Blowdryer.file()`, and then returns the value associated with the given key.

### Chinese for "dry" (干)

If you like brevity and unicode, you can replace `Blowdryer` with `干`.  We'll use `干` throughout the rest of the readme, but you can find-replace `干` with `Blowdryer` and get the same results.

```gradle
apply plugin: 'com.diffplug.blowdryer'

apply from: 干.file('someScript.gradle')
somePlugin {
  configFile 干.file('somePluginConfig.xml')
  configProp 干.prop('propfile', 'key')
}
```

### Script plugins

When you call into a script plugin, you might want to set some configuration values first.  You can read them inside the script using `干.proj('propertyName', 'propertyDescriptionForErrorMessages')`:

```gradle
// build.gradle
apply plugin: 'com.diffplug.blowdryer'
ext.pluginPass = 'supersecret'
ext.keyFile = new File('keyFile')
apply from: 干.file('someScript.gradle')

// someScript.gradle
apply plugin: 'com.diffplug.blowdryer'
somePlugin {
  pass 干.proj('pluginPass', 'password for the keyFile')
  // if the property isn't a String, you have to specify the class you expect
  keyFile 干.proj(File.class, 'keyFile', 'location of the keyFile')
}
```

If the property isn't set, you'll get a nice error message describing what was missing, along with links to gradle's documentation on how to set properties (`gradle.properties`, env variables, `ext`, etc).

#### Script plugin gotchas

Script plugins can't `import` any classes that were loaded from a third-party plugin on the `build.gradle` classpath (see [gradle/gradle#4007](https://github.com/gradle/gradle/issues/4007) and [gradle/gradle#1262](https://github.com/gradle/gradle/issues/1262) for details).  One workaround is put these plugins into the `settings.gradle` with [`apply false`](https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl).  See the "in the wild" section below to see how other people are working around this.

## Dev workflow

To change and test scripts before you push them up to GitHub, you can do this:

```gradle
// settings.gradle
blowdryerSetup {
  //github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
  devLocal '../path-to-local-blowdryer-acme'
}
```

The call to `devLocal` means that all calls to `Blowdryer.file` will skip caching and get served from that local folder's `src/main/resources` subfolder.  This sets up the following virtuous cycle:

- easily create/improve a plugin in one project using `devLocal '../blowdryer-acme'`
- commit the script, then tag and push to `acme/blowdryer-acme`
- because the `blowdryer-acme` version is immutably pinned **per-project**, you'll never break existing builds as you make changes
- when a project opts-in to update their blowdryer tag, they get all script improvements from that timespan, and an opportunity to test that none of the changes broke their usage.  If something broke, you can fix it or just go back to an older tag.

### `repoSubfolder`

If you want your scripts to come from a different subfolder, you can change it:

```gradle
// settings.gradle
blowdryerSetup {
  repoSubfolder 'some/other/dir/but/why'
  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
}
```

The nice thing about the default `src/main/resources` is that if you ever want to, you can publish the repository as a plain-old jar and pull the resources from that jar rather than from a github repository.  That's currently unsupported in Blowdryer, but it would be easy to add.

## API Reference

You have to apply the `com.diffplug.blowdryerSetup` plugin in your `settings.gradle`.  But you don't actually have to `apply plugin: 'com.diffplug.blowdryer'` in your `build.gradle`, you can also just use these static methods (even in `settings.gradle` or inside the code of other plugins).

```gradle
// com.diffplug.blowdryer.干 is alias of com.diffplug.blowdryer.Blowdryer
static File   干.immutableUrl(String guaranteedImmutableUrl)
static File   干.file(String resource)
static String 干.prop(String propFile, String key)
static String 干.proj(Project proj, String String key, String description)
static <T> T  干.proj(Project proj, Class<T> clazz, String String key, String description)
```

If you do `apply plugin: 'com.diffplug.blowdryer'` then you get an extension object (TODO: link to code) where the project field has been filled in for you, which is why we don't pass it explicitly in the examples before this section.  If you don't apply the plugin, you can still call these static methods and pass `project` explicitly for the `proj()` methods.

### Using with Kotlin

The Gradle Kotlin DSL doesn't play well with the name-based extension object that we use in Groovy, but you can just call the static methods above.

```kotlin
// settings.gradle.kts
plugins {
  id("com.diffplug.blowdryerSetup") version "1.0.0"
}
import com.diffplug.blowdryer.BlowdryerSetup
configure<BlowdryerSetup> {
  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
}

// inside settings.gradle.kts, build.gradle.kts, or any-script.gradle.kts
import com.diffplug.blowdryer.干 // or .Blowdryer

apply(from = 干.file("script.gradle.kts"))
somePlugin {
  configFile 干.file("somePluginConfig.xml")
  configProp 干.prop("propfile", "key")
  pass       干.proj(project, "pluginPass", "password for the keyFile")
  keyFile    干.proj(project, File.class, "keyFile", "location of the keyFile")
}
```

### Setup with something besides GitHub

`Blowdryer.immutableUrl` (TODO, link to javadoc) returns a `File` containing the downloaded content of the given URL.  It's on you to guarantee that the content of that URL is immutable.

When you setup the Blowdryer plugin in your `settings.gradle`, you're telling Blowdryer what URL scheme to use when resolving a call to `Blowdryer.file` (TODO, link to javadoc), for example:

```java
//blowdryer {
//  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
public void github(String repoOrg, GitAnchorType anchorType, String anchor) {
  String root = "https://raw.githubusercontent.com/" + repoOrg + "/" + anchor + "/" + repoSubfolder + "/";
  Blowdryer.setResourcePlugin(resource -> root + resource);
}
```

If you develop support for other git hosts, please open a PR!  You can test prototypes with the code below, and clean up your mistakes with `gradlew blowdryerWipeEntireCache`.

```gradle
blowdryerSetup {
  experimental { source -> 'https://someImmutableUrlScheme/' + source }
}
```

## In the wild

Here are resource repositories in the wild:

- https://github.com/diffplug/blowdryer-diffplug

## Acknowledgements

- [Gradle](https://gradle.com/) is *so* good.
- Maintained by [DiffPlug](https://www.diffplug.com/).
