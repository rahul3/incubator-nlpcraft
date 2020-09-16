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

package org.apache.nlpcraft

import java.io.IOException

import org.apache.nlpcraft.common.NCException
import org.apache.nlpcraft.model.NCModel
import org.apache.nlpcraft.model.tools.embedded.NCEmbeddedProbe
import org.apache.nlpcraft.model.tools.test.{NCTestClient, NCTestClientBuilder}
import org.apache.nlpcraft.probe.mgrs.model.NCModelManager
import org.junit.jupiter.api.{AfterEach, BeforeEach, TestInfo}

/**
  *
  */
class NCTestContext {
    protected var cli: NCTestClient = _

    @BeforeEach
    @throws[NCException]
    @throws[IOException]
    private def setUp(ti: TestInfo): Unit = {
        if (ti.getTestMethod.isPresent) {
            val a = ti.getTestMethod.get().getAnnotation(classOf[NCTestContextModel])

            if (a != null) {
                NCEmbeddedProbe.start(a.value().asInstanceOf[Class[NCModel]])

                cli = new NCTestClientBuilder().newBuilder.build

                cli.open(NCModelManager.getAllModels().head.model.getId)
            }
        }
    }

    @AfterEach
    @throws[NCException]
    @throws[IOException]
    private def tearDown(): Unit =
        if (cli != null) {
            cli.close()

            NCEmbeddedProbe.stop()
        }
}
