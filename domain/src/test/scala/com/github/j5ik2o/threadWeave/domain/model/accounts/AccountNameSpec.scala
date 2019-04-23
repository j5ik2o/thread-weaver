package com.github.j5ik2o.threadWeave.domain.model.accounts

import eu.timepit.refined.auto._
import org.scalatest.FreeSpec

class AccountNameSpec extends FreeSpec {
  "AccountName" - {
    "check" in {
      val result = AccountName("abcdef").withSuffix("_2")
      println(result)
    }
  }
}
