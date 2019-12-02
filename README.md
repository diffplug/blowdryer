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

Blowdryer lets you centralize your build scripts and config files into a single repository, with an easy workflow for pulling those resources into various projects that use them, improving them in-place, then cycling those improvements back across the other projects.

## How to use it

First, make a public github repository ([`diffplug/blowdryer-diffplug`](https://github.com/diffplug/blowdryer-diffplug) is a good example), and push the stuff that you want to centralize into the `src/main/resources` subdirectory of that repo.

Then, in the `build.gradle` for the **root** project that you want to suck these into, do this:

```gradle
plugins {
  id 'com.diffplug.blowdryer' version '1.0.0'
}

blowdryer {
  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
  //   or 'commit', '07f588e52eb0f31e596eab0228a5df7233a98a14'
  //   or 'tree', '07f588e52eb0f31e596eab0228a5df7233a98a14'
}
```

Now, in any other `build.gradle` throughout your project you can do this:

```gradle
import com.diffplug.blowdryer.AsFile

apply from: AsFile.resource('someScript.gradle')
// or
somePlugin {
    configFile AsFile.resource('somePluginConfig.xml')
}
```

`AsFile.resource` returns a `File` which was downloaded to your system temp directory, from the `src/main/resources` folder of `acme/blowdryer-acme`, at the `v1.4.5` tag.  Only one download will ever happen for the entire machine, and it will cache it until your system temp directory is cleaned.  To force a clean, you can run `gradlew blowdryerWipeEntireCache`. (TODO - implement task)

### How it works

`AsFile.immutableUrl` (TODO, link to javadoc) is another method you can use, which returns a `File` containing the downloaded content of the given URL.  It's on you to guarantee that the URL is immutable.

When you setup the blowdryer plugin in your root project, you're telling blowdryer what URL scheme to use when resolving a call to `AsFile.resource` (TODO, link to javadoc), for example:

```java
//blowdryer {
//  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
public void github(String repoOrg, GitAnchorType anchorType, String anchor) {
  String root = "https://raw.githubusercontent.com/" + repoOrg + "/" + anchor + "/" + repoSubfolder + "/";
  AsFile.setResourcePlugin(resource -> root + resource);
}
```

## Dev workflow

To change and test scripts before you push them up to GitHub, you can do this:

```gradle
blowdryer {
  //github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
  devLocal '../path-to-local-blowdryer-acme'
}
```

The call to `devLocal` (TODO: test) means that all calls to `AsFile.resource` will skip caching and get served from that local folder's `src/main/resources` subfolder.

### `repoSubfolder`

If you want your scripts to come from a different subfolder, you can change it:

```gradle
blowdryer {
  repoSubfolder 'some/other/dir/but/why'
  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
}
```

The nice thing about the default `src/main/resources` is that if you ever want to, you can publish the repository as a plain-old jar and pull the resources from that jar rather than from a github repository.  That's currently unsupported in blowdryer, but it would be easy to add.

## Limitations

If you apply any third-party plugin inside a script plugin, you cannot "see" it in any other script, including the main one. See [gradle/gradle#4007](https://github.com/gradle/gradle/issues/4007) for details.

The workaround is to make sure that either:

- each script only uses built-in gradle plugins and classes
- any script which applies a third-party plugin is completely "self-contained", and does not need to be referenced by any other plugins
- add that plugin as a dependency of your `blowdryer` plugin.

Another possible workaround would be to implement our own `apply from` which trades gradle's encapsulation away in favor of easier interoperability.  Dunno how hard that would be, but this would be a natural place for such a hack to live.

## In the wild

Here are resource repositories in the wild:

- https://github.com/diffplug/blowdryer-diffplug

## Acknowledgements

- Maintained by [DiffPlug](https://www.diffplug.com/).
