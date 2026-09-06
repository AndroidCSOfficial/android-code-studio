# LITE Flavor — Varian Ringan RAM 2GB (Agent-3)

Target: APK lebih kecil + hemat RAM, **AI (generativeai) tetap dipertahankan**.
`settings.gradle.kts` TIDAK diubah — semua modul tetap ada untuk varian `full`.

## Flavor

- Dimensi `tier`: `lite` (`applicationIdSuffix .lite`, `versionNameSuffix -lite`)
  dan `full`. Definisi di `core/app/build.gradle.kts:75-90`.
- Fallback suffix APK `universal` untuk varian flavor tanpa filter ABI
  (`core/app/build.gradle.kts:183-193`) agar konfigurasi varian tidak throw.
- Flag runtime: `LiteMode` (`ideconfigurations/.../utils/LiteMode.kt`),
  `isLowRam(context)` via `ActivityManager.isLowRamDevice` / `memoryClass <= 128`.

## Dipangkas (lite) vs Dipertahankan

> Stage-1 SAFE (saat ini, agar `lite` tetap kompilasi untuk Kotlin/Java):
> 9 kandidat di bawah masih `implementation` + tag `LITE-Stage1`.
> Stage-2 (setelah call-site di-guard `LiteMode.isLowRam()`): flip ke `fullImplementation`.

| Dependensi | Status lite Stage-1 | Lokasi |
|---|---|---|
| `utilities:uidesigner` | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:346` |
| `utilities:xmlInflater` | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:347` |
| `logging:idestats` | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:331` |
| `logging:logsender` | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:332` |
| BlurView | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:237` |
| seasonal.effects | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:244` |
| charts | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:259` |
| SilentInstaller | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:275` |
| appintro (composite) | STAGE-1 `implementation`, NEXT `fullImplementation` | `core/app/build.gradle.kts:307` |
| generativeai (AI) | DIPERTAHANKAN (`implementation`) | `core/app/build.gradle.kts:~268` |
| editor (`libs.common.editor`, `editor:impl`) | DIPERTAHANKAN | tidak diubah |
| java LSP (`java:javac-services`, `lsp-setup`, `lsp`) | DIPERTAHANKAN | tidak diubah |
| xml LSP inti (`xml:lsp`, `aaptcompiler`, `utils`) | DIPERTAHANKAN | tidak diubah |
| JGit | DIPERTAHANKAN | tidak diubah |
| preferences, treeview | DIPERTAHANKAN | tidak diubah |
| termux/*, gradle.properties | TIDAK DISENTUH (milik Agent-2/4) | — |

Setiap baris kandidat ditandai `// LITE-Stage1`: sebelum
`assembleLite`, call-site (Activity/preview/worker) yang memakai modul
full-only wajib di-guard dengan `LiteMode.isLowRam()`, baru flip ke `fullImplementation`.

## File dibuat/diubah

1. BARU: `ideconfigurations/src/main/java/com/tom/rv2ide/ideconfigurations/utils/LiteMode.kt`
   — `object LiteMode { ENABLED, isLowRam(context), memoryClassMb(context) }`.
   Dipilih `ideconfigurations` (Android library, ada `Context`);
   `utilities/shared` JVM murni sehingga tidak cocok.
2. UBAH: `core/app/build.gradle.kts` — flavor `lite`/`full` (72-90),
   fallback `universal` (183-193), 9 dep berat → `fullImplementation`
   (236-347).
3. APPEND: `core/app/proguard-rules.pro` (156-161) — keep `LiteMode` +
   `generativeai`.

## Kandidat exclude modul (catatan saja, TIDAK dieksekusi)

Jika suatu saat modularisasi per-flavor diizinkan: `:utilities:uidesigner`,
`:utilities:xml-inflater`, `:logging:idestats`, `:logging:logsender`.
Butuh guard kode + source-set per flavor — wewenang lanjutan, bukan sekarang.

## Verifikasi ringan

- `git diff --stat`: 2 file diubah (`core/app/build.gradle.kts`,
  `core/app/proguard-rules.pro`) + 1 file baru (`LiteMode.kt`).
- Assemble penuh TIDAK dijalankan (milik Agent-5).
