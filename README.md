# MYgram

Форк [Telegram for Android](https://github.com/DrKLO/Telegram) с собственной системой плагинов, кастомными настройками и пасхалками.

MYgram — это мессенджер на базе кода Telegram со встроенным плагин-движком: плагины (`.myp`) можно ставить из диалогового окна «MYgram Plugins» и расширять клиент без пересборки — обрабатывать сообщения, показывать собственные окна поверх интерфейса, обращаться к сети и хранить состояние.

> ⚠️ Это неофициальный форк. Не используйте название и логотип Telegram как свои.

## Возможности

- **Плагины (`.myp`)** — JS-движок (QuickJS через JNI) + песочница хуков и прав
- **MYgram Settings** — кастомные настройки клиента (вкладка в главных настройках)
- Плавающие UI-панели плагинов поверх всего приложения (`m.ui`)
- Пасхалка в стиле Android (7 тапов по «MyGram Ver»)
- Брендинг MYgram: имя, иконки, версия (`MYGRAM_VERSION_STRING`)

## Сборка

Требуется Android Studio 2025.1.4, Android NDK 27.2.12479018 и Android SDK 36.

1. Склонируйте репозиторий с подмодулями:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/OsDEev/Mygram.git
   ```
2. Замените `release.keystore` в `TMessagesProj/config` (пароли — в `gradle.properties`).
3. Создайте приложения `org.telegram.messenger` и `org.telegram.messenger.beta` в Firebase и положите `google-services.json` рядом с `TMessagesProj`.
4. Заполните API-ключи в `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java`.
5. Откройте проект в Android Studio «Open», соберите `TMessagesProj` (debug).

Gradle/JDK: в `gradle.properties` включён `org.gradle.java.installations.auto-download=false`, задание JDK — через `JAVA_HOME`.

## Плагины

Плагин — это ZIP-архив с расширением `.myp`:

```
my_plugin.myp
├── manifest.json       # обязательно (id, name, permissions, entry)
├── index.js            # entry (по умолчанию index.js)
├── helper.js           # загружается до entry (общие функции)
├── utils/*.js          # загружаются по алфавиту (утилиты)
└── locales/ru.json     # локализации
```

### manifest.json

```json
{
  "id": "com.example.my_plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "author": "You",
  "entry": "index.js",
  "permissions": ["MODIFY_OUTGOING_MESSAGES", "READ_MESSAGES", "LIFECYCLE", "NETWORK", "UI"]
}
```

Допустимые разрешения: `MODIFY_OUTGOING_MESSAGES`, `READ_MESSAGES`, `LIFECYCLE`, `NETWORK`, `STORAGE`, `UI`.
Права выдаются на время работы плагина и снимаются при его отключении/удалении.

### JS API

Контекст плагина (`m`):

- `m.log(msg)` / `m.warn(msg)` / `m.error(msg)` — логирование
- `m.notify(title, text)` — системное уведомление (Toast)
- `m.httpGet(url, callback(status, text))` — GET-запрос (нужно `NETWORK`)
- `m.getState()` / `m.setState(obj)` — персистентное состояние
- `m.ui.open({title, html, width, height, x, y, visible})` → `id` панели (нужно `UI`)
- `m.ui.update(id, options)` / `m.ui.close(id)`
- Вспомогательные: `allow()`, `block(message)`, `mutate(text)` и `getData()` (данные текущего хука)

### Хуки

Эти функции вызываются из клиента (если имя существует и есть нужное право):

| Хук | Событие | Право |
| --- | --- | --- |
| `onStartup` | запуск клиента | `LIFECYCLE` |
| `onSettingsOpen` | открыты настройки | `LIFECYCLE` |
| `onChatOpen` | открыт диалог `{chatId}` | `LIFECYCLE` |
| `onInputChanged` | ввод текста `{text, chatId}` | `READ_MESSAGES` |
| `onSendMessage` | отправка сообщения `{text, chatId}` | `MODIFY_OUTGOING_MESSAGES` |
| `onOutgoingPrepared` | подготовка исходящего | `MODIFY_OUTGOING_MESSAGES` |
| `onReceiveMessage` | входящее сообщение `{text, chatId}` | `READ_MESSAGES` |

Синхронные хуки могут вернуть `{action: "block", message}` или `{action: "mutate", text}` (либо использовать `block()`/`mutate()`).

### Пример

Готовый демо-плагин лежит в [`samples/mygram_test`](samples/mygram_test) (`mygram_test.myp`). Базовые примеры: [`sample-plugin`](sample-plugin), [`test-plugin`](test-plugin). Команды демо в тексте сообщения:

- `#test` — добавить `[+MYgram test]` к исходящему
- `#upper` — отправить ВЕРХНИМ регистром
- `#block` — заблокировать сообщение
- `#mark` — пометить входящее `[Y]`

## Лицензии

Код основан на [Telegram for Android](https://github.com/DrKLO/Telegram) (GPLv2). Плагин-движок MYgram распространяется в рамках исходного кода репозитория.