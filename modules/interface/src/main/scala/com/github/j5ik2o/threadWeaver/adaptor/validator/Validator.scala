package com.github.j5ik2o.threadWeaver.adaptor.validator

trait Validator[A, B] {
  def validate(value: A): ValidationResult[B]
}
