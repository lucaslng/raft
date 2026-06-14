# Raft

Raft is a 3D first-person survival game built with libGDX in which the player is stranded on a small wooden raft in a vast, endlessly drifting ocean. The player must collect floating trash to expand their raft. The player can craft essential buildings such as water filters, cooking pots, and farms, and manage their health, hunger, and thirst to stay alive. A shark lurks beneath the raft and attacks whenever the player falls into the water, adding danger to any attempts to swim out and collect items. The ultimate goal is to craft and place a sail, steer the raft northward through the waves, and reach a distant safe haven 5,000 units north before the player's stats run out.

I implemented all the features in the proposal which were not optional. But I think the game needs multiplayer and more progression to actually be fun. I wrote the project with this in mind, so making more buildings, ui panels, tools, and items is really easy, and implementing multiplayer doesn't require too many changes. However, I couldnt work on it because I was sick, otherwise I definitely would have made these changes.

On top of the mandatory features, I also implemented a loading screen, music, and a lot of sound effects.

The one difference to the initial designs is the UI. I changed it a lot because I really didn't like the original UI style. It came from a skin that I found online and I couldn't find better skins. Also, I made the hotbar text-only because I couldn't find good 2D textures for buildings that would correspond to their 3D textures, and I didn't have time to render their 3D textures into 2D and then render them on the hotbar.

My code is in `core/src/main/java/com/lucaslng/raft`

## Cheats

In the settings, there is a cheats toggle! You get free items and unlock crafting recipes at the start. Also, extending your raft becomes free.

## Quickstart

Run using `./gradlew lwjgl3:run` or `gradlew.bat lwjgl3:run`
Build jar using `./gradlew lwjgl3:jar` or `gradlew.bat lwjgl3:jar`

## Known Bugs/Issues

- Swimming is a little bit finicky, I don't know how to tune the physics values, especially buoyancy, to perfect it
- Ocean waves are not translucent, they sometimes cover the raft
- The ocean is tiled, at the edges of the tiling there is a border. But it is barely noticable and you'll only find it if you're looking for it
- Some models have slightly offset hitboxes. I can't really fix this unless I find better models, make my own, or hardcode the offset/scaling. So I just made sure everything was close enough. If you want to see the hitboxes, turn on debugging in the settings

## Gradle

The stuff below is automatically generated.

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
