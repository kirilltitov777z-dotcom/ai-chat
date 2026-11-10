# LotiChat

Нативное Android-приложение для чата с бесплатным DeepSeek. Полностью на **Kotlin + Jetpack Compose**, интерфейс на **Material You 3** (динамические цвета с обоев).

**Скачать APK:** [AI-Chat.apk](https://github.com/kirilltitov777z-dotcom/ai-chat/releases/download/apk/AI-Chat.apk)

## Что умеет

- Чат с ИИ онлайн: OpenRouter (бесплатные модели DeepSeek) или официальный DeepSeek API, плюс свой сервер (LM Studio, Ollama)
- Material You 3: динамическая палитра Android 12+, светлая/тёмная/системная тема, 6 акцентных цветов
- Стриминг ответа, кнопка «стоп», повтор ответа, длинная история чатов с поиском по чатам в меню
- Две маленькие кнопки у отправки: **Мышление** (DeepSeek R1 + блок рассуждений) и **Поиск** (Wikipedia + DuckDuckGo)
- Вложения: камера, галерея, любые файлы. Картинки отправляются как изображения, текстовые файлы — как текст
- Если модель без зрения, картинки автоматически не отправляются и запрос повторяется
- Markdown в ответах: код, списки, заголовки, цитаты, ссылки
- Настройки: ключ, модель, список моделей с сервера, системный промпт, температура, тема, акцент
- Долгие ответы не обрываются: таймауты отключены
- Данные хранятся только на телефоне, экспорт истории в JSON

## Сборка

```bash
gradle -p android-app assembleRelease
```

APK: `android-app/app/build/outputs/apk/release/app-release.apk`

Сборка автоматическая в GitHub Actions (`.github/workflows/build.yml`) и сразу публикуется в релиз `apk`.

## Структура

```
android-app/app/src/main/java/io/margy/aichat/
├── MainActivity.kt
├── data/        модели, настройки, хранение чатов
├── net/         ChatApi (SSE-стриминг), WebSearch
├── ui/          экраны, тема Material 3, markdown, ViewModel
└── util/        обработка файлов и фото
```

Подпись: `android-app/lotichat.jks` — чтобы обновления ставились поверх без удаления приложения.
