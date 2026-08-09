# Kharji 💸

[![CI](https://github.com/Meko123456/Kharji/actions/workflows/ci.yml/badge.svg)](https://github.com/Meko123456/Kharji/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**ხარჯი** (*kharji* — Georgian for "expense") — a multi-currency expense tracker
built for a life lived between two currencies.

<p align="center">
  <img src="docs/screenshot-home.png" width="320" alt="Home: multi-currency month summary with converted total, day-grouped entries" />
</p>

Born from a real problem: earning and spending across **GEL and AED** means no
mainstream tracker fits. Kharji does three things most don't:

- 🏦 **Auto-capture from bank SMS** — a `NotificationListenerService` parses
  TBC and Bank of Georgia transaction messages and creates entries automatically.
  No manual entry for card payments.
- 💱 **True multi-currency** — every entry keeps its original currency; totals
  convert via cached FX rates with a full offline fallback.
- 📤 **Your data is yours** — one-tap CSV export, everything in Room, no cloud.

## Working today

- ✅ Manual entries: quick-add dialog, GEL/AED/USD/EUR, category chips, merchant/notes
- ✅ Day-grouped list, per-currency monthly totals, **converted ≈GEL grand total**
- ✅ FX rates auto-refreshed daily (open.er-api.com), cached in Room, offline
  fallback with stale-rate indicator
- ✅ CSV export via share sheet (RFC-4180-safe, tested)
- ✅ Money core: integer minor-unit arithmetic — no floating-point money, tested

## Coming next

- TBC / BoG SMS notification parsing (opt-in, on-device only — messages never leave the phone)
- Receipt scanning (CameraX + ML Kit) — tracked in issues

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · NotificationListenerService ·
Ktor (FX rates) · WorkManager · CameraX + ML Kit (later)

## Status

🚧 Active development. See [issues](../../issues) for the roadmap.

## Privacy

SMS/notification parsing is **opt-in**, runs entirely on-device, and only reads
messages from known bank senders. Nothing is uploaded anywhere, ever.

## License

[MIT](LICENSE)
