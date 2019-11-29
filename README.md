# Blowdryer: keep your gradle builds DRY

The pain points of maintaining multiple loosely-related gradle projects in separate repositories are:

- challenging to keep their build files consistent (copy-paste doesn't scale)
- frustrating to fix the same build upgrade problems over and over in multiple repositories
- a single "master plugin" which applies plugins for you is too restrictive
	- hard to debug
	- hard to experiment and innovate

Blowdryer helps by providing structure, performance, and workflow for applying a coherent set of [script plugins](https://docs.gradle.org/current/userguide/plugins.html#sec:script_plugins) (`apply from: 'somescript.gradle'`).

## How it works

In the `build.gradle` for each of your projects, you do this:

```gradle
plugins {
id 'com.diffplug.blowdryer' version '1.0.0'
}

blowdryer {
templates 'java8', 'eclipse', 'spotless'
}
```

Which will apply the scripts with those same names in the [`src/main/resources`](src/main/resources) directory of the blowdryer repository, at the same version that blowdryer was published.  If those scripts rely on any local configuration files, you can use `AsFile.fromResource()` to grab them from the git repository as local files.

## How to add your own templates

The section above was fibbing.  You should not use this plugin - you should fork it and use *your own* plugin.  For example, this is how ACME should use it:

```gradle
plugins {
id 'org.acme.blowdryer' version '1.0.0'
}

blowdryerAcme {
templates 'kotlin', 'idea', 'spotless'
}
```

If you have a new way to make or compose templates, please contribute it back to this repository.  But if you just have a new template, the way to share it is by starting as a fork from this repository, and once you have something useful then send a PR to extend this list:

- [blowdryer-diffplug](https://github.com/diffplug/blowdryer-diffplug)

People can browse the forks to see how other organizations are splitting their builds up.

### Exact steps to fork

1. Fork this repository
	- rename `TemplateExtension.NAME` to be `blowdryerAcme`
	- change the package from `com.diffplug` to `org.acme`
	- change the plugin ID, implementation class, and maven metadata in `build.gradle`
2. Put your scripts into `src/main/resources`
3. Modify the `Templates` enum to have a value for each script you would like to have

### Testing

To test your plugin, you can use the normal [publish to mavenLocal approach](https://github.com/diffplug/goomph/blob/master/CONTRIBUTING.md#test-locally).  There are a few tricks to make things even easier:

```gradle
blowdryer {
devPath = rootProject.file('../blowdryer')
templates 'kotlin', 'idea', 'spotless'
applyFromLocal 'foo.gradle'
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
