# VK Reforged

VK Reforged — это современный, многофункциональный клиент VK для Android, созданный с использованием Jetpack Compose (Material You). Проект ориентирован на скорость, чистоту интерфейса и современные практики разработки.

## 🛠 Технологический стек

- **UI:** Jetpack Compose, Material 3 (Material You)
- **Архитектура:** MVVM
- **Сеть:** Retrofit 2, OkHttp 4, ktor
- **Инъекция зависимостей:** Hilt
- **База данных:** Room (кэширование сообщений)
- **Медиа:** Media3 (ExoPlayer, MediaSession)
- **Фоновые задачи:** WorkManager, Foreground Services
- **Аналитика:** Firebase Crashlytics

## 🚀 Начало работы

### Предварительные требования
1. Android Studio Ladybug или новее.
2. Аккаунт в [Google Firebase](https://firebase.google.com/).

### Установка
1. Клонируйте репозиторий.
2. Создайте проект в консоли Firebase и получите файл `google-services.json`.
3. Поместите `google-services.json` в директорию `app/`.
4. Откройте проект в Android Studio.
5. Выполните сборку (`Build` -> `Make Project`).

## ⚙️ Конфигурация

Приложение использует официальный Client Secret для доступа к методам аудио и расширенным функциям сообщений. Настройки API находятся в `di/NetworkModule.kt` и `data/repository/AuthRepository.kt`.

---

*Reforged with ❤️ for the VK community.*
