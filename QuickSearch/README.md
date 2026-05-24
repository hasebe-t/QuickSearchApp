# みれる - AI調査アプリ（Android版）

スクリーンショットやカメラ映像の気になる部分を指で選択して、
ML Kit + Gemini AI がリアルタイムに解説するAndroidアプリです。

---

## 機能

| 機能 | 内容 |
|------|------|
| 📸 スクリーンショット | 画像を読み込み、指でドラッグ選択してAI解析 |
| 📷 カメラモード | タップでリアルタイムAI解析（即時/詳細） |
| 🔤 テキスト認識 | ML Kit OCR（日本語・英語対応） |
| 🏷️ 画像ラベリング | ML Kit で写真からオブジェクトを特定 |
| 📦 オブジェクト検出 | ML Kit で物体を検出・分類 |
| 🤖 詳細解析 | Gemini 1.5 Flash で詳細情報を生成 |
| 📋 履歴管理 | 調査結果をRoom DBに保存・後から閲覧 |
| ⚙️ 設定画面 | Gemini APIキー・Googleアカウント登録 |

---

## ビルド環境

- **Android Studio Panda** (2024.2.1) 以降
- JDK 17
- Android SDK API 35（minSdk 26）
- Kotlin 2.0.0

---

## セットアップ手順

### 1. プロジェクトを開く
Android Studio で `mireru-android/` フォルダを開く

### 2. local.properties を作成
```
# local.properties（プロジェクトルートに作成）
sdk.dir=C:\Users\yourname\AppData\Local\Android\Sdk
```
※ Android Studio が自動生成する場合はそのままでOK

### 3. Gemini APIキーを取得
1. https://aistudio.google.com/app/apikey にアクセス
2. Googleアカウントでログイン
3. 「Create API Key」でキーを生成
4. キーをコピー（`AIzaSy...` で始まる文字列）

### 4. ビルド＆実行
1. Android実機をUSB接続（またはエミュレータ起動）
2. Android Studio の ▶ Run ボタンをクリック

### 5. アプリ内で設定
1. アプリ起動後、右上の ⚙ アイコンをタップ
2. 「Googleアカウント」にメールアドレスを入力して保存
3. 「Gemini APIキー」に取得したキーを貼り付けて保存
4. メイン画面に戻って使用開始

---

## AI解析フロー

```
画像選択 / カメラタップ
    ↓
ML Kit OCR（テキスト認識）       ← オフライン・高速
    ↓
ML Kit 画像ラベリング             ← オフライン・高速
    ↓
ML Kit オブジェクト検出           ← オフライン・高速
    ↓
Gemini 1.5 Flash（詳細解析）     ← オンライン・高精度
    ↓
結果をサブウィンドウに表示
```

---

## プロジェクト構成

```
app/src/main/java/com/mireru/app/
├── MainActivity.kt
├── MireruApplication.kt
├── data/
│   ├── GeminiRepository.kt     ← Gemini API通信
│   ├── SettingsDataStore.kt    ← APIキー・設定保存
│   ├── HistoryDao.kt
│   └── HistoryDatabase.kt
├── di/AppModule.kt
├── model/
│   ├── AnalysisResult.kt
│   └── HistoryItem.kt
├── ui/
│   ├── MainScreen.kt           ← スクリーンショット画面
│   ├── CameraScreen.kt         ← カメラ画面
│   ├── HistoryScreen.kt        ← 履歴画面
│   ├── SettingsScreen.kt       ← 設定画面（NEW）
│   ├── SubWindowSheet.kt       ← 結果ボトムシート
│   ├── ContextMenuOverlay.kt   ← コンテキストメニュー
│   ├── PermissionScreen.kt     ← 権限要求
│   └── theme/
├── util/
│   ├── OcrHelper.kt            ← ML Kit OCR
│   ├── MlKitHelper.kt          ← ML Kit ラベリング＋検出（NEW）
│   └── ImageCropper.kt
└── viewmodel/
    ├── MainViewModel.kt
    ├── CameraViewModel.kt
    ├── HistoryViewModel.kt
    └── SettingsViewModel.kt    ← NEW
```

---

## 使用ライブラリ

| ライブラリ | バージョン | 用途 |
|-----------|-----------|------|
| Jetpack Compose | BOM 2024.08 | UI |
| CameraX | 1.3.4 | カメラ |
| ML Kit Text Recognition Japanese | 16.0.1 | 日本語OCR |
| ML Kit Image Labeling | 3.0.8 | 画像ラベリング |
| ML Kit Object Detection | 17.0.2 | オブジェクト検出 |
| Gemini 1.5 Flash API | v1beta | AI詳細解析 |
| Room | 2.6.1 | 履歴DB |
| DataStore Preferences | 1.1.1 | 設定保存 |
| Hilt | 2.52 | DI |
| OkHttp | 4.12.0 | HTTP通信 |
| Coil | 2.7.0 | 画像表示 |
