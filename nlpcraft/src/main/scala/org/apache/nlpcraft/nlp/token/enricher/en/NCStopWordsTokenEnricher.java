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

package org.apache.nlpcraft.nlp.token.enricher.en;

import org.apache.nlpcraft.NCModelConfig;
import org.apache.nlpcraft.NCRequest;
import org.apache.nlpcraft.NCToken;
import org.apache.nlpcraft.NCTokenEnricher;
import org.apache.nlpcraft.nlp.token.enricher.en.impl.NCStopWordsImpl;

import java.util.List;
import java.util.Set;

/**
 * TODO: enriches with <code>stopword</code> property.
 */
public class NCStopWordsTokenEnricher implements NCTokenEnricher {
    private final NCStopWordsImpl impl;

    /**
     * TODO: shoud we check single words?
     */
    public NCStopWordsTokenEnricher(Set<String> addStops, Set<String> exclStops) {
        impl = new NCStopWordsImpl(addStops, exclStops);
    }

    public NCStopWordsTokenEnricher() {
        impl = new NCStopWordsImpl(null, null);
    }

    @Override
    public void enrich(NCRequest req, NCModelConfig cfg, List<NCToken> toks) {
        assert impl != null;
        impl.enrich(req, cfg, toks);
    }

    @Override
    public void onStart(NCModelConfig cfg) {
        impl.onStart(cfg);
    }

    @Override
    public void onStop(NCModelConfig cfg) {
        impl.onStop(cfg);
    }
}
