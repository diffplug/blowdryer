# Blowdryer: keep your gradle builds DRY

The pain points of maintaining multiple loosely-related gradle projects in separate repositories are:

- challenging to keep their build files consistent (copy-paste doesn't scale)
- frustrating to fix the same build upgrade problems over and over in multiple repositories
- a single "master plugin" which applies plugins for you is too restrictive
  - hard to debug
  - hard to experiment and innovate

Blowdryer helps by providing structure, performance, and workflow for applying a coherent set of config files and [script plugins](https://docs.gradle.org/current/userguide/plugins.html#sec:script_plugins) (`apply from: 'somescript.gradle'`).

## How to use it

In the `build.gradle` for your **root** project, do this:

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

`AsFile.resource` returns a `File` which was downloaded to your system temp directory, from the `src/main/resources` folder of `acme/blowdryer-foo`, at the `v1.4.5` tag.  Only one download will ever happen for the entire machine, and it will cache it until your system temp directory is cleaned.  To force a clean, you can run `gradlew blowdryerWipeEntireCache`.

### How it works

`AsFile.immutableUrl` is another method you can use, which returns a `File` containing the downloaded content of the given URL.  It's on you to guarantee that the URL is immutable.

When you setup the blowdryer plugin in your root project, you're telling blowdryer what URL scheme to use when resolving a call to `AsFile.resource`, for example:

```java
public void github(String repoOrg, GitAnchorType anchorType, String anchor) {
  String root = "https://raw.githubusercontent.com/" + repoOrg + "/" + anchor + "/" + repoSubfolder + "/";
  AsFile.setResourcePlugin(resource -> {
    return root + resource;
  });
}
```

## Dev workflow

To change and test scripts before you push them up to GitHub, you can do this:

```gradle
blowdryer {
  github 'acme/blowdryer-acme', 'tag', 'v1.4.5'
  devLocal '../path-to-local-blowdryer-acme'
}
```




Once you set `File devPath` to anything non-null, all Blowdryer resources will be resolved from that local folder, rather than from the Blowdryer jar.  `applyFromLocal` does exactly the same thing as `template`, except it skips the enum typechecking and just goes straight to the `$devPath/src/main/resources/foo.gradle`.

## Limitations

If you apply any third-party plugin inside a script plugin, you cannot "see" it in any other script, including the main one. See [gradle/gradle#4007](https://github.com/gradle/gradle/issues/4007) for details.

The workaround is to make sure that either:

- each script only uses built-in gradle plugins and classes
- any script which applies a third-party plugin is completely "self-contained", and does not need to be referenced by any other plugins
- add that plugin as a dependency of your `blowdryer` plugin.

Another possible workaround would be to implement your own `apply from` which trades gradle's encapsulation away in favor of easier interoperability.




Here is how

- turning immutable URLs (e.g. a hashed git blob) into local files
  - downloaded once to your system temp dir, then never checked again (even across multiple builds)
  - local file can be used for any gradle property you want
- apply a coherent set of script plugins

end up having to fix the same build problem over and over .  You end up copy-pasting common bits of configuration from project to project.  Blowdryer is a project which helps with (Dont)
