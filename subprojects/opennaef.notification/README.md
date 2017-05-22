openNaEF Notification
=====================
NaEFのDtoChangesを受け取り、伝搬する。

## 概要

opennaef.notifier は NaEFデータへ何かしらの変更が発生した時に、その変更を通知するものです。

通知の受け取り側は、必要な通知のみを受け取るために、変更があったオブジェクトタイプ(node, port, etc...)ごとにフィルターをかけることができます。

## Webhooks
Webhook を使用して NaEF への変更を受け取ることができます。
変更が発生すると、登録された URL に対して HTTP POST で変更内容を送信します。

Webhook 通知を受け取る URL を登録するには、HTTP POST リクエストを送信する必要があります。
また、Webhook 通知を受けるアプリケーションは、ping リクエストに対して `200 OK` を返す必要があります。
```
POST /opennaef.notification/hooks HTTP/1.1
Content-Type: application/json
{
  "callback_url": "http://example.com/callback-url",
  "filter": {}
  "active": true
}
```

| name         | description                           |
|--------------|---------------------------------------|
| callback_url | 通知を受ける URL                       |
| filter       | [filter format](#filter%20format)     |
| active       | false を指定した場合は通知を一時停止する |

NaEF変更内容の通知は3回までリトライされます。通知に失敗した場合は、`"active": false` が設定され、通知を一時停止します。
通知を再開する場合は、hook変更 API で`"active": true` を指定してください。

[Webhooks](./Webhooks.md)

#### ping request
Callback URLの登録時、変更時には以下のフォーマットの Ping リクエストを送信します。
Notificationを受け取るアプリケーションはこのリクエストに対して `200 OK` を返す必要があります。
```
POST http:///callback_url/
Content-Type: application/json
{
  "type": "ping",
  "hook": {
    "callback_url": "http://example.com/callback-url",
    "filter": {}
    "active": true
  }  
}
```

## WebSocket API
- `notifier/commit`

WebSocket で上記のURLへ接続します。

フィルターはフィルターフォーマットに従ってnotifierへWebSocketでメッセージを送ります。

## filter format
- 変更があったオブジェクトの集合が指定したタイプのみである(turned A)

`{"for_all": ["object_type"]}`

- 変更があったオブジェクトの集合に指定したタイプが1つでも含まれる(turned E)

`{"exists": ["object_type"]}`

## Events
| name   | description                |
|:-------|:---------------------------|
| commit | NaEFに変更があったことを通知 |
| ping   | ping                       |

## Payloads
```
{
  "type": "commit"
  "notify_time": 1483196400000,
  "target_version": "wffff",
  "target_time": 12345,
  "uri": "http://localhost:2510/api/v1/dto-changes?version=wffff&time=12345",
  "dto_changes": {}
}
```

- notify_time: opennaef.notification から通知を出した時刻
- target_version: NaEF の Transaction Id
- target_time: NaEF の Transaction へ設定した時刻
- uri: NaEF Restful API DtoChanges 取得API の URI
- dto_changes: NaEF Restful API DtoChanges 取得APIから取得できるものと同値

## jvm args
| name                    | description      |
|:------------------------|:-----------------|
| notification.dir        | required         |
| derby.stream.error.file | derby error log  |