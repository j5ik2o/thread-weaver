package com.github.j5ik2o.threadWeaver.api

class APIException(errorMessages: Seq[String]) extends Exception(errorMessages.mkString(", "))
