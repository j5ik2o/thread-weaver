package com.github.j5ik2o.threadWeaver.adaptor.util

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks

trait ScalaCheckSupport extends PropertyChecks {

  def sameAs[A](c: Traversable[A], d: Traversable[A]): Boolean = {
    def counts(e: Traversable[A]) = e groupBy identity mapValues (_.size)
    counts(c) == counts(d)
  }

  val timestampGen: Gen[Long] =
    Gen.choose(-24 * 60 * 60 * 1000, 24 * 60 * 60 * 1000).map(_ + System.currentTimeMillis())

}
