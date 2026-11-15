# AGENTS.md

BetterSony: Android companion app for the Sony WH-1000XM4. Kotlin, Jetpack Compose, Glance widgets, Quick Settings tiles. Talks to the headphones over Sony's MDR protocol (RFCOMM), no network.

## Layout

- `mdr/` frame codec and blocking RFCOMM client
- `data/` headset state, MDR parsing, session, repository, prefs, background job
- `ui/` Compose screens (custom components, no Material)
- `alerts/` low battery notification and 15 min background job
- `widget/` Glance widgets, incl. the power off widget
- `tile/` Quick Settings tiles
- `docs/PROTOCOL.md` verified MDR commands
- `app/src/debug/` headless MDR probe (debug builds only)

## Rules

- Never add the INTERNET permission.
- Only send MDR commands listed in `docs/PROTOCOL.md`. Never probe unknown functions (firmware update and test commands can brick or reset the headphones).
- One command in flight at a time, always wait for the ACK.
- Keep files under ~250 lines. No trivial comments, no magic numbers.
- UI copy: short and natural, no em dashes.
- Conventional Commits, one logical change per commit.

## Skills

- `.opencode/skills/build` build, install, demo data, probe
- `.opencode/skills/release` publish a GitHub release
