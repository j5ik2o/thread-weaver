package com.github.j5ik2o.threadWeaver.adaptor

import cats.data.ValidatedNel
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError

package object validator {
  type ValidationResult[A] = ValidatedNel[InterfaceError, A]
}
