# Project instructions

## Working principle

- Think from first principles. Do not assume the user already knows exactly what they want or how to achieve it.
- If the motivation or goal is unclear, stop and discuss it with the user.
- If the goal is clear but the proposed path is not the shortest, explain that and recommend the shorter path.
- Do not preserve backward compatibility when the current requirements supersede obsolete behavior; remove obsolete paths instead of adding compatibility layers, fallbacks, or migrations.
- Choose the simplest implementation that fully meets the current requirements. Avoid speculative abstractions, configuration, and indirection.
- Grow the system in layers: start with the smallest end-to-end implementation that works, then add capabilities on top of that working product.
- Keep components modular and concerns clearly separated; never trade a working product for unfinished complexity.
- Prefer established, well-maintained libraries when they reduce overall complexity or improve reliability.
- Reuse dependencies already in the project before writing custom implementations or adding packages; check their documentation and types before assuming a capability is missing.
- Make architectural decisions for the long term; do not accept stopgaps intended to be replaced later.

## Release discipline

- Debug-only simulator: use `127.0.0.1:5745` for all manual installs, device tests,
  and debug verification. Do not use the four release targets during debug.
- Every project update must increment both `versionCode` and `versionName` in `app/build.gradle` before building the APK.
- Release APK updates are installed to all four simulator targets by default:
  - BlueStacks Air instance `米饭` (`Tiramisu64_20`, `127.0.0.1:5755`)
  - BlueStacks Air instance `地球` (`Tiramisu64_21`, `127.0.0.1:5765`)
  - BlueStacks Air instance `地球瘦子` (`Tiramisu64_22`, `127.0.0.1:5775`)
  - BlueStacks Air instance `栗威` (`Tiramisu64_23`, `127.0.0.1:5785`)
- Attempt each release target once. If ADB connection or `install -r` fails, report that
  target and continue without retrying; the simulator may not have finished starting.
  Do not restart or recreate an instance merely to complete the install.
- Debug instance: `Tiramisu64_19` (`127.0.0.1:5745`)
