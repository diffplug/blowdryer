# <img align="left" src="logo.png"> Blowdryer: keep your gradle builds dry

<!---freshmark shields
output = [
    link(shield('Gradle plugin', 'gradle plugin', 'com.diffplug.blowdryer', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.blowdryer'),
    link(shield('Changelog', 'changelog', versionLast, 'blue'), 'CHANGELOG.md'),
    link(shield('Maven central', 'mavencentral', 'here', 'blue'), 'https://search.maven.org/classic/#search%7Cgav%7C1%7Cg%3A%22com.diffplug%22%20AND%20a%3A%22blowdryer%22'),
    link(shield('Javadoc', 'javadoc', 'here', 'blue'), 'https://javadoc.io/doc/com.diffplug/blowdryer/{{versionLast}}/index.html'),
].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/gradle_plugin-com.diffplug.blowdryer-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.blowdryer)
[![Changelog](https://img.shields.io/badge/changelog-1.7.0-blue.svg)](CHANGELOG.md)
[![Maven central](https://img.shields.io/badge/mavencentral-here-blue.svg)](https://search.maven.org/classic/#search%7Cgav%7C1%7Cg%3A%22com.diffplug%22%20AND%20a%3A%22blowdryer%22)
[![Javadoc](https://img.shields.io/badge/javadoc-here-blue.svg)](https://javadoc.io/doc/com.diffplug/blowdryer/1.7.0/index.html)
<!---freshmark /shields -->

If you have multiple loosely-related gradle projects in separate repositories, then you probably have these problems:

- challenging to keep build files consistent (copy-paste doesn't scale)
- frustrating to fix the same build upgrade problems over and over in multiple repositories
- a single "master plugin" which applies plugins for you is too restrictive
  - hard to debug
  - hard to experiment and innovate

Blowdryer lets you centralize your build scripts, config files, and properties into a single repository, with an easy workflow for pulling those resources into various projects that use them, improving them in-place, then cycling those improvements back across the other projects.

<!---freshmark version
output = prefixDelimiterReplace(input, "id 'com.diffplug.blowdryerSetup' version '", "'", versionLast)
output = prefixDelimiterReplace(output, 'id("com.diffplug.blowdryerSetup") version "', '"', versionLast)
output = prefixDelimiterReplace(output, 'https://javadoc.io/static/com.diffplug/blowdryer/', '/', versionLast)
output = prefixDelimiterReplace(output, "'com.diffplug:blowdryer:", "'", versionLast)
-->

## How to use it

First, make a public github repository ([`diffplug/blowdryer-diffplug`](https://github.com/diffplug/blowdryer-diffplug) is a good example), and push the stuff that you want to centralize into the `src/main/resources` subdirectory of that repo.

Then, in the `settings.gradle` for the project that you want to suck these into, do this:

```gradle
plugins {
  id 'com.diffplug.blowdryerSetup' version '1.7.0'
}

blowdryerSetup {
  github('acme/blowdryer-acme', 'tag', 'v1.4.5')
  //                         or 'commit', '07f588e52eb0f31e596eab0228a5df7233a98a14'
  //                         or 'tree',   'a5df7233a98a1407f588e52eb0f31e596eab0228'

  // or gitlab('acme/blowdryer-acme', 'tag', 'v1.4.5').authToken('abc123').customDomainHttp('acme.org')
  // or bitbucket('acme/blowdryer-acme', 'tag', 'v1.4.5').authToken('abc123').customDomainHttps('acme.org')
}
```
* Reference on how to create [application password](https://support.atlassian.com/bitbucket-cloud/docs/app-passwords/)
for Bitbucket Cloud private repo access.<br/>
* Reference on how to create [personal access token](https://confluence.atlassian.com/bitbucketserver/personal-access-tokens-939515499.html)
for Bitbucket Server private repo access.

Now, in *only* your root `build.gradle`, do this: `apply plugin: 'com.diffplug.blowdryer'`.  Now, in any project throughout your gradle build (including subprojects), you can do this:

```gradle
apply from: Blowdryer.file('someScript.gradle')
somePlugin {
  configFile Blowdryer.file('somePluginConfig.xml')
  configProp Blowdryer.prop('propfile', 'key') // key from propfile.properties
}
```

`Blowdryer.file()` returns a `File` which was downloaded to your system temp directory, from the `src/main/resources` folder of `acme/blowdryer-acme`, at the `v1.4.5` tag.  Only one download will ever happen for the entire machine, and it will cache it until your system temp directory is cleaned.  To force a clean, you can run `gradlew blowdryerWipeEntireCache`.

`Blowdryer.prop()` parses a java `.properties` file which was downloaded using `Blowdryer.file()`, and then returns the value associated with the given key.

### Chinese for "dry" (干)

If you like brevity and unicode, you can replace `Blowdryer` with `干`.  We'll use `干` throughout the rest of the readme, but you can find-replace `干` with `Blowdryer` and get the same results.

```gradle
apply from: 干.file('someScript.gradle')
somePlugin {
  configFile 干.file('somePluginConfig.xml')
  configProp 干.prop('propfile', 'key')
}
```

### Script plugins

When you call into a script plugin, you might want to set some configuration values first.  You can read them inside the script using `干.proj('propertyName', 'property description for error message')`:

```gradle
// build.gradle
ext.pluginPass = 'supersecret'
ext.keyFile = new File('keyFile')
apply from: 干.file('someScript.gradle')

// someScript.gradle
somePlugin {
  pass 干.proj('pluginPass', 'password for the keyFile')
  // if the property isn't a String, you have to specify the class you expect
  keyFile 干.proj(File.class, 'keyFile', 'location of the keyFile')
}
```

If the property isn't set, you'll get a nice error message describing what was missing, along with links to gradle's documentation on how to set properties (`gradle.properties`, env variables, `ext`, etc).

#### Script plugin gotchas

Script plugins can't `import` any classes that were loaded from a third-party plugin on the `build.gradle` classpath. There is an easy workaround, which is to declare all plugins and their versions in the `settings.gradle` file. Blowdryer includes a mechanism for centralizing plugins and their versions, see [plugin versions](#plugin-versions) below.

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

The nice thing about the default `src/main/resources` is that if you ever want to, you can package the files into a plain-old jar and pull the resources from that jar rather than from a github repository.

### Packaging as jar

```gradle
// settings.gradle
blowdryerSetup {
  localJar(file('/absolute/path/to/dependency.jar'))
}
```

To pull this jar from a maven repository, see [#21](https://github.com/diffplug/blowdryer/issues/21).

## Plugin versions

We recommend that your `settings.gradle` should look like this:

```gradle
plugins {
  id 'com.diffplug.blowdryerSetup' version '1.7.0'
  id 'acme.java' version '1.0.0' apply false
  id 'acme.kotlin' version '2.0.0' apply false
}
blowdryerSetup {
  github('acme/blowdryer-acme', 'tag', 'v1.4.5')
  setPluginsBlockTo {
    it.file('plugin.versions')
  }
}
```

First note that every plugin has `apply false` except for `com.diffplug.blowdryerSetup`. That is on purpose. We need to apply `blowdryerSetup` so that we can use the `blowdryerSetup {}` block, and we need to do `apply false` on the other plugins because we're just putting them on the classpath, not actually using them (yet).

The second thing to note is `setPluginsBlockTo { it.file('plugin.versions') }`. That means that if you go to `github.com/acme/blowdryer-acme` and then open the `v1.4.5` tab and then go into the `src/main/resources` folder, you will find a file called `plugin.versions`. And the content of that file will be

```gradle
  id 'com.diffplug.blowdryerSetup' version '1.7.0'
  id 'acme.java' version '1.0.0' apply false
  id 'acme.kotlin' version '2.0.0' apply false
```

Blowdryer is using the same immutable file mechanism described earlier, but this time it's using it to set just that one section of your `settings.gradle` using a workflow very similar to the [`spotlessCheck` / `spotlessApply` idea](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md).

### Updating plugin versions

The workflow goes like this:

1. Enter `devLocal` mode (demonstrated [above](#dev-workflow))
2. Update the `plugin.versions` file
3. When you try to run your build, you will get an error
  - > settings.gradle plugins block has the wrong content. Add -DsetPluginVersions to overwrite
4. Add `-DsetPluginVersions` to your command line
5. You'll get another error
 - > settings.gradle plugins block was written successfully. Plugin versions have been updated, try again.
6. Now the plugins block will be up-to-date and your next build will succeed

### Tweaking the `plugin.versions`

It doesn't *have* to be called `plugin.versions`, it's just using the `干.file` mechanism and sticking that file in. So you could have `plugin-java.versions` and `plugin-kotlin.versions`. Also, you have other methods you can call:

```gradle
setPluginsBlockTo {
  it.file('plugin.versions')
  it.file('kotlin-extras.versions')
  it.add("  id 'special-plugin-for-just-this-project' version '1.0.0'")
  it.remove("   id 'acme.java' version '1.0.0' apply false")
  it.replace('1.7.20', '1.8.0') // update Kotlin version but only for this build
}
```

### Compared to version catalogs

Recent versions of Gradle shipped a flexible [version catalog](https://docs.gradle.org/current/userguide/platforms.html) feature. You can use that in combination with blowdryer's `setPluginsBlockTo`. The problem is that every plugin you use throughout the build still has to be declared in the `settings.gradle` with `apply false`. Just having the version in the catalog isn't enough. See [script plugin gotchas](#script-plugin-gotchas) above for the gory classloader details.

Disappointingly, you can't use `libs.versions.toml` inside the `settings.gradle` file, which is exactly the place that we need it.

## API Reference

You have to apply the `com.diffplug.blowdryerSetup` plugin in your `settings.gradle`.  But you don't actually have to `apply plugin: 'com.diffplug.blowdryer'` in your `build.gradle`, you can also just use these static methods (even in `settings.gradle` or inside the code of other plugins).

```gradle
// com.diffplug.blowdryer.干 is alias of com.diffplug.blowdryer.Blowdryer
static File   干.file(String resource)
static String 干.prop(String propFile, String key)
static String 干.proj(Project proj, String String key, String description)
static <T> T  干.proj(Project proj, Class<T> clazz, String String key, String description)
static File   干.immutableUrl(String guaranteedImmutableUrl)
static File   干.immutableUrl(String guaranteedImmutableUrl, String fileSuffix)
  // 干.immutableUrl('https://foo.org/?file=blah.foo&rev=7') returns a file which ends in `.foo-rev-7`
  // 干.immutableUrl('https://foo.org/?file=blah.foo&rev=7', '.foo') returns a file which ends in `.foo`
```

- [javadoc `Blowdryer`](https://javadoc.io/doc/com.diffplug/blowdryer/1.7.0/com/diffplug/blowdryer/Blowdryer.html)
- [javadoc `BlowdryerSetup`](https://javadoc.io/doc/com.diffplug/blowdryer/1.7.0/com/diffplug/blowdryer/BlowdryerSetup.html)
- [javadoc `BlowdryerSetup.PluginsBlock`](https://javadoc.io/doc/com.diffplug/blowdryer/latest/com/diffplug/blowdryer/BlowdryerSetup.PluginsBlock.html)

If you do `apply plugin: 'com.diffplug.blowdryer'` then every project gets an extension object ([code](https://github.com/diffplug/blowdryer/blob/master/src/main/java/com/diffplug/blowdryer/BlowdryerPlugin.java)) where the project field has been filled in for you, which is why we don't pass it explicitly in the examples before this section.  If you don't apply the plugin, you can still call these static methods and pass `project` explicitly for the `proj()` methods.

### Using with Kotlin

The Gradle Kotlin DSL doesn't play well with the name-based extension object that we use in Groovy, but you can just call the static methods above.

```kotlin
// settings.gradle.kts
plugins {
  id("com.diffplug.blowdryerSetup") version "1.7.0"
}
import com.diffplug.blowdryer.BlowdryerSetup
import com.diffplug.blowdryer.BlowdryerSetup.GitAnchorType
configure<BlowdryerSetup> {
  github("acme/blowdryer-acme", GitAnchorType.TAG, "v1.4.5")
  //devLocal("../path-to-local-blowdryer-acme")
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

<a name="setup-with-something-besides-github"></a>

### Other packaging options

[`Blowdryer.immutableUrl`](https://javadoc.io/static/com.diffplug/blowdryer/1.7.0/com/diffplug/blowdryer/Blowdryer.html#immutableUrl-java.lang.String-) returns a `File` containing the downloaded content of the given URL.  It's on you to guarantee that the content of that URL is immutable.

When you setup the Blowdryer plugin in your `settings.gradle`, you're telling Blowdryer what URL scheme to use when resolving a call to [`Blowdryer.file`](https://javadoc.io/static/com.diffplug/blowdryer/1.7.0/com/diffplug/blowdryer/Blowdryer.html#file-java.lang.String-), for example:

```java
//blowdryer {
//  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
public GitHub github(String repoOrg, GitAnchorType anchorType, String anchor) {
  String root = "https://raw.githubusercontent.com/" + repoOrg + "/" + anchor + "/" + repoSubfolder + "/";
  Blowdryer.setResourcePlugin(resource -> root + resource);
  return <fluent_configurator_for_optional_auth_token>;
}
```

If you develop support for other git hosts, please open a PR!  You can test prototypes with the code below, and clean up your mistakes with `gradlew blowdryerWipeEntireCache`.

```gradle
blowdryerSetup {
  experimental { source -> 'https://someImmutableUrlScheme/' + source }
}
```

## In the wild

Here are resource repositories in the wild (PRs welcome for others!)

- https://github.com/diffplug/blowdryer-diffplug
- https://github.com/mytakedotorg/blowdryer-mtdo

## Blowdryer for [gulp](https://gulpjs.com/), etc.

It would be handy to have something like this for other script-based build systems.  It would be great to standardize on `干`, feel free to name your project `blowdryer-foo`.  If you find or build one, whatever names it chooses, let us know with an issue, and we'll link to it here!

## Requirements

Requires Java 8+ and Graadle 6.8+.

## Acknowledgements

<!---freshmark /version -->

- Thanks to [Volker Gropp](https://github.com/vgropp) for implementing [GitLab support](https://github.com/diffplug/blowdryer/pull/15) and [authToken support for GitHub and GitLab](https://github.com/diffplug/blowdryer/pull/18).
- Thanks to [Sergey Sklarov](https://github.com/bHacklv) for implementing [Bitbucket support](https://github.com/diffplug/blowdryer/pull/23).
- Thanks to [Chris Serra](https://github.com/chris-serra) for implementing [local jar support](https://github.com/diffplug/blowdryer/pull/20).
- Thanks to [Zac Sweers](https://github.com/ZacSweers) for [sparking](https://github.com/diffplug/spotless/pull/488) the idea for lightweight publishing of immutable scripts.
- [Gradle](https://gradle.com/) is *so* good.
- Maintained by [DiffPlug](https://www.diffplug.com/).
