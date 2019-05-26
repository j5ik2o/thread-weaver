class: animation-fade impact

.top-bar[

]
.bottom-bar[
  ScalaMatsuri 2019
]

# AWS EKSとAkkaを使って
# EventnSourcingを作るにはどうしたらよいか

ScalaMatsuri 2019

かとじゅん(@j5ik2o)

.center[<img src="images/logo-hz.png" width="20%">]

---
title: AWS EKSとAkkaを使ってEvent Sourcingを作るにはどうしたらよいか
class: animation-fade
layout: true

<!-- This slide will serve as the base layout for all your slides -->

.top-bar[
  {{title}}
]
.bottom-bar[
  ScalaMatsuri 2019
]

---

# 自己紹介

.col-6[
- Chatwork テックリード
- github/j5ik2o
    - [scala-ddd-base](https://github.com/j5ik2o/scala-ddd-base)
    - [scala-ddd-base-akka-http.g8](https://github.com/j5ik2o/scala-ddd-base-akka-http.g8)
    - [reactive-redis](https://github.com/j5ik2o/reactive-redis)
    - [reactive-memcached](https://github.com/j5ik2o/reactive-memcached)
- 翻訳レビュー
    - [エリックエヴァンスのドメイン駆動設計](https://amzn.to/2PmEHuU)
    - [Akka実践バイブル](https://amzn.to/2Qx54uU)
]

.col-6[
.center[<img src="images/self-prof.png" width="50%">]
]

---

# 最近の発表ネタ

1. [ドメインモデリングの始め方](https://speakerdeck.com/j5ik2o/tomeinmoterinkufalseshi-mefang) - AWS Dev Day Tokyo 2018
    - ドメインオブジェクトの発見・実装・リファクタリングの方法論をカバー
1. [Scalaでのドメインモデリングのやり方](https://speakerdeck.com/j5ik2o/scaladefalsedomeinmoderingufalseyarikata) - Scala関西Summit 2018
    - 1.のスライドと同様の観点だが、より実装技法寄りの議論をカバー

---

# アジェンダ

- 同じネタはやりません
    - 過去のネタについて議論したいなら、懇親会で捕まえてください！

1. ドメインイベントを使ったモデリングと実装
1. 集約を跨がる整合性の問題

---

# まとめ

- ドメインイベントは、ドメインの分析と実装の両方で使えるツール
- 集約を跨がる整合性の問題は難しいが、解決方法がないわけではない

---
class: impact

# 一緒に働くエンジニアを募集しています！

## http://corp.chatwork.com/ja/recruit/

.center[<img src="images/logo-vt.png" width="20%">]
