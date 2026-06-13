# Manajemen Warung App

Aplikasi Android (Frontend) untuk sistem Manajemen Warung.

## Cara Menjalankan Proyek Secara Lokal

**Prasyarat:** Pastikan Anda telah menginstal [Android Studio](https://developer.android.com/studio).

1. Buka Android Studio.
2. Pilih opsi **Open** dan arahkan ke folder direktori proyek ini.
3. Tunggu hingga Android Studio selesai melakukan sinkronisasi Gradle (biarkan Android Studio memperbaiki ketidakcocokan jika ada).
4. Buat file bernama `.env` di direktori proyek sesuai dengan format pada `.env.example`.
5. Jika terjadi *error* terkait konfigurasi *signing*, Anda mungkin perlu menghapus baris `signingConfig = signingConfigs.getByName("debugConfig")` dari file `app/build.gradle.kts`.
6. Jalankan aplikasi pada Emulator atau Perangkat Fisik (Android Device).
