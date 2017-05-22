# Webhooks

## list hooks
```
GET /hooks
```
#### response
```
HTTP/1.1 200 OK
Content-Type:application/json
[
  {
    "active": true,
    "callback_url": "http://callback_url",
    "failed": false,
    "id": 1
  }
]
```

## Get single hook
```
GET /hooks/:id
```
#### response
```
HTTP/1.1 200 OK
Content-Type:application/json
{
  "active": true,
  "callback_url": "http://callback_url",
  "failed": false,
  "id": 1
}
```

## Create a hook
```
POST /hooks
```
登録前に `callback_url` に対して ping リクエストが送信されます。
ping リクエストで `200 OK` が得られない場合は `400 Bad Request` が返されます。

#### parameters
| name         | type    | description                           |
|--------------|---------|---------------------------------------|
| callback_url | string  | 通知を受ける URL                       |
| filter       | object  |[filter format](#filter%20format)      |
| active       | boolean |false を指定した場合は通知を一時停止する  |

#### example
`node` に対する変更があった場合に `http://callback_url` へ通知する 
```
{
  "callback_url": "http://callback_url",
  "filter": {"for_all": ["node"]},
  "active": true
}
```

#### response
```
HTTP/1.1 201 Created
Content-Type:application/json
Location:http://lopennaef.notifier/hooks/1
{
  "id": 1,
  "callback_url": "http://callback_url",
  "filter": {"for_all": ["node"]},
  "active": true,
  "failed": false
}
```

## Edit a hook
```
PATCH /hooks/:id
```
`callback_url` が変更されるか、`"active": true` を指定した場合は ping リクエストが送信されます。
ping リクエストで `200 OK` が得られない場合は `400 Bad Request` が返されます。

#### parameters
| name         | type    | description                           |
|--------------|---------|---------------------------------------|
| callback_url | string  | 通知を受ける URL                       |
| filter       | object  |[filter format](#filter%20format)      |
| active       | boolean |false を指定した場合は通知を一時停止する  |

#### example
通知を一時停止する
```
{
  "active": false
}
```

#### response
```
HTTP/1.1 200 OK
Content-Type:application/json
{
  "id": 1,
  "callback_url": "http://callback_url",
  "filter": {"for_all": ["node"]},
  "active": false,
  "failed": false
}
```

## Delete a hook
```
DELETE /hooks/:id
```

#### response
```
204 No Content
```

## Ping a hook
```
POST /hooks/:id/pings
```
callback_url に対して ping リクエストを送信します。
ping に成功した場合は `204 No Content` を返します。
失敗した場合は `400 Bad Request` が返されます。

#### response
```
204 No Content
```