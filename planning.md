# Panduan Pembuatan APK (APK Build Plan)

Dokumen ini berisi langkah-langkah sistematis untuk mem-build (membangun) file APK Android dari proyek ini. Instruksi ini dibuat secara spesifik agar mudah diikuti oleh *junior developer* atau *AI agent*.

## Prasyarat Lingkungan (Environment)
Pastikan terminal berada di direktori *root* proyek (misalnya: `/home/padikering/Documents/kerja/manajemen warung/fe`).

---

## Langkah 1: Persiapan File `.env`
Proyek ini membutuhkan file `.env` untuk menyimpan API key rahasia.

1. **Periksa** apakah file `.env` sudah ada.
2. Jika belum ada, **duplikat** file `.env.example` dan ubah namanya menjadi `.env` menggunakan perintah berikut:
   ```bash
   cp .env.example .env
   ```
3. **Isi** variabel `GEMINI_API_KEY` di dalam file `.env`. (Jika hanya untuk memastikan proses build berhasil tanpa interaksi API asli, Anda dapat menggunakan nilai *dummy* seperti `GEMINI_API_KEY=dummy_key`).

## Langkah 2: Persiapan `debug.keystore`
Konfigurasi Gradle proyek ini membutuhkan file `debug.keystore` di *root* direktori untuk proses *signing* APK. Saat ini file tersebut disimpan dalam format *base64*.

1. **Dekode** file `debug.keystore.base64` agar menjadi file `debug.keystore` yang bisa dibaca oleh sistem:
   ```bash
   base64 --decode debug.keystore.base64 > debug.keystore
   ```
   *(Catatan: Jika `--decode` tidak dikenali, gunakan `base64 -d debug.keystore.base64 > debug.keystore`)*

## Langkah 3: Memberikan Izin Eksekusi pada Gradle
Pastikan script `gradlew` memiliki izin untuk dieksekusi oleh sistem operasi.

1. Jalankan perintah berikut di terminal:
   ```bash
   chmod +x gradlew
   ```

## Langkah 4: Proses Build APK
Mulai proses kompilasi kode dan pembuatan APK versi Debug.

1. Eksekusi perintah Gradle berikut:
   ```bash
   ./gradlew assembleDebug
   ```
2. Tunggu hingga proses selesai. Waktu build dapat bervariasi bergantung pada spesifikasi perangkat dan apakah Gradle perlu mengunduh *dependency* (librari) baru. Pastikan status di terminal menunjukkan `BUILD SUCCESSFUL`.

## Langkah 5: Verifikasi Hasil Build
Setelah proses build berhasil, pastikan file APK telah benar-benar dibuat.

1. Lokasi standar keluaran APK ada di *path* berikut:
   `app/build/outputs/apk/debug/app-debug.apk`
2. **Cek** keberadaan file tersebut dengan perintah:
   ```bash
   ls -la app/build/outputs/apk/debug/app-debug.apk
   ```
   *(Catatan: Proyek ini mungkin memiliki folder khusus keluaran build `.build-outputs/app-debug.apk`. Periksa juga di folder tersebut jika konfigurasi Gradle diubah untuk memindahkan hasilnya ke sana: `ls -la .build-outputs/app-debug.apk`)*

---
**Selesai!** Jika Anda mengikuti langkah di atas dan melihat pesan sukses, maka aplikasi siap untuk diinstal ke emulator atau perangkat fisik (menggunakan perintah `./gradlew installDebug` atau di-*drag-and-drop* ke emulator).
