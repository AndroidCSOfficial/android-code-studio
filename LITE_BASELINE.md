# LITE Baseline — Agent-1

Tanggal: 2026-09-06 (UTC)
Repo: /data/data/com.termux/files/home/acs-lite
Peran: READ-ONLY + setup branch. TIDAK edit build.gradle / termux / lite logic (milik agent lain).

## 1. Branch & Commit
- Base: `dev` (origin/dev, shallow depth 1)
- Branch aktif: `lite/ram2gb-lite` (baru dibuat dari dev via `git checkout -b lite/ram2gb-lite`)
- Commit HEAD: `e93e7468e6482ea51ef8e1adad6b7c6de86ae7cd`
- Subject: `e93e746 ci: use GitHub Secrets for signing credentials`
- `git log --oneline -5`: hanya 1 commit visible (shallow depth 1) → `e93e746`
- `git status` saat verifikasi awal (di dev): clean. Setelah checkout branch, muncul perubahan milik agent lain (DIBIARKAN, tidak disentuh):
  - `M core/app/build.gradle.kts` (24 insertions)
  - `?? LITE_BUILD.md`
  - `?? gradle.properties.lite`
  - `?? ideconfigurations/.../LiteMode.kt`

## 2. Ukuran Repo
- `du -sh .` → `243M`
- Top-15 (`du -sh */ | sort -rh | head`):
  1. 66M `termux/`
  2. 61M `composite-builds/`
  3. 16M `core/`
  4. 6.7M `external/`
  5. 3.3M `xml/`
  6. 3.2M `utilities/`
  7. 1.8M `java/`
  8. 1.7M `editor/`
  9. 1.2M `tooling/`
  10. 352K `event/`
  11. 321K `logging/`
  12. 232K `images/`
  13. 182K `annotation/`
  14. 71K `gradle/`
  15. 56K `ideconfigurations/` (52K saat cek pertama, fluktuasi karena file untracked agent lain)

## 3. Modul (settings.gradle.kts)
- File: `settings.gradle.kts` (`rootProject.name = "AndroidCodeStudio"`)
- Hitungan mentah: `grep -o '":[^"]*"' | wc -l` → `60`
- Koreksi: -1 template `":${module}"` (dependencySubstitution loop), -2 commented (`:server:server`, `:server:shared`) → **57 modul aktif**
- Daftar 57: `:annotation:annotations`, `:annotation:processors`, `:annotation:processors-ksp`, `:external:acsprovider`, `:external:atc`, `:core:projectdata`, `:external:logwire`, `:core:actions`, `:core:app`, `:ideconfigurations`, `:core:common`, `:core:indexing-api`, `:core:indexing-core`, `:core:lsp-api`, `:core:lsp-models`, `:core:projects`, `:core:resources`, `:editor:api`, `:editor:impl`, `:editor:lexers`, `:editor:treesitter`, `:event:eventbus`, `:event:eventbus-android`, `:event:eventbus-events`, `:java:javac-services`, `:java:lsp-setup`, `:java:lsp`, `:logging:idestats`, `:logging:logger`, `:logging:logsender`, `:termux:application`, `:termux:emulator`, `:termux:shared`, `:termux:view`, `:tooling:api`, `:tooling:builder-model-impl`, `:tooling:events`, `:tooling:impl`, `:tooling:model`, `:tooling:plugin`, `:tooling:plugin-config`, `:utilities:build-info`, `:utilities:flashbar`, `:utilities:framework-stubs`, `:utilities:lookup`, `:utilities:preferences`, `:utilities:shared`, `:utilities:templates-api`, `:utilities:templates-impl`, `:utilities:treeview`, `:utilities:uidesigner`, `:utilities:xml-inflater`, `:xml:aaptcompiler`, `:xml:dom`, `:xml:lsp`, `:xml:resources-api`, `:xml:utils`
- Composite builds tambahan: `composite-builds/build-logic`, `composite-builds/build-deps`, `composite-builds/build-deps-common`

## 4. Submodule
- `.gitmodules`:
  ```
  [submodule "composite-builds/external/logback-android"]
    path = composite-builds/external/logback-android
    url = https://github.com/evil-hero/logback-android
  ```
- `git submodule status` (dan `--recursive`) → **output kosong, exit 0** (tidak ada entri terdaftar)
- `git ls-files --stage | grep -c "^160000"` → `0` (tidak ada gitlink)
- `ls composite-builds/external/logback-android/` → **TERISI** (logback-classic, logback-core, dsb. + .github, README, dsb.) — file terlacak sebagai regular files (100644), bukan submodule.
- Kesimpulan: **TIDAK kosong, TIDAK perlu init. JANGAN `git submodule update --init` penuh (hemat disk).** Entri `.gitmodules` stale / sudah di-flatten ke shallow clone.

## 5. Verifikasi File Kunci
- `ls core/app/src/main` → `AndroidManifest.xml`, `assets`, `java`, `res` (ADA)
- `cat version.properties` → `# rev version control` + `version.name.rev=1.0`
- `cat ideconfigurations/build.gradle.kts` → ADA (`com.android.library` + `kotlin-android`, namespace `${BuildConfig.packageName}.ideconfigurations`, deps: `projects.core.resources`, `projects.core.common`, material, appcompat, preference, core-ktx, kotlin)
- `git branch -a` (awal): `* dev`, `remotes/origin/dev` saja. Kini + `lite/ram2gb-lite`.

## 6. RAM / Disk Device
- Spek tugas: Mem 3.6G, avail 8.8G
- Aktual `free -h` (2026-09-06):
  ```
  Mem: 3.5Gi total, 3.0Gi used, ~217Mi free, ~625Mi buff/cache, ~583Mi available
  Swap: 6.0Gi total, 2.5Gi used, 3.5Gi free
  ```
  → RAM ketat, wajib build hemat (sesuai misi lite/ram2gb).
- Aktual `df -h .`:
  ```
  /dev/block/dm-61  47G  Size, 39G Used, 8.5G Avail (82%)
  ```
  → konsisten dengan estimasi avail 8.8G (sedikit turun ke 8.5G). Jangan init submodule penuh.

## 7. Catatan untuk Koordinator
- Branch `lite/ram2gb-lite` sudah siap, base = dev @ e93e746.
- Working tree kotor oleh agent lain — Agent-1 tidak menyentuh.
- File ini (`LITE_BASELINE.md`) satu-satunya file baru dari Agent-1.
