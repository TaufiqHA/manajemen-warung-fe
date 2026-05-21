# Rencana Implementasi: Pembersihan Konteks "Gemini"

## Tujuan
Menghapus semua referensi, teks, variabel, dan konteks yang berkaitan dengan "Gemini" di dalam file `README.md`.

## File yang Menjadi Target
- `README.md` (di root proyek)

## Instruksi Pengerjaan (Langkah demi Langkah)

**1. Persiapan**
- Buka file `README.md`.
- Lakukan pencarian teks (Find / `Ctrl+F` atau `Cmd+F`) untuk kata kunci berikut (pencarian bersifat *case-insensitive* / abaikan huruf besar-kecil):
  - `gemini`
  - `gemini api`
  - `gemini api key`
  - `GEMINI_API_KEY`

**2. Eksekusi Penghapusan**
- **Paragraf/Penjelasan:** Jika ada kalimat atau paragraf yang secara spesifik menjelaskan tentang penggunaan Gemini (misalnya cara mendapatkan key, memasukkan API key, atau penjelasan integrasi AI Gemini), **hapus keseluruhan kalimat atau paragraf tersebut** agar tidak meninggalkan kalimat yang terpotong.
- **Konfigurasi / Code Blocks:** Jika terdapat contoh *code block* (seperti contoh isi file `.env`) yang memiliki referensi `GEMINI_API_KEY=...`, hapus baris tersebut.
- **Daftar/Poin (List):** Jika kata "Gemini" berada di dalam sebuah *bullet point* fitur atau *requirements*, hapus poin tersebut secara keseluruhan.

**3. Perapihan Format Markdown**
- Setelah teks dan baris dihapus, pastikan tidak ada baris kosong (blank lines) berlebih yang membuat jarak antar paragraf/header terlihat aneh.
- Pastikan struktur penomoran atau daftar *bullet point* tidak rusak (tetap berurutan dan rapi).

**4. Validasi Akhir**
- Lakukan pencarian sekali lagi terhadap kata kunci `gemini` di `README.md`. Pastikan hasilnya **0 (nol) temuan**.
- Pastikan kalimat di atas dan di bawah area yang dihapus tetap menyambung dan masuk akal dibaca.

## Batasan (Constraints)
- **HANYA** ubah file `README.md`.
- **DILARANG** menghapus, memodifikasi, atau mencari teks di dalam file lainnya (seperti `.env.example`, `GEMINI.md`, atau kode sumber di dalam folder `app/`).
