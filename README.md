# controller

Android virtual controller PoC: a floating overlay (stick + face buttons)
that drives a **uinput-backed virtual Xbox 360 gamepad**, so games with
native gamepad support see a real controller and hide their own touch
controls -- see [`docs/DESIGN.md`](docs/DESIGN.md) for the full design
walkthrough and the reasoning behind this approach.

## Why uinput, not USB/Bluetooth HID

The original ask was "make the app look like a USB or Bluetooth
controller." That's not physically possible: `BluetoothHidDevice` and USB
gadget HID both make the device present itself to *another* host, not to
itself. The actual mechanism that makes Android's own input stack treat the
app as a connected gamepad is a kernel-level virtual input device created
via `/dev/uinput`. See `docs/DESIGN.md` for the rest of the reasoning
(deadzone handling, why touch-injection/Accessibility can't clear this bar,
etc).

## Module layout

- `:core` -- plain Kotlin (no Android dependency): `PadState`, `StickMapper`
  (deadzone/clamp), the Xbox 360 uinput device profile, and
  `PadEventTranslator`/`UinputCommandBuilder` that turn state changes into
  uinput JSON commands. Fully unit tested, runnable without an Android SDK:
  `./gradlew :core:test`.
- `:app` -- the Android app: Shizuku integration (`shizuku/`), the floating
  overlay service (`overlay/`), and the permissions/pad-tester UI (`ui/`,
  `MainActivity`).

## How it works (v0 scope)

1. `MainActivity` walks the user through granting Shizuku access, the
   overlay permission, and (Android 13+) the notification permission, and
   includes a **pad tester** panel that reads back `InputDevice`/axis/key
   state live -- useful for confirming the virtual gamepad is alive without
   needing a real game installed.
2. `OverlayService` draws one stick + 4 face buttons (A/B/X/Y) as **separate
   small `WindowManager` windows**, not one full-screen overlay -- so
   untouched screen area passes straight through to the app underneath
   instead of being swallowed by a single big touchable window.
3. Touch on those controls updates a `PadState`, which
   `ShizukuGamepadBackend` diffs against the previously sent state
   (`PadEventTranslator`) and forwards to a Shizuku `UserService`
   (`UinputUserService`) running under the **shell UID**.
4. That privileged process shells out to the `uinput` command-line tool
   (bundled with AOSP), which owns `/dev/uinput` and registers a
   `Microsoft X-Box 360 pad` device -- chosen because Android ships
   `Vendor_045e_Product_028e.kl` out of the box, so games that whitelist
   known controllers are more likely to recognize it.
5. A small always-visible handle in the corner toggles capture on/off, so
   menus and system dialogs can still be tapped without stopping the
   overlay service.

## Known gaps / what still needs on-device verification

This was built in an environment with **no Android SDK and no physical
device or emulator**, so only `:core` (pure Kotlin logic) could actually be
compiled and unit-tested here. Everything in `:app` compiles conceptually
against documented APIs but has not been exercised on real hardware yet.
Before trusting this beyond "it builds in CI":

- **uinput JSON schema.** `UinputCommandBuilder` transcribes the
  `register`/`inject` command format documented for AOSP's `cmds/uinput`
  tool from memory/documentation, not from a device. Run
  `uinput --help` (or check the AOSP source for the target OS version) on
  the actual test device and diff against `UinputCommandBuilder` before
  relying on it -- field names/nesting may differ across Android versions.
- **Whether `shell` can open `/dev/uinput` at all** on the target device.
  This is the single load-bearing assumption of the whole Shizuku-based
  design. Check with `adb shell ls -lZ /dev/uinput` and `adb shell uinput -`
  before writing more code on top of this.
- **Whether the target game(s) re-scan for gamepads after launch**, or only
  at startup. If only at startup, the app needs a "launch the game for me"
  flow so the virtual pad exists before the game's input scan runs (not
  built in v0).
- Games using `HIDE_OVERLAY_WINDOWS` (Android 12+) can force-hide this
  overlay; there is no workaround.

## Troubleshooting

- **App shows "Shizuku: not running" even though the Shizuku app itself
  shows "Running", and tapping Request does nothing.** This was caused by a
  missing `<queries>` declaration for Shizuku's package
  (`moe.shizuku.privileged.api`) in `AndroidManifest.xml` -- required on
  Android 11+ (API 30+) package-visibility rules, without which this app
  cannot resolve Shizuku at all, and `Shizuku.requestPermission()` silently
  no-ops. Fixed; if it recurs, confirm `adb shell pm list packages | grep
  shizuku` shows the package and that this app was reinstalled (not just
  updated) after the manifest fix, since manifest `<queries>` changes need a
  fresh install to take effect reliably. `adb logcat -s ShizukuProvider` on
  the device while relaunching the app is the most direct way to see
  whether the binder handshake is happening at all.
- **Request button now gives feedback instead of doing nothing** even when
  Shizuku genuinely isn't reachable -- pressing it always re-polls
  `Shizuku.pingBinder()` first (a manual retry), then shows a toast
  explaining why it can't proceed if it still can't.

## Roadmap (per the design doc)

- **v0 (this PoC):** Shizuku + `uinput` CLI backend, fixed stick+4-button
  layout, pad tester.
- **v1:** JNI backend talking to `/dev/uinput` directly (lower latency than
  shelling out to the `uinput` CLI on every event), layout editor, stick
  feel tuning UI, saved profiles.
- **v2:** per-package profile auto-switching, gyro-as-right-stick,
  macros/turbo, root backend.

## Building

```bash
./gradlew :core:test          # pure-Kotlin logic, no Android SDK needed
./gradlew :app:assembleDebug  # needs Android SDK (compileSdk 34)
```

CI (`.github/workflows/build-release.yml`) runs both on every push/PR to
`main`, and publishes the debug APK as a GitHub Release on pushes to `main`.
Note the release APK is **debug-signed** -- fine for sideloading and manual
testing, not for any kind of distribution beyond that.
