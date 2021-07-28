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

package org.apache.nlpcraft.models.stm.indexes

import org.apache.nlpcraft.model.{NCIntent, NCIntentMatch, NCResult, _}
import org.apache.nlpcraft.models.stm.indexes.NCStmSpecModelAdapter.mapper
import org.apache.nlpcraft.{NCTestContext, NCTestEnvironment}
import org.junit.jupiter.api.Test

import java.util.{List => JList}
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.language.implicitConversions

case class NCStmLimitSpecModelData(note: String, indexes: Seq[Int])

class NCStmLimitSpecModel extends NCStmSpecModelAdapter {
    @NCIntent("intent=limit term(limit)~{tok_id() == 'nlpcraft:limit'} term(elem)~{has(tok_groups(), 'G')}")
    private def onLimit(ctx: NCIntentMatch, @NCIntentTerm("limit") limit: NCToken): NCResult =
        NCResult.json(
            mapper.writeValueAsString(
                NCStmLimitSpecModelData(
                    note = limit.meta[String]("nlpcraft:limit:note"),
                    indexes = limit.meta[JList[Int]]("nlpcraft:limit:indexes").asScala.toSeq
                )
            )
        )
}

@NCTestEnvironment(model = classOf[NCStmLimitSpecModel], startClient = true)
class NCStmLimitSpec extends NCTestContext {
    private def extract(s: String): NCStmLimitSpecModelData = mapper.readValue(s, classOf[NCStmLimitSpecModelData])

    @Test
    private[stm] def test(): Unit = {
        checkResult(
            "top 23 a a",
            extract,
            NCStmLimitSpecModelData(note = "A", indexes = Seq(1))
        )
        checkResult(
            "test test b b",
            extract,
            NCStmLimitSpecModelData(note = "B", indexes = Seq(2))
        )
    }
}