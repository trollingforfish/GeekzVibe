# Pemutar Musik & Lirik - Immersive UI 🎧

Aplikasi pemutar musik mobile premium dengan lirik lagu lengkap, pencarian pintar 3 sumber + fallback AI, dan penerjemah Bahasa Indonesia otomatis. Didesain dengan **Immersive UI** modern (Material 3, Cosmic Dark theme, ambient glowing backdrops, dan glassmorphic panels).

Aplikasi ini diimplementasikan menggunakan **Android Native (Kotlin & Jetpack Compose)** dengan arsitektur MVVM bersih yang sangat optimal dan responsif untuk Handphone maupun Tablet.

---

## 🚀 Fitur Utama Aplikasi

### 1. Pemutar Musik Imersif
- **Kontrol Musik Lengkap**: Play, Pause, Skip Next, Skip Previous, dan Progress seek slider presisi.
- **Volume & Audio**: Slider volume terintegrasi langsung di layar Now Playing.
- **Mode Putar**: Dukungan penuh untuk mode **Shuffle** (acak) dan **Repeat** (putar ulang lagu tunggal atau seluruh antrean).
- **Desain Cantik**: Layar Now Playing dengan visual album art besar, bayangan lembut, border elegan, dan background gradasi glowing dinamis.
- **Equalizer**: Graphic Equalizer 5-Band (60Hz, 230Hz, 910Hz, 4kHz, 14kHz) yang interaktif untuk menghasilkan kualitas audio terbaik.
- **Sleep Timer**: Pengatur waktu tidur otomatis (5m, 15m, 30m, 45m, 60m) untuk menghentikan musik saat Anda tertidur.

### 2. Pencarian Lirik Pintar (4 Sumber)
Pencarian lirik otomatis berdasarkan informasi lagu (Judul & Artis) dengan prioritas:
1. **Musixmatch API** (Priority 1)
2. **Genius API** (Priority 2)
3. **AZLyrics** (Priority 3 - Scraping HTTP & regex parser aman)
4. **Gemini AI Fallback** (Priority 4 - Fallback cerdas yang menjamin lirik 100% ditemukan)
- **Tampilan Lirik**: Layar lirik penuh dengan animasi scrolling halus.
- **Indikator Loading**: Animasi loading sirkular modern yang memberi tahu proses pencarian lirik aktif.
- **Lokal Cache**: Lirik disimpan di database lokal **Room SQLite** agar bisa diakses secara offline tanpa perlu request ulang.

### 3. Penerjemah Lirik Instan
- **Terjemahan Sekali Ketuk**: Tombol "TERJEMAHKAN" di halaman lirik yang menggunakan **Gemini AI Translator** untuk menerjemahkan lirik asing ke Bahasa Indonesia puitis.
- **Mode Bilingual (Side-by-Side)**: Menampilkan baris lirik asli tepat di atas baris terjemahannya untuk kemudahan bernyanyi.
- **Mode Toggle**: Saklar praktis untuk beralih tampilan antara lirik asli penuh atau lirik terjemahan penuh.
- **Lokal Cache Terjemahan**: Hasil terjemahan disimpan permanen bersama lirik di cache lokal (Room).

### 4. Manajemen Lagu & Playlist
- **Pemindai Media (Scan)**: Memindai penyimpanan lokal perangkat secara otomatis mencari file audio berformat `.mp3`, `.m4a`, atau `.wav`.
- **Pre-loaded Streaming Tracks**: Database secara otomatis di-seed dengan 5 lagu berkualitas tinggi bebas royalti saat aplikasi pertama kali dibuka, sehingga Anda bisa langsung memutar musik tanpa perlu memindahkan file manual!
- **Daftar Suka (Favorit)**: Tandai lagu kesayangan Anda dengan tombol hati yang interaktif.
- **Manajemen Playlist**: Membuat, mengganti nama, menghapus playlist kustom, serta menambah/menghapus lagu dari playlist secara dinamis.

---

## 📂 Struktur Folder Proyek

```text
app/src/main/java/com/example/
├── MainActivity.kt               # Entry point, Bottom Navigation, & MiniPlayer Orchestrator
├── data/
│   ├── model/
│   │   ├── Song.kt               # Entity data model lagu
│   │   ├── Playlist.kt           # Entity data model playlist
│   │   ├── PlaylistSongCrossRef.kt # Relasi many-to-many playlist & lagu
│   │   └── Lyric.kt              # Entity cache lirik & terjemahan
│   ├── database/
│   │   ├── AppDatabase.kt        # Room Database local storage
│   │   └── MusicDaos.kt          # DAO untuk query SQLite
│   ├── repository/
│   │   └── MusicRepository.kt    # Abstraksi data (MediaStore scan, seed, cache lirik)
│   └── service/
│   │   ├── AudioPlayerService.kt # Media3 ExoPlayer wrapper (play, pause, eq, volume)
│   │   ├── GeminiService.kt      # Direct REST API ke Gemini AI
│   │   ├── LyricService.kt       # API Musixmatch, Genius, scraping AZLyrics, & Gemini fallback
│   │   └── TranslationService.kt # Penerjemah lirik cerdas Bahasa Indonesia via Gemini
├── ui/
│   ├── theme/
│   │   ├── Color.kt              # Token warna Immersive Cosmic Theme
│   │   ├── Theme.kt              # Konfigurasi M3 Dark/Light Theme
│   │   └── Type.kt               # Tipografi teks
│   ├── viewmodel/
│   │   └── MusicViewModel.kt     # State management, coroutines, timer, & equalizer
│   ├── screen/
│   │   ├── HomeScreen.kt         # Dashboard utama (salam dinamis, search, kontrol premium, list)
│   │   ├── PlayerScreen.kt       # Immersive Now Playing (glow, seek, volume, shuffle, repeat)
│   │   ├── LyricScreen.kt        # Visualizer lirik & saklar terjemahan bilingual
│   │   ├── PlaylistScreen.kt     # CRUD playlist & penambah lagu checkable
│   │   └── SearchScreen.kt       # Eksplorasi genre terpopuler & pencarian penuh
│   └── widget/
│       ├── MiniPlayer.kt         # Bar pemutar musik mini melayang dengan progress bar top
│       ├── SongTile.kt           # Desain baris lagu dengan feedback ripple & dropdown opsi
│       └── LyricDisplay.kt       # Scrolling lirik dinamis (bilingual & toggle)
```

---

## 🛠️ Cara Setup & Menjalankan Aplikasi

### 1. Prasyarat
- **Android Studio** (Koala atau versi terbaru)
- **Android SDK** API level 24 (Android 7.0) atau lebih tinggi
- **Gradle** versi 8.0+

### 2. Memasukkan API Key Gemini (PENTING)
Aplikasi menggunakan **Gemini AI** untuk pencarian lirik otomatis dan penerjemah puitis Bahasa Indonesia. 

- **Di Editor Google AI Studio**:
  1. Buka panel **Secrets** di pojok kanan bawah layar editor.
  2. Tambahkan variabel dengan key: `GEMINI_API_KEY`.
  3. Masukkan token API Gemini Anda di bagian value.
  4. Build ulang atau restart pemutar. Token akan terhubung secara aman via `BuildConfig` tanpa ter-hardcode di file sumber!

### 3. Build & Run Manual
Untuk menjalankan aplikasi secara manual di perangkat fisik atau emulator Anda:
```bash
# Clone atau buka direktori proyek ini
cd /workspace

# Jalankan kompilasi Gradle untuk verifikasi build
gradle assembleDebug
```
Setelah berhasil dikompilasi, Anda dapat menginstal file APK hasil build di perangkat Anda:
- Path output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 GitHub Actions Workflow

Skenario CI/CD otomatis dikonfigurasi lengkap di `.github/workflows/build.yml` yang mencakup:
- **Build Android**: Menghasilkan file APK rilis universal, split-per-abi, serta file AAB (App Bundle) siap rilis di Google Play Store.
- **Build iOS**: Mengompilasi aplikasi untuk iOS (No Codesign) sebagai Runner.app.
- **Build Web**: Mengompilasi build web Flutter.
- **Auto Release**: Membuat rilis otomatis di GitHub saat ada tag versi baru yang di-push (`v1.0.0`, dll), menyertakan seluruh aset APK/AAB, dan menyusun changelog otomatis dari riwayat commit.
- **Keamanan**: Kunci API di-inject secara aman menggunakan `--dart-define` melalui GitHub Secrets (`secrets.GEMINI_API_KEY`, dsb).
