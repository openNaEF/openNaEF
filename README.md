# OpenNaEF

## https://www.opennaef.io/

# Overview
openNaEFは、時間軸ネットワークトポロジ マネジメントフレームワークです。
本バージョンはαバージョンにての公開です。

# 特徴
- 既存ネットワークテクノロジやプロトコルはすべてモデル化済み

- すべてのネットワークモデルオブジェクトは2次元の時間軸を持ち、過去から将来予定までのネットワークトポロジのライフサイクル管理が可能です

- Restful APIやCLIでネットワークオブジェクトを操作可能

- 各種のサービスプロバイダやネットワークオペレータにて15年以上利用されてきたフレームワークのオープンソースでの公開となります

# スケジュールベースのネットワークトポロジ管理とは

履歴×スケジュールの「2次元時間軸」管理機能によってネットワークインフラに関わる将来の設計や過去の状況の追跡が可能になりネットワークサービスのライフサイクル管理に大いに役にたつものとなっています。

## スケジュール時間軸
- オブジェクトにスケジュールと関連づいた属性・参照を持たせることができます。
- 「今こうなっている」情報だけではなく「今こうなっていて、12/01 にはこう変わり、さらに 12/15 にはまたこう変わる」という将来の予定管理も行うことが可能です。

## 操作時間軸
- すべてのオブジェクトは変更履歴を保持しており、いつ、誰がデータを変更したのかを容易に追跡できます。 スケジュールと変更履歴の両方をネイティブでサポートする DB は openNaEF のものが唯一です。

# 時間軸管理の重要性
通常のTSDBとの違いは、TSDBは起こった事象の時間軸管理、openNaEFは将来予定の管理が可能（通常のTSDBはモニタリングやログ。openNAEFは設計とトポロジのライフサイクル管理が主目的）

- ネットワークオペレータの業務にはリソース予約等の将来予定の管理が重要
- ネットワーク設計の自動化する際、設計のバリデーションにはネットワークの構成情報が必要。
- 将来予定を設計するには予定を作る際の事前条件が必要となる。
- 将来予定は、事前の将来予定のネットワークトポロジから制約を受ける
- 将来予定の変更には、影響する将来予定が存在する

# 大多数のネットワークプロトコルをモデル化
- LAG のような論理ポート, PseudoWire, VLAN, VPLS, VRF,  IP-Subnet などのネットワークや VXLAN, EVPN, TRILL などの最新技術までをサポートすることが可能

- 基本要素としては、Node, Hardware, ProtocolPort (if), Network, Link, Network ID Pool からなり、その関係は Cross Connection, Containment, Stack, Owner (Children), Alias から構成されています。

- この構成要素を適切に組み合わせることで、ほぼすべてのネットワークプロトコルをモデル化することができます。IP, VLAN, MPLS のような一般的なネットワークのみならず、Q-in-Q, Overlay, Virtual Router も表現可能です。

- 構成要素は本質的で実際の実装経験に裏打ちされているため、現時点で知られているすべてのネットワークが表現可能です。そのため新たなプロトコルが登場したとしても, 既存プロトコルと同じ方法で扱え、プロトコルサポートを追加するのが容易になっています。

# openNaEF操作インターフェース
- DTO(Data Transfer Object)を利用しRPC経由
- restful APIによる操作、
- NaEFシェルを利用したコマンドラインインターフェースによる操作

# openNaEF付属アプリケーション
- openNaEF topology-viewer

- openNaEF inventory-web


# openNaEF Lifecycleサポートパッケージ
- Discovery Package(TBD)

- Activation Package(TBD)

- Workflow Package(TBD)

# オープンソースの実例システム
- OkinawaOpenLab TestBed Orchestrator

# Author
 https://www.opennaef.io/about

# QuickStart Guide

近日公開いたします

# Building from Source

近日公開いたします


# Licence
This software is licensed under the Apache License, version 2 ("ALv2"), quoted below.

Copyright 1999-2017 http://www.iiga.jp https://www.opennaef.io

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.

