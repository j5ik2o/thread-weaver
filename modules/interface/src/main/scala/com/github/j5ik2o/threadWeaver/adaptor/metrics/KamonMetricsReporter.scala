package com.github.j5ik2o.threadWeaver.adaptor.metrics

import com.github.j5ik2o.akka.persistence.dynamodb.metrics.MetricsReporter
import kamon.Kamon

class KamonMetricsReporter extends MetricsReporter {

  private val asyncWriteMessagesCallDuration     = Kamon.metrics.histogram("async-write-messages-call")
  private val asyncWriteMessagesCallCounter      = Kamon.metrics.counter("async-write-messages-call")
  private val asyncWriteMessagesCallErrorCounter = Kamon.metrics.counter("async-write-messages-call-error")

  override def setAsyncWriteMessagesCallDuration(value: Long): Unit = asyncWriteMessagesCallDuration.record(value)
  override def addAsyncWriteMessagesCallCounter(value: Long): Unit  = asyncWriteMessagesCallCounter.increment(value)
  override def addAsyncWriteMessagesCallErrorCounter(value: Long): Unit =
    asyncWriteMessagesCallErrorCounter.increment(value)

  private val asyncDeleteMessagesToCallDuration    = Kamon.metrics.histogram("async-delete-messages-to-call")
  private val asyncDeleteMessagesToCalCounter      = Kamon.metrics.counter("async-delete-messages-to-call")
  private val asyncDeleteMessagesToCalErrorCounter = Kamon.metrics.counter("async-delete-messages-to-call-error")

  override def setAsyncDeleteMessagesToCallDuration(value: Long): Unit = asyncDeleteMessagesToCallDuration.record(value)
  override def addAsyncDeleteMessagesToCallCounter(value: Long): Unit  = asyncDeleteMessagesToCalCounter.increment(value)
  override def addAsyncDeleteMessagesToCallErrorCounter(value: Long): Unit =
    asyncDeleteMessagesToCalErrorCounter.increment(value)

  private val asyncReplayMessagesCallDuration     = Kamon.metrics.histogram("async-replay-messages-call")
  private val asyncReplayMessagesCallCounter      = Kamon.metrics.counter("async-replay-messages-call")
  private val asyncReplayMessagesErrorCallCounter = Kamon.metrics.counter("async-replay-messages-call-error")

  override def setAsyncReplayMessagesCallDuration(value: Long): Unit = asyncReplayMessagesCallDuration.record(value)
  override def addAsyncReplayMessagesCallCounter(value: Long): Unit  = asyncReplayMessagesCallCounter.increment(value)
  override def addAsyncReplayMessagesCallErrorCounter(value: Long): Unit =
    asyncReplayMessagesErrorCallCounter.increment(value)

  private val asyncReadHighestSequenceNrCallDuration = Kamon.metrics.histogram("async-read-highest-sequence-nr-call")
  private val asyncReadHighestSequenceNrCallCounter  = Kamon.metrics.counter("async-read-highest-sequence-nr-call")
  private val asyncReadHighestSequenceNrErrorCallCounter =
    Kamon.metrics.counter("async-read-highest-sequence-nr-call-error")

  override def setAsyncReadHighestSequenceNrCallDuration(value: Long): Unit =
    asyncReadHighestSequenceNrCallDuration.record(value)
  override def addAsyncReadHighestSequenceNrCallCounter(value: Long): Unit =
    asyncReadHighestSequenceNrCallCounter.increment(value)
  override def addAsyncReadHighestSequenceNrCallErrorCounter(value: Long): Unit =
    asyncReadHighestSequenceNrErrorCallCounter.increment(value)

  private val putMessagesCallDuration     = Kamon.metrics.histogram("put-messages-call")
  private val putMessagesCallCounter      = Kamon.metrics.counter("put-messages-call")
  private val putMessagesCallErrorCounter = Kamon.metrics.counter("put-messages-call-error")

  override def setPutMessagesCallDuration(value: Long): Unit     = putMessagesCallDuration.record(value)
  override def addPutMessagesCallCounter(value: Long): Unit      = putMessagesCallCounter.increment(value)
  override def addPutMessagesCallErrorCounter(value: Long): Unit = putMessagesCallErrorCounter.increment(value)

  private val deleteMessagesCallDuration     = Kamon.metrics.histogram("delete-messages-call")
  private val deleteMessagesCallCounter      = Kamon.metrics.counter("delete-messages-call")
  private val deleteMessagesCallErrorCounter = Kamon.metrics.counter("delete-messages-call-error")

  override def setDeleteMessagesCallDuration(value: Long): Unit     = deleteMessagesCallDuration.record(value)
  override def addDeleteMessagesCallCounter(value: Long): Unit      = deleteMessagesCallCounter.increment(value)
  override def addDeleteMessagesCallErrorCounter(value: Long): Unit = deleteMessagesCallErrorCounter.increment(value)

  private val getJournalRowsItemDuration         = Kamon.metrics.histogram("get-journal-rows-item")
  private val getJournalRowsItemCounter          = Kamon.metrics.counter("get-journal-rows-item")
  private val getJournalRowsItemCallCounter      = Kamon.metrics.counter("get-journal-rows-item-call")
  private val getJournalRowsItemCallErrorCounter = Kamon.metrics.counter("get-journal-rows-item-call-error")

  override def setGetJournalRowsItemDuration(value: Long): Unit    = getJournalRowsItemDuration.record(value)
  override def addGetJournalRowsItemCounter(value: Long): Unit     = getJournalRowsItemCounter.increment(value)
  override def addGetJournalRowsItemCallCounter(value: Long): Unit = getJournalRowsItemCallCounter.increment(value)
  override def addGetJournalRowsItemCallErrorCounter(value: Long): Unit =
    getJournalRowsItemCallErrorCounter.increment(value)

  private val updateMessageCallDuration     = Kamon.metrics.histogram("update-message-call")
  private val updateMessageCallCounter      = Kamon.metrics.counter("update-message-call")
  private val updateMessageCallErrorCounter = Kamon.metrics.counter("update-message-call-error")

  override def setUpdateMessageCallDuration(value: Long): Unit     = updateMessageCallDuration.record(value)
  override def addUpdateMessageCallCounter(value: Long): Unit      = updateMessageCallCounter.increment(value)
  override def addUpdateMessageCallErrorCounter(value: Long): Unit = updateMessageCallErrorCounter.increment(value)

  private val putJournalRowsItemDuration         = Kamon.metrics.histogram("put-journal-rows-item")
  private val putJournalRowsItemCounter          = Kamon.metrics.counter("put-journal-rows-item")
  private val putJournalRowsItemCallCounter      = Kamon.metrics.counter("put-journal-rows-item-call")
  private val putJournalRowsItemErrorCallCounter = Kamon.metrics.counter("put-journal-rows-item-call-error")
  private val putJournalRowsCallDuration         = Kamon.metrics.histogram("put-journal-row-call")
  private val putJournalRowsCallCounter          = Kamon.metrics.counter("put-journal-rows-call")
  private val putJournalRowsCallErrorCounter     = Kamon.metrics.counter("put-journal-rows-call-error")

  override def setPutJournalRowsItemDuration(value: Long): Unit    = putJournalRowsItemDuration.record(value)
  override def addPutJournalRowsItemCounter(value: Long): Unit     = putJournalRowsItemCounter.increment(value)
  override def addPutJournalRowsItemCallCounter(value: Long): Unit = putJournalRowsItemCallCounter.increment(value)
  override def addPutJournalRowsItemCallErrorCounter(value: Long): Unit =
    putJournalRowsItemErrorCallCounter.increment(value)
  override def setPutJournalRowsCallDuration(value: Long): Unit     = putJournalRowsCallDuration.record(value)
  override def addPutJournalRowsCallCounter(value: Long): Unit      = putJournalRowsCallCounter.increment(value)
  override def addPutJournalRowsCallErrorCounter(value: Long): Unit = putJournalRowsCallErrorCounter.increment(value)

  private val deleteJournalRowsItemDuration         = Kamon.metrics.histogram("delete-journal-rows-item")
  private val deleteJournalRowsItemCounter          = Kamon.metrics.counter("delete-journal-rows-item")
  private val deleteJournalRowsItemCallCounter      = Kamon.metrics.counter("delete-journal-rows-item-call")
  private val deleteJournalRowsItemCallErrorCounter = Kamon.metrics.counter("delete-journal-rows-item-call-error")
  private val deleteJournalRowsCallDuration         = Kamon.metrics.histogram("delete-journal-rows-call")
  private val deleteJournalRowsCallCounter          = Kamon.metrics.counter("delete-journal-rows-call")
  private val deleteJournalRowsCallErrorCounter     = Kamon.metrics.counter("delete-journal-rows-call-error")

  override def setDeleteJournalRowsItemDuration(value: Long): Unit = deleteJournalRowsItemDuration.record(value)
  override def addDeleteJournalRowsItemCounter(value: Long): Unit  = deleteJournalRowsItemCounter.increment(value)
  override def addDeleteJournalRowsItemCallCounter(value: Long): Unit =
    deleteJournalRowsItemCallCounter.increment(value)
  override def addDeleteJournalRowsItemCallErrorCounter(value: Long): Unit =
    deleteJournalRowsItemCallErrorCounter.increment(value)
  override def setDeleteJournalRowsCallDuration(value: Long): Unit = deleteJournalRowsCallDuration.record(value)
  override def addDeleteJournalRowsCallCounter(value: Long): Unit  = deleteJournalRowsCallCounter.increment(value)
  override def addDeleteJournalRowsCallErrorCounter(value: Long): Unit =
    deleteJournalRowsCallErrorCounter.increment(value)

  private val highestSequenceNrItemDuration         = Kamon.metrics.histogram("highest-sequence-nr-item")
  private val highestSequenceNrItemCounter          = Kamon.metrics.counter("highest-sequence-nr-item")
  private val highestSequenceNrItemCallCounter      = Kamon.metrics.counter("highest-sequence-nr-item-call")
  private val highestSequenceNrItemCallErrorCounter = Kamon.metrics.counter("highest-sequence-nr-item-call-error")
  private val highestSequenceNrCallDuration         = Kamon.metrics.histogram("highest-sequence-nr-call")
  private val highestSequenceNrCallCounter          = Kamon.metrics.counter("highest-sequence-nr-call")
  private val highestSequenceNrCallErroCounter      = Kamon.metrics.counter("highest-sequence-nr-call")

  override def setHighestSequenceNrItemDuration(value: Long): Unit = highestSequenceNrItemDuration.record(value)
  override def addHighestSequenceNrItemCounter(value: Long): Unit  = highestSequenceNrItemCounter.increment(value)
  override def addHighestSequenceNrItemCallCounter(value: Long): Unit =
    highestSequenceNrItemCallCounter.increment(value)
  override def addHighestSequenceNrItemCallErrorCounter(value: Long): Unit =
    highestSequenceNrItemCallErrorCounter.increment(value)
  override def setHighestSequenceNrCallDuration(value: Long): Unit = highestSequenceNrCallDuration.record(value)
  override def addHighestSequenceNrCallCounter(value: Long): Unit  = highestSequenceNrCallCounter.increment(value)
  override def setHighestSequenceNrCallErrorCounter(value: Long): Unit =
    highestSequenceNrCallErroCounter.increment(value)

  private val getMessagesItemDuration         = Kamon.metrics.histogram("get-messages-item")
  private val getMessagesItemCounter          = Kamon.metrics.counter("get-messages-item")
  private val getMessagesItemCallCounter      = Kamon.metrics.counter("get-messages-item-call")
  private val getMessagesItemCallErrorCounter = Kamon.metrics.counter("get-messages-item-call-error")
  private val getMessagesCallDuration         = Kamon.metrics.histogram("get-messages-call")
  private val getMessagesCallCounter          = Kamon.metrics.counter("get-messages-call")
  private val getMessagesCallErrorCounter     = Kamon.metrics.counter("get-messages-call-error")

  override def setGetMessagesItemDuration(value: Long): Unit         = getMessagesItemDuration.record(value)
  override def addGetMessagesItemCounter(value: Long): Unit          = getMessagesItemCounter.increment(value)
  override def addGetMessagesItemCallCounter(value: Long): Unit      = getMessagesItemCallCounter.increment(value)
  override def addGetMessagesItemCallErrorCounter(value: Long): Unit = getMessagesItemCallErrorCounter.increment(value)
  override def setGetMessagesCallDuration(value: Long): Unit         = getMessagesCallDuration.record(value)
  override def addGetMessagesCallCounter(value: Long): Unit          = getMessagesCallCounter.increment(value)
  override def addGetMessagesCallErrorCounter(value: Long): Unit     = getMessagesCallErrorCounter.increment(value)

  private val allPersistenceIdsItemDuration         = Kamon.metrics.histogram("all-persistence-ids-item")
  private val allPersistenceIdsItemCounter          = Kamon.metrics.counter("all-persistence-ids-item")
  private val allPersistenceIdsItemCallCounter      = Kamon.metrics.counter("all-persistence-ids-item-call")
  private val allPersistenceIdsItemCallErrorCounter = Kamon.metrics.counter("all-persistence-ids-item-call-error")
  private val allPersistenceIdsCallDuration         = Kamon.metrics.histogram("all-persistence-ids-call")
  private val allPersistenceIdsCallCounter          = Kamon.metrics.counter("all-persistence-ids-call")
  private val allPersistenceIdsCallErrorCounter     = Kamon.metrics.counter("all-persistence-ids-call-error")

  override def setAllPersistenceIdsItemDuration(value: Long): Unit = allPersistenceIdsItemDuration.record(value)
  override def addAllPersistenceIdsItemCounter(value: Long): Unit  = allPersistenceIdsItemCounter.increment(value)
  override def addAllPersistenceIdsItemCallCounter(value: Long): Unit =
    allPersistenceIdsItemCallCounter.increment(value)
  override def addAllPersistenceIdsItemCallErrorCounter(value: Long): Unit =
    allPersistenceIdsItemCallErrorCounter.increment(value)
  override def setAllPersistenceIdsCallDuration(value: Long): Unit = allPersistenceIdsCallDuration.record(value)
  override def addAllPersistenceIdsCallCounter(value: Long): Unit  = allPersistenceIdsCallCounter.increment(value)
  override def addAllPersistenceIdsCallErrorCounter(value: Long): Unit =
    allPersistenceIdsCallErrorCounter.increment(value)

  private val eventsByTagItemDuration         = Kamon.metrics.histogram("event-by-tag-item")
  private val eventsByTagItemCounter          = Kamon.metrics.counter("event-by-tag-item")
  private val eventsByTagItemCallCounter      = Kamon.metrics.counter("event-by-tag-item-call")
  private val eventsByTagItemCallErrorCounter = Kamon.metrics.counter("event-by-tag-item-call-error")
  private val eventsByTagCallDuration         = Kamon.metrics.histogram("event-by-tag-call")
  private val eventsByTagCallCounter          = Kamon.metrics.counter("event-by-tag-call")
  private val eventsByTagCallErrorCounter     = Kamon.metrics.counter("event-by-tag-call-error")

  override def setEventsByTagItemDuration(value: Long): Unit     = eventsByTagItemDuration.record(value)
  override def addEventsByTagItemCounter(value: Long): Unit      = eventsByTagItemCounter.increment(value)
  override def addEventsByTagItemErrorCounter(value: Long): Unit = eventsByTagItemCallCounter.increment(value)
  override def addEventsByTagItemCallCounter(value: Long): Unit  = eventsByTagItemCallErrorCounter.increment(value)
  override def setEventsByTagCallDuration(value: Long): Unit     = eventsByTagCallDuration.record(value)
  override def addEventsByTagCallCounter(value: Long): Unit      = eventsByTagCallCounter.increment(value)
  override def addEventsByTagCallErrorCounter(value: Long): Unit = eventsByTagCallErrorCounter.increment(value)

  private val journalSequenceItemDuration     = Kamon.metrics.histogram("journal-sequence-item")
  private val journalSequenceItemCounter      = Kamon.metrics.counter("journal-sequence-item")
  private val journalSequenceCallDuration     = Kamon.metrics.histogram("journal-sequence-call")
  private val journalSequenceCallCounter      = Kamon.metrics.counter("journal-sequence-call")
  private val journalSequenceCallErrorCounter = Kamon.metrics.counter("journal-sequence-call-error")

  override def setJournalSequenceItemDuration(value: Long): Unit     = journalSequenceItemDuration.record(value)
  override def addJournalSequenceItemCounter(value: Long): Unit      = journalSequenceItemCounter.increment(value)
  override def setJournalSequenceCallDuration(value: Long): Unit     = journalSequenceCallDuration.record(value)
  override def addJournalSequenceCallCounter(value: Long): Unit      = journalSequenceCallCounter.increment(value)
  override def addJournalSequenceCallErrorCounter(value: Long): Unit = journalSequenceCallErrorCounter.increment(value)

}
