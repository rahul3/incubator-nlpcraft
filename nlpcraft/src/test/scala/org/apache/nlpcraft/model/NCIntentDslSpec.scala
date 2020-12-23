/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nlpcraft.model

import org.apache.nlpcraft.{NCTestContext, NCTestEnvironment}
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.Test

import scala.language.implicitConversions

/**
  * Intents DSL test model.
  */
class NCIntentDslSpecModel extends NCModelAdapter(
    "nlpcraft.intents.dsl.test", "Intents DSL Test Model", "1.0"
) {
    private implicit def convert(s: String): NCResult = NCResult.text(s)

    // Moscow population filter.
    @NCIntent("intent=bigCity term(city)~{id == 'nlpcraft:city' && ~nlpcraft:city:citymeta['population'] >= 10381222}")
    private def onBigCity(ctx: NCIntentMatch): NCResult = "OK"

    @NCIntent("intent=otherCity term(city)~{id == 'nlpcraft:city' }")
    private def onOtherCity(ctx: NCIntentMatch): NCResult = "OK"
}

/**
  * Intents DSL test.
  */
@NCTestEnvironment(model = classOf[NCIntentDslSpecModel], startClient = true)
class NCIntentDslSpec extends NCTestContext {
    private def check(txt: String, intent:  String): Unit = {
        val res = getClient.ask(txt)

        assertTrue(res.isOk, s"Checked: $txt")
        assertTrue(res.getResult.isPresent, s"Checked: $txt")
        assertEquals(intent, res.getIntentId, s"Checked: $txt")
    }

    @Test
    def testBigCity(): Unit = check("Moscow", "bigCity")

    @Test
    def testOtherCity(): Unit = check("San Francisco", "otherCity")
}

