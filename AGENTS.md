# Project instructions

## Ponytail skill

- Use the available `ponytail` skill at its default `full` intensity only for coding, bug fixes, refactoring, code review, or software design tasks.
- Do not use `ponytail` for non-coding work such as installation, running commands, testing an existing build, answering questions, or general discussion.
- When it applies, read the skill's current `SKILL.md` before taking action and follow it throughout the response.

## Working principle

- Think from first principles. Do not assume the user already knows exactly what they want or how to achieve it.
- If the motivation or goal is unclear, stop and discuss it with the user.
- If the goal is clear but the proposed path is not the shortest, explain that and recommend the shorter path.

## Release discipline

- Every project update must increment both `versionCode` and `versionName` in `app/build.gradle` before building the APK.
- Install every APK update to all three simulator targets by default:
  - BlueStacks Air instance `米饭` (`Tiramisu64_15`, `127.0.0.1:5705`)
  - BlueStacks Air instance `栗威` (`Tiramisu64_17`, `127.0.0.1:5725`)
  - BlueStacks Air instance `地球瘦子` (`Tiramisu64_18`, `127.0.0.1:5735`)
