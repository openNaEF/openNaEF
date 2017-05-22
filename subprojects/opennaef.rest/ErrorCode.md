# error code 一覧
PREFIX-XXYZZ

- PREFIX: 分類
- XX: 発生場所特定用
- Y: HttpStatusCode
- ZZ: 連番

## prefix
- HTTP一般エラー: HTTP-YYY
- rest api系: API-00YZZ
- config系: API-01YZZ
- BuildHelper系: API-02YZZ
- Derby系: API-03YZZ
- NAEF(coreapp系): NAEF-00YZZ
- DEBUG: DEBUG-00000

## error code
- API-00500
  レスポンス送信失敗.
  
- API-00501
  一般エラー.
  catchしきれなかったExceptionに対するエラー

- API-01500
  設定ファイルエラー.

- API-02400
  入力値が不正です.

- API-02401
  パスが不正です.

- API-02402
  不正なフォーマットです.

- API-02500
  コマンドの生成に失敗しました.

- API-02501
  型定義が一致しませんでした.

- API-03500
  DB通信エラー.

- NAEF-00400
  更新失敗.

- NAEF-00410
  不正なIDです.
  
- NAEF-00500
  NAEF通信エラー.

- DEBUG-0000
  DEBUGエラー.