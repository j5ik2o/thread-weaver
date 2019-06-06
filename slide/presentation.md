class: animation-fade impact

.top-bar[

]
.bottom-bar[
  ScalaMatsuri 2019
]

# How to build an Event-Sourcing system Ho
# using Akka with EKS

ScalaMatsuri 2019

Junichi Kato(@j5ik2o)

.center[<img src="images/logo-hz.png" width="20%">]

---
title: How to build an Event-Sourcing system using Akka with EKS
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

# Who am I

.col-6[
- Chatwork Tech-Lead
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

# Agenda

1. Event Sourcing with Akka
2. Deployment to EKS

.bottom-bar[
Akka
]

???
今回のアジェンダはこのとおりです。
まずAkkaでのEventSourcingのやりかたについて
そのあとはEKSへのデプロイについて話します。

---
class: impact

# Akka with Event Sourcing

---

# Event Sourcing

- The latest state is derived by the events
- For example, transactions such as the e-commerce are sourced on events. This is nothing special.
- An event sequence represents an immutable history.
    - The transaction makes the following unique corrections. Events are never modified or deleted.
    - The order #0001 is canceled at the #0700, and the corrected data is registered at the slip #0701.
  
.center[<img src="images/real-events.png" width="80%">]

???
- イベントソーシングとは何か？
- 最新状態をイベントによって導出することです。そのためにできることはすべて定義に入ります。
- たとえば、Eコマースにおける取引はイベントを基にしています。これは特別なことではありません。
- あるイベント列はある不変な歴史を表します
  - 取引は以下のユニークな訂正を行います。イベントは決して変更されたり削除されたりしません(イベントを巻き上げる以外は)
  - 注文 1番を700番でキャンセルし、701番で修正データを登録します 

---

# Domain Events

.col-6[
- Events that occurred in the past
- Domain Events are events that domain experts is interested in
- Generally, Domain Events is expressed as a verb in past tense
  - CustomerRelocated
  - CargoShipped
]
.col-6[
- Events and commands are similar, but different languages ​​are handled by humans
  - Command may be rejected
  - Indicates that the event has already occurred
]  
.center[<img src="images/event-stream.png" width="80%">]

???
EventSourcingはイベントをモデリングの主軸に起きます。つまりドメインで起こる過去の出来事にフォーカスします。
ドメインイベントはドメインエキスパートが関心を持つ出来事です。一般にドメインイベントは動詞の過去形で表現されます。
たとえば、顧客の引越や貨物の出荷など

---
class: impact

# Consider thread-weaver 
# as an example of a simple chat application.

???
ここからは論よりコード。シンプルなチャットアプリケーションの事例として
thread-weaverという架空プロジェクトで話しを進めます。

---

# System requirements

- API server accepts commands and queries from API clients
- Create a thread to start the chat
- Only members can post to threads
- Only text messages posted to threads
- Omit authentication and authorization for convenience

???
システム要件について
チャットはチャットメッセージをサーバとクライアントの間でやりとりできるようにします。
APIサーバはAPIクライアントからのコマンドとクエリを受け付けます
チャットを開始するためにスレッドを作ります。
スレッドに投稿できるのはメンバーだけです。
スレッドにはテキストメッセージをポストできます。
便宜上、認証・認可は省きます

---

# System Configuration

.col-6[
.center[<img src="images/syste-diagram.svg">]
]

.col-6[
- Split the application into the command stack and the query stack
- The command is sent to (clustered sharding) aggregate actor
- The aggregate actor stores(appends) domain events in storage when it accepts a command
- RMU(cluster sharding ) starts up in conjunction with the aggregation actor and reads the domain events for the appropriate aggregate ID immediately after startup, executes the SQL, and creates the Read-Model
- Query using DAO to load and return the lead model
- Deploy the api-server as a kubernetes pod
]

???
システム構成です。
- アプリケーションをコマンドスタックとクエリスタックに分割します
- コマンドはクラスターシャーディングされた集約アクターに送信されます
- 集約アクターはコマンドを受け付けるとストレージにドメインイベントを書き込みます
- RMU（クラスタ共有）は集約アクタと連携して起動し、起動直後に適切な集約IDのドメインイベントを読み取り、SQLを実行してRead-Modelを作成します
- DAOを使用してリードモデルをロードして返します
- api-serverをkubernetesポッドとしてデプロイします。

---

class: impact

# Command stack side

???
それでは詳細にコマンド側をみていきましょう


---

# Domain Objects

.col-6[
- Account
    - Account information identifying the user of the system
- Thread
    - Indicates a place to exchange Messages
- Message
    - A hearsay written in some language
]
.col-6[
- Administrator
  - Administrator of the Thread
- Member
  - Users of the Thread
]
.center[<img src="images/domain-models.svg" width="80%">]

???
- アカウント
  - システムのユーザーを識別するアカウント情報
- スレッド
  - メッセージを交換する場所を示します
- メッセージ
  - ある言語で書かれた伝聞
- 管理者
  - スレッドの管理者
- メンバー
  - スレッドのユーザ

---

# Commands/Domain Events

ThreadEvent sub types

- Create/Destroy Thread
    - ThreadCreated
    - ThreadDestroyed
- Join/Leave AdministratorIds
    - AdministratorIdsJoined
    - AdministratorIdsLeft
- Join/Leave MemberIds
    - MemberIdsJoined
    - MemberIdsLeft
- Add/Remove Messages
    - MessagesAdded 
    - MessagesRemoved

???
コマンドとドメインイベントについて。
以下のような命令に対応して、ドメインイベントが発生します。
ドメインイベントにフォーカスすると振る舞いとしてのコマンドが見えます。
これをそのままドメインの振る舞いとして実装しましょう。
管理者IDをINSERTしたりDELETEしたりというCUDの言葉より、JoinやLeaveという動詞を使う方が貧血症を回避できるでしょう。

---

# Layered architecture

.col-6[
- Clean Architecture
- Common
    - interface-adaptors
    - infrastructure
- Command side
    - use-cases
    - domain
- Query side
    - data access streams
    - data access objects
] 
.col-6[
.center[<img src="images/clean-architecture.jpeg" width="100%">]
]

???
ドメインを隔離するために、なんらかのレイヤー化アーキテクチャを使いましょう。
ここではクリーンアーキテクチャを採用しています。詳しい話は 藤井さんの実践 Clean Architecture のセッションを聞いてみてください。
共通のレイヤーはインターフェイスアダプタ層とインフラストラクチャ層です。
コマンドサイドはドメイン層とユースケース層です。
クエリサイドは非正規データにアクセスするためのDAOとそのストリームラッパーがあります。

---

# Projects structure

.center[
<object type="image/svg+xml" data="images/modules.svg" width="900"></object>
]

???
プロジェクト構造はこのようになります。contractsというモジュールはプロトコルと契約としてのインターフェイスのみを定義しています。
modulesというものは、実装が含まれます。依存の方向性は循環しないようになっています。

---

# Domain objects with actors

.col-8[
.center[
<object type="image/svg+xml" data="images/actor-tree.svg" ></object>
]
]
.col-4[
- Actors that fulfill all the functions are undesirable
- Follow object-oriented principles to build a hierarchy of actors with a single responsibility
]

???
次にドメインオブジェクトとアクターの関係性を示した図を説明します。
まずはじめにすべての機能を満たすアクターは必要ありません。
オブジェクト指向の原則に従って、単一の責任でアクターの階層を構築しましょう

---

# Thread

```scala
trait Thread {

  def isAdministratorId(accountId: AccountId): Boolean
  def isMemberId(accountId: AccountId): Boolean

  def joinAdministratorIds(value: AdministratorIds, senderId: AccountId, at: Instant): Result[Thread]
  def leaveAdministratorIds(value: AdministratorIds, senderId: AccountId, at: Instant): Result[Thread]
  def getAdministratorIds(senderId: AccountId): Result[AdministratorIds]

  def joinMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Result[Thread]
  def leaveMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Result[Thread]
  def getMemberIds(senderId: AccountId): Result[MemberIds]

  def addMessages(values: Messages, at: Instant): Result[Thread]
  def removeMessages(values: MessageIds, removerId: AccountId, at: Instant): Result[(Thread, MessageIds)]
  def getMessages(senderId: AccountId): Result[Messages]

  def destroy(senderId: AccountId, at: Instant): Result[Thread]
}
```

???
- 今回のドメインのコアはThreadです。
- ユビキタス言語で表現される振る舞いのセットが定義されています。これらは副作用のない関数です。
---

# ThreadAggregate

.col-6[
```scala
class ThreadAggregate(id: ThreadId,
  subscribers: Seq[ActorRef]) extends Actor {
  // add messages handler
  private def commandAddMessages(thread: Thread): Receive = {
    case AddMessages(requestId, threadId,
      messages, createAt, reply) if threadId == id =>
      thread.addMessages(messages, createAt) match {
        case Left(exception) =>
          if (reply)
            sender() ! AddMessagesFailed(ULID(), requestId,
              threadId, exception.getMessage, createAt)
        case Right(newThread) =>
          if (reply)
            sender() ! AddMessagesSucceeded(ULID(), requestId,
              threadId, messages.toMessageIds, createAt)
          context.become(onCreated(newThread))
      }
  }
  
  override def receive: Receive = { /*...*/ }
}
```
]
.col-6[
- Actors that support transactional integrity
- The boundary of the data update is the same as the boundary the aggregates has.
- For example, when an actor receives the CreateThead command, a Thread state is generated internally
- Then Messages are also added to the Thread when the AddMessages command is receives
- If the other commands defined in the protocol are received by the Actor, the Actor will have corresponding side effects.
]

???
- トランザクションの整合性をサポートするアクター
- データ更新の境界は、集計値の境界と同じです。
- たとえば、アクターがCreateTheadコマンドを受け取ると、内部でThread状態が生成されます。
- その後、AddMessagesコマンドを受信したときにメッセージもスレッドに追加されます。
- プロトコルで定義された他のコマンドがアクターによって受信された場合、アクターは対応する副作用を持ちます。

---

# ThreadAggreateSpec

.col-6[
```scala
val threadId        = ThreadId()
val threadRef       = newThreadRef(threadId)
val now             = Instant.now
val administratorId = AccountId()
val title           = ThreadTitle("test")

threadRef ! CreateThread(ULID(), threadId, administratorId, 
  None, title, None, AdministratorIds(administratorId),
  MemberIds.empty, now, reply = false)

val messages = Messages(TextMessage(MessageId(), None, 
  ToAccountIds.empty, Text("ABC"), memberId, now, now))
threadRef ! AddMessages(ULID(), threadId, messages, 
  now, reply = true)

expectMsgType[AddMessagesResponse] match {
 case f: AddMessagesFailed =>
   fail(f.message)
 case s: AddMessagesSucceeded =>
   s.threadId shouldBe threadId
   s.createAt shouldBe now
}
```
]

???
- このようにメッセージパッシングを使ってテストを実装します

---

# PersistentThreadAggregate(1/2)

.col-6[
```scala
object PersistentThreadAggregate {
  def props(id: ThreadId, subscribers: Seq[ActorRef]): Props =
      ...
}

class PersistentThreadAggregate(id: ThreadId, 
  subscribers: Seq[ActorRef], 
  propsF: (ThreadId, Seq[ActorRef]) => Props)
    extends PersistentActor with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = 
    OneForOneStrategy() { case _: Throwable => Stop }

  private val childRef =
    context.actorOf(propsF(id, subscribers), 
      name = ThreadAggregate.name(id))
  context.watch(childRef)

  override def persistenceId: String = ThreadAggregate.name(id)

  override def receiveRecover: Receive = {
    case e: ThreadCommonProtocol.Event with ToCommandRequest =>
      childRef ! e.toCommandRequest
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }
```
]
.col-6[
- Actors that add the persistence function to ThreadAggregate
- Domain behavior is provided by child actors
- The recover process sends commands generated from events to child actors.
]

???
- ThreadAggregateに永続化機能を追加するアクターです
- ドメインの振る舞いは子アクターによって提供されます
- receiveRecover処理は、ドメインイベントから生成されたコマンドを子アクターに送信します

---

# PersistentThreadAggregate(2/2)

.col-6[
```scala
  override def receiveCommand: Receive = {
    case Terminated(c) if c == childRef =>
      context.stop(self)
    case m: CommandRequest with ToEvent =>
      childRef ! m
      context.become(sending(sender(), m.toEvent))
    case m =>
      childRef forward m
  }
  
  private def sending(replyTo: ActorRef, 
    event: ThreadCommonProtocol.Event): Receive = {
    case s: CommandSuccessResponse => persist(event) { _ =>
        replyTo ! s
        unstashAll()
        context.unbecome()
      }
    case f: CommandFailureResponse =>
      replyTo ! f
      unstashAll()
      context.unbecome()
    case _ =>
      stash()
  }
}
```
]

.col-6[
- Delegate to child actors when receiving commands. Persists only on success
- message processing is suspended until a command response is returned
]

???
- コマンドを受信したとき、子アクターに委譲します
- そのコマンドの応答が返されるまで、メッセージ処理は一時停止されます。

---

# PersitentThreadAggregateSpec

.col-8[
```scala
// Create id = 1 of Thread actor
threadRef1 ! CreateThread(ULID(), threadId, administratorId, None, title, None, 
  AdministratorIds(administratorId), MemberIds.empty, now, reply = false)
val messages = Messages(TextMessage(MessageId(), None, 
  ToAccountIds.empty, Text("ABC"), memberId, now, now))
threadRef1 ! AddMessages(ULID(), threadId, messages, now, reply = false)

//Stop id = 1 of Thread actor
killActors(threadRef)

// Recover id = 1 of Thread actor
val threadRef2 = system.actorOf(PersistentThreadAggregate.props(threadId, Seq.empty))

// Check if it is in the previous state
threadRef2 ! GetMessages(ULID(), threadId, memberId, now)
expectMsgType[GetMessagesResponse] match {
  case f: GetMessagesFailed =>
    fail(f.message)
  case s: GetMessagesSucceeded =>
    s.threadId shouldBe threadId
    s.createAt shouldBe now
    s.messages shouldBe messages
}
```
]
.col-4[
- a test that intentionally stops and restarts the persistence actor
- Replayed state after reboot
]

???
- 永続化アクターを意図的に停止して再起動するテスト
- 再起動後に状態をリプレイできます


---

# ThreadAggregates(Message Broker)

.col-6[
```scala
class ThreadAggregates(subscribers: Seq[ActorRef], 
    propsF: (ThreadId, Seq[ActorRef]) => Props)
    extends Actor
    with ActorLogging
    with ChildActorLookup {
  override type ID             = ThreadId
  override type CommandRequest = ThreadProtocol.CommandMessage
  
  override def receive: Receive = forwardToActor

  override protected def childName(childId: ThreadId): String = childId.value.asString
  override protected def childProps(childId: ThreadId): Props = propsF(childId, subscribers)
  override protected def toChildId(commandRequest: ThreadProtocol.CommandMessage): ThreadId = commandRequest.threadId
}
```
]
.col-6[
- The message broker that bundles multiple ThreadAggregates as child actors 
- Most of the logic is in ChildActorLookup
- Resolve the actor name from ThreadId in the command message, and transfer the message to the corresponding child actor.  If there is no child actor, generate an actor and then forward the message to the actor
]

???
- 複数のThreadAggregateを子アクターとして束ねるメッセージブローカー
- ほとんどのロジックはChildActorLookupにあります
- コマンドメッセージ内のThreadIdからアクター名を解決し、対応づく子アクターにメッセージを転送します。子アクターがいない場合は子アクターを生成してからメッセージを転送します。

---

# ChildActorLookup

.col-8[
```scala
trait ChildActorLookup extends ActorLogging { this: Actor =>
  implicit def context: ActorContext
  type ID
  type CommandRequest

  protected def childName(childId: ID): String
  protected def childProps(childId: ID): Props
  protected def toChildId(commandRequest: CommandRequest): ID

  protected def forwardToActor: Actor.Receive = {
    case _cmd =>
      val cmd = _cmd.asInstanceOf[CommandRequest]
      context
        .child(childName(toChildId(cmd)))
        .fold(createAndForward(cmd, toChildId(cmd)))(forwardCommand(cmd))
  }

  protected def forwardCommand(cmd: CommandRequest)(childRef: ActorRef): Unit =
    childRef forward cmd

  protected def createAndForward(cmd: CommandRequest, childId: ID): Unit = 
    createActor(childId) forward cmd

  protected def createActor(childId: ID): ActorRef =
    context.actorOf(childProps(childId), childName(childId))
}
```
]
.col-4[
- Create a child actor if none exists and forward the message
- forward the message to its child actors, if any
]

???
メッセージブローカはメッセージを転送しますが、内部で子アクターの生成も担当します。

---

# ShardedThreadAggregates (1/2)

.col-6[
```scala
object ShardedThreadAggregates {

  def props(subscribers: Seq[ActorRef], propsF: (ThreadId, Seq[ActorRef]) => Props): Props =
    Props(new ShardedThreadAggregates(subscribers, propsF))

  def name(id: ThreadId): String = id.value.asString

  val shardName = "threads"

  case object StopThread

　// function to extract an entity id
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: CommandRequest => (cmd.threadId.value.asString, cmd)
  }

  // function to extract a shard id
  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: CommandRequest =>
      val mostSignificantBits  = cmd.threadId.value.mostSignificantBits  % 12
      val leastSignificantBits = cmd.threadId.value.leastSignificantBits % 12
      s"$mostSignificantBits:$leastSignificantBits"
  }

}
```
]
.col-6[
- Allow ThreadAggregates to be distributed across a cluster
- extractEntityId is the function to extract an entity id
- extractShardId is the function to extract a shard id
]

???
-  ThreadAggregateをクラスタ全体に分散できるようにする
-  extractEntityIdはエンティティIDを抽出するための関数です
-  extractShardIdはシャードIDを抽出する関数です

---

# ShardedThreadAggregates (2/2)

.col-6[
```scala
class ShardedThreadAggregates(subscribers: Seq[ActorRef], 
  propsF: (ThreadId, Seq[ActorRef]) => Props)
    extends ThreadAggregates(subscribers, propsF) {
  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      log.debug("ReceiveTimeout")
      context.parent ! Passivate(stopMessage = StopThread)
    case StopThread =>
      log.debug("StopWallet")
      context.stop(self)
  }
}
```
]
.col-6[
- Inherit ThreadAggregates
- Then add an implementation to passivate ShardedThreadAggregates when occurred ReceiveTimeout 
]

???
- ShardedThreadAggregatesはThreadAggregatesを継承します
- 一定時間を過ぎたらアクターをランタイムから退避させる設定を追加します

---

# ShardedThreadAggregatesRegion

.col-6[
```scala
object ShardedThreadAggregatesRegion {

  def startClusterSharding(subscribers: Seq[ActorRef])
    (implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).start(
      ShardedThreadAggregates.shardName,
      ShardedThreadAggregates.props(subscribers, 
        PersistentThreadAggregate.props),
      ClusterShardingSettings(system),
      ShardedThreadAggregates.extractEntityId,
      ShardedThreadAggregates.extractShardId
    )

  def shardRegion(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system)
      .shardRegion(ShardedThreadAggregates.shardName)
}
```
]
.col-6[
- The startClusterSharing method will start ClusterSharing with the specified settings
- The shardRegion method gets the ActorRef to the started ShardRegion.
]

???
最後にクラスターシャーディングのための設定を追加します。
- startClusterSharingメソッドは指定した設定に基づいてクラスターシャーディングを開始します
- shardRegionメソッドは開始されたShardRegionへのActor参照を返します

---

# MultiJVM Testing

```scala
    "setup shared journal" in {
      Persistence(system)
      runOn(controller) { system.actorOf(Props[SharedLeveldbStore], "store") }
      enterBarrier("persistence-started")
      runOn(node1, node2) {
        system.actorSelection(node(controller) / "user" / "store") ! Identify(None)
        val sharedStore = expectMsgType[ActorIdentity].ref.get
        SharedLeveldbJournal.setStore(sharedStore, system)
      }
      enterBarrier("setup shared journal")
    }
    "join cluster" in within(15 seconds) {
      join(node1, node1) { ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty) }
      join(node2, node1) { ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty) }
      enterBarrier("join cluster")
    }
    "createThread" in { runOn(node1) {
        val accountId = AccountId(); val threadId  = ThreadId(); val title = ThreadTitle("test")
        val threadRef = ShardedThreadAggregatesRegion.shardRegion
        threadRef ! CreateThread(ULID(), threadId, accountId, None, title, None, AdministratorIds(accountId), 
          MemberIds.empty, Instant.now, reply = true)
        expectMsgType[CreateThreadSucceeded](10 seconds).threadId shouldBe threadId
      }
      enterBarrier("create thread")
    }
```

???
これはMultiJVMテストの例です。
akka-persistenceの初期化とクラスターメンバーのジョイン後に、スレッドを作成する処理を記述しています。
ローカルアクターにメッセージを送信するとの同じようにRPCを実現します。これは非同期でかつノンブロッキングです。

---

# cluster-sharding with persistence

.col-8[
.center[
<object type="image/svg+xml" data="images/akka-event-sourcing.svg" height="500"></object>
]
]
.col-4[
- Actors with state in on-memory are distributed across the cluster
- Domain events that occur are saved in partitioned storage by aggregate ID
]

???
cluster-shardingとpersistenceを振り返ります。
- 状態をオンメモリに保持したアクターはクラスター上に分散されます
- 発生したドメインイベントは、集約ID毎にパーティショニングされたストレージに追記保存されます

---

# CreateThreadUseCaseUntypeImpl

```scala
class CreateThreadUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef, parallelism: Int = 1, timeout: Timeout = 3 seconds
)(implicit system: ActorSystem) extends CreateThreadUseCase {
  override def execute: Flow[UCreateThread, UCreateThreadResponse, NotUsed] =
    Flow[UCreateThread].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      (threadAggregates ? CreateThread(
        ULID(), request.threadId, request.creatorId, None, request.title, request.remarks,
        request.administratorIds, request.memberIds, request.createAt, reply = true
      )).mapTo[CreateThreadResponse].map {
        case s: CreateThreadSucceeded =>
          UCreateThreadSucceeded(s.id, s.requestId, s.threadId, s.createAt)
        case f: CreateThreadFailed =>
          UCreateThreadFailed(f.id, f.requestId, f.threadId, f.message, f.createAt)
      }
    }
}
```

???
より複雑なユースケースではワークフローを制御しますが、このユースケースでは単純にスレッド集約にコマンドを送信し応答を待ちます。
また、プロトコルとしてのメッセージは変換します。

---

# ThreadCommandControllerImpl

```scala
trait ThreadCommandControllerImpl extends ThreadCommandController with ThreadValidateDirectives with MetricsDirectives {
  private val createThreadUseCase: CreateThreadUseCase     = bind[CreateThreadUseCase]
  private val createThreadPresenter: CreateThreadPresenter = bind[CreateThreadPresenter]

  override private[controller] def createThread: Route =
    path("threads" / "create") {
      post {
        extractMaterializer { implicit mat =>
          entity(as[CreateThreadRequestJson]) { json =>
            validateJsonRequest(json).apply { commandRequest =>
              val responseFuture = Source
                .single(commandRequest)
                .via(createThreadUseCase.execute)
                .via(createThreadPresenter.response)
                .runWith(Sink.head)
              onSuccess(responseFuture) { response =>
                complete(response)
              }
            }
          }
        }
      }
    }
```

---

# ThreadQueryControllerImpl

```scala
trait ThreadQueryControllerImpl extends ThreadQueryController with ThreadValidateDirectives with MetricsDirectives {
  private val threadDas: ThreadDas = bind[ThreadDas]
  // ...
  override private[controller] def getThread: Route =
    path("threads" / Segment) { threadIdString => get {
        extractExecutionContext { implicit ec =>
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              parameter('account_id) { accountValue =>
                validateAccountId(accountValue) { accountId =>
                  onSuccess(threadDas.getThreadByIdSource(accountId, threadId)
                    .via(threadPresenter.response)
                    .runWith(Sink.headOption[ThreadJson]).map(identity)) { 
                      case None => reject(NotFoundRejection("thread is not found", None))
                      case Some(response) => complete(GetThreadResponseJson(response)) 
                  }
                }
              }
            }
          }
        }
      }
    }
  // ...
}
```

---

# Bootstrap

```scala
object Main extends App {

  SLF4JBridgeHandler.install()

  implicit val logger = LoggerFactory.getLogger(getClass)
  val config: Config = ConfigFactory.load()

  implicit val system: ActorSystem                        = ActorSystem("thread-weaver-api-server", config)
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val cluster                                    = Cluster(system)
  logger.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  cluster.subscribe(
    system.actorOf(Props[ClusterWatcher]),
    ClusterEvent.InitialStateAsEvents,
    classOf[ClusterDomainEvent]
  )

  val readJournal = PersistenceQuery(system).readJournalFor[DynamoDBReadJournal](DynamoDBReadJournal.Identifier)
  val dbConfig    = DatabaseConfig.forConfig[JdbcProfile]("slick", config)
  val profile     = dbConfig.profile
  val db          = dbConfig.db

  val clusterSharding = ClusterSharding(system.toTyped)

  val host = config.getString("thread-weaver.api-server.host")
  val port = config.getInt("thread-weaver.api-server.http.port")

  val akkaHealthCheck = HealthCheck.akka(host, port)

  val design =
    DISettings.design(host, port, system.toTyped, clusterSharding, materializer, readJournal, profile, db, 15 seconds)
  val session = design.newSession
  session.start

  val routes = session
      .build[Routes].root ~ readinessProbe(akkaHealthCheck).toRoute ~ livenessProbe(akkaHealthCheck).toRoute

  val bindingFuture = Http().bindAndHandle(routes, host, port).map { serverBinding =>
    system.log.info(s"Server online at ${serverBinding.localAddress}")
    serverBinding
  }

  Cluster(system).registerOnMemberUp({
    logger.info("Cluster member is up!")
  })

  sys.addShutdownHook {
    session.shutdown
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}
```

---


# Kubernetes/EKSを学ぶ

- [Kubernetes公式サイト](https://kubernetes.io/ja/docs/home/)
- [Amazon EKS](https://docs.aws.amazon.com/ja_jp/eks/latest/userguide/what-is-eks.html)
- [Amazon EKS Workshop](https://eksworkshop.com/)


---

# まとめ

- ドメインイベントは、ドメインの分析と実装の両方で使えるツール
- 集約を跨がる整合性の問題は難しいが、解決方法がないわけではない

---
class: impact

# 一緒に働くエンジニアを募集しています！

## http://corp.chatwork.com/ja/recruit/

.center[<img src="images/logo-vt.png" width="20%">]
