/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import scala.annotation.nowarn
import scala.scalajs.reflect.Reflect

import scala.scalajs.LinkingInfo.{linkTimeIf, moduleKind, ModuleKind}

import io.github.scalawasm.wasi4s.wasi.clocks
import io.github.scalawasm.wasi4s.wasi.random.random
import io.github.scalawasm.wasi4s.wasi.cli.stdout

import Test.*
import scala.annotation.tailrec

private[scalacheck] object Platform {

  @nowarn("msg=is never used")
  def runWorkers(
      params: Parameters,
      workerFun: Int => Result,
      stop: () => Unit
  ): Result = {
    workerFun(0)
  }

  @nowarn("msg=is never used")
  def loadModule(name: String, loader: ClassLoader): AnyRef = {
    Reflect
      .lookupLoadableModuleClass(name + "$")
      .getOrElse(throw new ClassNotFoundException(name + "$"))
      .loadModule()
      .asInstanceOf[AnyRef]
  }

  @nowarn("msg=is never used")
  def newInstance(name: String, loader: ClassLoader, paramTypes: Seq[Class[?]])(args: Seq[AnyRef]): AnyRef = {
    Reflect
      .lookupInstantiatableClass(name)
      .getOrElse(throw new ClassNotFoundException(name))
      .getConstructor(paramTypes*)
      .getOrElse(throw new NoSuchMethodError(paramTypes.mkString("<init>(", ",", ")")))
      .newInstance(args*)
      .asInstanceOf[AnyRef]
  }

  type EnableReflectiveInstantiation = scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

  def getClassLoader: ClassLoader = null

  lazy val out =  stdout.getStdout()

  def println(msg: String): Unit =
    linkTimeIf(moduleKind == ModuleKind.WasmComponent) {
      // blocking-write-and-flush will trap for more than 4096 bytes
      val maxBytes = 4096
      var arr: Array[Byte] = msg.getBytes()
      while (arr.size > maxBytes) {
        val (l, r) = arr.splitAt(maxBytes)
        out.blockingWriteAndFlush(l)
        arr = r
      }

      if (arr.size > 0) out.blockingWriteAndFlush(arr)
    } {
      scala.Console.println(msg)
    }

  def nanoTime(): Long =
    linkTimeIf(moduleKind == ModuleKind.WasmComponent) {
      clocks.monotonic_clock.now()
    } {
      System.nanoTime()
    }
  def nextRandomLong(): Long =
    linkTimeIf(moduleKind == ModuleKind.WasmComponent) {
      random.getRandomU64()
    } {
      scala.util.Random.nextLong()
    }
}
