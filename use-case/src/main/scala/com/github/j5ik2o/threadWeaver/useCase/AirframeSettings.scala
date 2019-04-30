package com.github.j5ik2o.threadWeaver.useCase

import wvlet.airframe._

object AirframeSettings {

  val design = newDesign
    .bind[CreateThreadUseCase].to[CreateThreadUseCaseImpl]

}
