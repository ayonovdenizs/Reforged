# API Пользовательских Бейджей — VTL Reforged

Документация для интеграции системы бейджей в мод ВКонтакте.

---

## Обзор

Система бейджей позволяет отображать у пользователей мода ВК специальные значки, назначенные администратором на сервере. Клиент **не может** назначать или подделывать бейджи — они выдаются только через админ-панель.

Пользователи мода ВК — **отдельная сущность** (`VKUser`), не связанная с пользователями сайта. Идентификация происходит по **VK ID**.

## Типы бейджей

| type | slug              | label               | icon     | priority |
|------|-------------------|---------------------|----------|----------|
| 0    | `verified_donator`| Проверенный донатер | `verified` | 2      |
| 228  | `prometheus`      | Прометей            | `fire`    | 3      |
| 404  | `developer`       | Разработчик         | `code`    | 1      |

**Приоритет:** чем меньше число `priority`, тем выше приоритет бейджа. Порядок отображения: `developer` → `verified_donator` → `prometheus`.

---

## Эндпоинт

### GET `/api_vtlr/users/{vk_id}/badges/`

Возвращает список бейджей, назначенных пользователю мода ВК.

**Параметры:**
- `vk_id` (int) — ID пользователя ВКонтакте.

**Успешный ответ — `200 OK`:**

```json
{
  "status": "success",
  "data": {
    "vk_id": 123456789,
    "badges": [
      {
        "type": 404,
        "slug": "developer",
        "label": "Разработчик",
        "icon": "code",
        "priority": 1
      },
      {
        "type": 0,
        "slug": "verified_donator",
        "label": "Проверенный донатер",
        "icon": "verified",
        "priority": 2
      }
    ]
  }
}
```

**Пользователь не найден — `404 Not Found`:**

```json
{
  "status": "error",
  "message": "User not found"
}
```

**Пользователь без бейджей — `200 OK` с пустым массивом:**

```json
{
  "status": "success",
  "data": {
    "vk_id": 987654321,
    "badges": []
  }
}
```

---

## Поля бейджа

| Поле      | Тип    | Описание                                        |
|-----------|--------|-------------------------------------------------|
| `type`    | int    | Числовой идентификатор типа бейджа (0/228/404)  |
| `slug`    | string | Уникальный строковый идентификатор              |
| `label`   | string | Человекочитаемое название                       |
| `icon`    | string | Идентификатор иконки для отображения            |
| `priority`| int    | Приоритет (меньше = выше)                       |

---

## Рекомендации по отображению

### Иконки по `slug`:

| slug               | Рекомендуемая иконка |
|--------------------|----------------------|
| `verified_donator` | Синяя галочка (✓)    |
| `prometheus`       | Огонь (🔥)           |
| `developer`        | Код (`</>`)          |

### Сортировка:
Бейджи уже возвращаются отсортированными по `priority` (по возрастанию). Отображайте их в том порядке, в котором они приходят в ответе.

### Кэширование:
Рекомендуется кэшировать ответ на клиенте (например, на 5–10 минут), так как бейджи меняются редко.

---

## Примеры

### Python (requests)

```python
import requests

def get_user_badges(vk_id: int, base_url: str = "https://your-site.com") -> list[dict]:
    response = requests.get(f"{base_url}/api_vtlr/users/{vk_id}/badges/")
    if response.status_code == 404:
        return []
    response.raise_for_status()
    return response.json()["data"]["badges"]
```

### JavaScript (fetch)

```javascript
async function getUserBadges(vkId) {
  const response = await fetch(`/api_vtlr/users/${vkId}/badges/`);
  if (response.status === 404) return [];
  if (!response.ok) throw new Error(`API error: ${response.status}`);
  const data = await response.json();
  return data.data.badges;
}
```

---

## Безопасность

- Бейджи назначаются **только** администратором через админ-панель.
- Публичный API доступен без авторизации (только чтение).
- Клиент не может изменить или добавить бейджи через API.
- В ответе возвращаются только безопасные поля: `type`, `slug`, `label`, `icon`, `priority`.