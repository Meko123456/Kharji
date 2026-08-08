# Kharji 💸

**ხარჯი** (*kharji* — Georgian for "expense") — a multi-currency expense tracker
built for a life lived between two currencies.

Born from a real problem: earning and spending across **GEL and AED** means no
mainstream tracker fits. Kharji does three things most don't:

- 🏦 **Auto-capture from bank SMS** — a `NotificationListenerService` parses
  TBC and Bank of Georgia transaction messages and creates entries automatically.
  No manual entry for card payments.
- 💱 **True multi-currency** — every entry keeps its original currency; totals
  convert via cached FX rates with a full offline fallback.
- 📤 **Your data is yours** — one-tap CSV export, everything in Room, no cloud.

## Planned features

- Manual entries with categories, notes, and quick-add favorites
- TBC / BoG SMS notification parsing (opt-in, on-device only — messages never leave the phone)
- FX rates cached daily; stale-rate indicator when offline
- Monthly overview per currency + converted total
- CSV export via share sheet
- Receipt scanning (CameraX + ML Kit) — later milestone, tracked in issues

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · NotificationListenerService ·
Ktor (FX rates) · WorkManager · CameraX + ML Kit (later)

## Status

🚧 Day 1 — README-first. See [issues](../../issues) for the full roadmap.

## Privacy

SMS/notification parsing is **opt-in**, runs entirely on-device, and only reads
messages from known bank senders. Nothing is uploaded anywhere, ever.

## License

[MIT](LICENSE)
