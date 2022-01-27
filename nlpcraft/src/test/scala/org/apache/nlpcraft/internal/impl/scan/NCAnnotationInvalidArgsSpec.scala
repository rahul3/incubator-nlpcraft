/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nlpcraft.internal.impl.scan

import org.apache.nlpcraft.*
import org.apache.nlpcraft.internal.impl.NCAnnotationsScanner
import org.apache.nlpcraft.nlp.util.*
import org.apache.nlpcraft.nlp.util.opennlp.*
import org.junit.jupiter.api.Test

import java.util

/**
  * It tests invalid intents methods parameters types usage.
  * Note that for some kind of models (it depends on creation type) we can't check methods arguments during scan.
  */
class NCAnnotationInvalidArgsSpec:
    class DefinedClassModelValid extends NCModel:
        override def getConfig: NCModelConfig = CFG
        override def getPipeline: NCModelPipeline = EN_PIPELINE

        // Valid parameters.
        @NCIntent("intent=validList term(list)~{# == 'x'}[0,10]")
        def validList(@NCIntentTerm("list") list: List[NCEntity]): NCResult = processListEntity(list)

        @NCIntent("intent=validOpt term(opt)~{# == 'x'}?")
        def validOpt(@NCIntentTerm("opt") opt: Option[NCEntity]): NCResult = processOptEntity(opt)

    class DefinedClassModelInvalidLst extends NCModel:
        override def getConfig: NCModelConfig = CFG
        override def getPipeline: NCModelPipeline = EN_PIPELINE

        // Invalid parameters.
        @NCIntent("intent=invalidList term(list)~{# == 'x'}[0,10]")
        def invalidList(@NCIntentTerm("list") list: List[Int]): NCResult = processListInt(list)

    class DefinedClassModelInvalidOpt extends NCModel:
        override def getConfig: NCModelConfig = CFG
        override def getPipeline: NCModelPipeline = EN_PIPELINE

        // Invalid parameters.
        @NCIntent("intent=invalidOpt term(opt)~{# == 'x'}?")
        def invalidOpt(@NCIntentTerm("opt") opt: Option[Int]): NCResult = processOptInt(opt)


    private val CHECKED_MDL_VALID: NCModel = new DefinedClassModelValid
    private val CHECKED_MDL_INVALID_LST: NCModel = new DefinedClassModelInvalidLst
    private val CHECKED_MDL_INVALID_OPT: NCModel = new DefinedClassModelInvalidOpt
    private val UNCHECKED_MDL_MIX: NCModel =
        new NCModel:
            override def getConfig: NCModelConfig = CFG
            override def getPipeline: NCModelPipeline = EN_PIPELINE

            // Valid parameters.
            @NCIntent("intent=validList term(list)~{# == 'x'}[0,10]")
            def validList(@NCIntentTerm("list") list: List[NCEntity]): NCResult = processListEntity(list)

            @NCIntent("intent=validOpt term(opt)~{# == 'x'}?")
            def validOpt(@NCIntentTerm("opt") opt: Option[NCEntity]): NCResult = processOptEntity(opt)

            // Invalid parameters.
            @NCIntent("intent=invalidList term(list)~{# == 'x'}[0,10]")
            def invalidList(@NCIntentTerm("list") list: List[Int]): NCResult = processListInt(list)

            @NCIntent("intent=invalidOpt term(opt)~{# == 'x'}?")
            def invalidOpt(@NCIntentTerm("opt") opt: Option[Int]): NCResult = processOptInt(opt)

    private val INTENT_MATCH =
        val ent = NCTestEntity("id", "reqId", tokens = NCTestToken())

        new NCIntentMatch:
            override def getIntentId: String = "impIntId"
            override def getIntentEntities: util.List[util.List[NCEntity]] = col(col(ent))
            override def getTermEntities(idx: Int): util.List[NCEntity] = col(ent)
            override def getTermEntities(termId: String): util.List[NCEntity] = col(ent)
            override def getVariant: NCVariant = new NCVariant:
                override def getEntities: util.List[NCEntity] = col(ent)

    private def mkResult0(obj: Any): NCResult =
        println(s"Result body: $obj, class=${obj.getClass}")
        val res = new NCResult()
        res.setBody(obj)
        res

    private def processOptInt(opt: Option[Int]): NCResult =
        // Access and cast.
        val body: Int = opt.get
        mkResult0(body)

    private def processOptEntity(opt: Option[NCEntity]): NCResult =
        // Access and cast.
        val body: NCEntity = opt.get
        mkResult0(body)

    private def processListInt(list: List[Int]): NCResult =
        // Access and cast.
        val bodyHead: Int = list.head
        mkResult0(list)

    private def processListEntity(list: List[NCEntity]): NCResult =
        // Access and cast.
        val bodyHead: NCEntity = list.head
        mkResult0(list)

    private def col[T](t: T): util.List[T] = java.util.Collections.singletonList(t)

    private def testOk(mdl: NCModel, intentId: String): Unit =
        val cb = new NCAnnotationsScanner(mdl).scan().find(_.intent.id == intentId).get.callback

        println(s"Test finished [modelClass=${mdl.getClass}, intent=$intentId, result=${cb.cbFun.apply(INTENT_MATCH)}")

    private def testRuntimeClassCast(mdl: NCModel, intentId: String): Unit =
        val cb = new NCAnnotationsScanner(mdl).scan().find(_.intent.id == intentId).get.callback

        try
            cb.cbFun.apply(INTENT_MATCH)

            require(false)
        catch
            case e: NCException =>
                if e.getCause != null && e.getCause.isInstanceOf[ClassCastException] then
                    println(s"Expected error: $e")
                    e.printStackTrace(System.out)
                else throw e

    private def testScanError(mdl: NCModel, intentId: String): Unit =
        try
            new NCAnnotationsScanner(mdl)
        catch
            case e: NCException =>
                println(s"Expected error: $e")
                e.printStackTrace(System.out)

    @Test
    def test(): Unit =
        testOk(CHECKED_MDL_VALID, "validList")
        testOk(CHECKED_MDL_VALID, "validOpt")
        testScanError(CHECKED_MDL_INVALID_LST, "invalidList")
        testScanError(CHECKED_MDL_INVALID_OPT, "invalidOpt")

        testOk(UNCHECKED_MDL_MIX, "validList")
        testOk(UNCHECKED_MDL_MIX, "validOpt")
        testRuntimeClassCast(UNCHECKED_MDL_MIX, "invalidList")
        testRuntimeClassCast(UNCHECKED_MDL_MIX, "invalidOpt")