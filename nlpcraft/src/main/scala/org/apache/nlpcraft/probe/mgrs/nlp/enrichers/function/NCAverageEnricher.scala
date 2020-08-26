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
 *
 */

package org.apache.nlpcraft.probe.mgrs.nlp.enrichers.function

import java.io
import _root_.io.opencensus.trace.Span
import org.apache.nlpcraft.common.nlp.NCNlpSentence
import org.apache.nlpcraft.probe.mgrs.NCModelDecorator
import org.apache.nlpcraft.probe.mgrs.nlp.NCProbeEnricher

object NCAverageEnricher extends NCProbeEnricher {
    /**
     *
     * Processes this NLP sentence.
     *
     * @param mdl     Model decorator.
     * @param ns      NLP sentence to enrich.
     * @param senMeta Sentence metadata.
     * @param parent  Span parent.
     */
    override def enrich(mdl: NCModelDecorator,
                        ns: NCNlpSentence,
                        senMeta: collection.Map[String, io.Serializable],
                        parent: Span): Unit = ???
}
