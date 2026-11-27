# Publishing a release

A release gives a version of TransLogix that runs on Windows **without Java or Maven installed**:
a zip holding `TransLogix.exe` and its own Java runtime (about 50 MB).

## Automatic (GitHub Actions)

Pushing a tag that starts with `v` runs `.github/workflows/release.yml`. It builds the app, runs the
tests, packages it and attaches `TransLogix-<version>-windows.zip` to the GitHub Release of that tag.

Only from an up-to-date `main` with a green `build` check:

```powershell
git switch main
git pull
git tag -a v0.3-iteration3 -m "Itération 3"
git push origin v0.3-iteration3
```

The number at the start of the tag (`0.3`) becomes the app version. Follow the run in the **Actions** tab;
the zip then appears under **Releases**.

## By hand (on your own machine)

Needs JDK 21 (it contains `jpackage`):

```powershell
.\scripts\package-windows.ps1 -Version 0.3
```

Result: `target\dist\TransLogix-0.3-windows.zip`. To try it without unzipping:
`target\dist\TransLogix\TransLogix.exe`.

## Using the downloaded app

1. Unzip, open the `TransLogix` folder, double-click `TransLogix.exe`.
   Windows SmartScreen may warn about an unknown publisher (the app is not signed): **More info → Run anyway**.
2. Without settings the app runs in **demo mode** (in memory, nothing saved).
3. To use MySQL, set `DB_URL`, `DB_USER` and `DB_PASSWORD` as Windows user environment variables
   (same values as in the README), then start the app again. The sidebar shows `Base MySQL connectée`.

## How it works

- `ma.translogix.Launcher` is the entry point of the packaged app. JavaFX refuses to start a main class
  that extends `Application` from a plain classpath, so `Launcher` just calls `Main.main`.
- `mvnw package` builds the app jar and copies its runtime dependencies to `target/lib`
  (`maven-dependency-plugin`).
- `jdeps` lists the Java modules the app needs and `jpackage` (part of JDK 21) bundles only those with the app.
  The modules used only by the MySQL driver at connection time (`java.naming`, `java.management`) are added by
  the script: without them the app crashes as soon as `DB_URL` is set.
