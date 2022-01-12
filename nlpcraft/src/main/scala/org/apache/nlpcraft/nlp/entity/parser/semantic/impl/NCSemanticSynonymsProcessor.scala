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
package org.apache.nlpcraft.nlp.entity.parser.semantic.impl

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.dataformat.yaml.*
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.scalalogging.LazyLogging
import org.apache.nlpcraft.*
import org.apache.nlpcraft.internal.makro.NCMacroParser
import org.apache.nlpcraft.internal.util.NCUtils
import org.apache.nlpcraft.nlp.entity.parser.semantic.*
import org.apache.nlpcraft.nlp.entity.parser.semantic.impl.NCSemanticChunkKind.*

import java.io.InputStream
import java.util
import java.util.Set as JSet
import java.util.regex.*
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

/**
  *
  * @param elementId
  * @param value
  */
private[impl] case class NCSemanticSynonymsElementData(elementId: String, value: Option[String])

/**
  *
  * @param textSynonyms
  * @param mixedSynonyms
  */
private[impl] case class NCSemanticSynonymsHolder(
    textSynonyms: Map[String, Set[NCSemanticSynonymsElementData]],
    mixedSynonyms: Map[Int, Map[String, Seq[NCSemanticSynonym]]]
)

/**
  *
  */
private[impl] object NCSemanticSynonymsProcessor extends LazyLogging:
    private final val SUSP_SYNS_CHARS = Seq("?", "*", "+")
    private final val REGEX_FIX = "//"
    private final val ID_REGEX = "^[_a-zA-Z]+[a-zA-Z0-9:\\-_]*$"

    /**
      *
      * @param iter
      * @return
      */
    private def hasNullOrEmpty(iter: Iterable[String]): Boolean = iter.exists(p => p == null || p.strip.isEmpty)

    /**
      *
      * @param macros
      * @param elements
      */
    private def checkMacros(macros: Map[String, String], elements: Seq[NCSemanticElement]): Unit =
        require(elements != null)

        if macros != null then
            if hasNullOrEmpty(macros.keySet) then throw new NCException("Some macro names are null or empty.") // TODO: error text.
            if hasNullOrEmpty(macros.values) then throw new NCException("Some macro bodies are null or empty.") // TODO: error text.

            val set = elements.filter(_.getSynonyms != null).flatMap(_.getSynonyms.asScala) ++ macros.values

            for (makro <- macros.keys if !set.exists(_.contains(makro)))
                logger.warn(s"Unused macro detected [macro=$makro]")

            def isSuspicious(s: String): Boolean = SUSP_SYNS_CHARS.exists(s.contains)

            // Ignore suspicious chars if regex is used in macro...
            for ((name, value) <- macros if isSuspicious(name) || (isSuspicious(value) && !value.contains("//")))
                // TODO: error text.
                logger.warn(
                    s"Suspicious macro definition (use of ${SUSP_SYNS_CHARS.map(s => s"'$s'").mkString(", ")} chars) [" +
                    s"macro=$name" +
                    s"]"
                )

    /**
      *
      * @param syns
      * @param elemId
      * @param valueName
      */
    private def checkSynonyms(syns: JSet[String], elemId: String, valueName: Option[String] = None): Unit =
        def mkDesc: String =
            val valuePart = if valueName.isDefined then s", value=${valueName.get}" else ""

            s"[id=$elemId$valuePart]"

        if syns != null then
            if hasNullOrEmpty(syns.asScala) then throw new NCException(s"Some synonyms are null or empty $mkDesc") // TODO: error text.

            val susp = syns.asScala.filter(syn => !syn.contains("//") && SUSP_SYNS_CHARS.exists(susp => syn.contains(susp)))

            if susp.nonEmpty then
                // TODO: error text.
                logger.warn(
                    s"Suspicious synonyms detected (use of ${SUSP_SYNS_CHARS.map(s => s"'$s'").mkString(", ")} chars) $mkDesc"
                )
    /**
      *
      * @param elems
      */
    private def checkElements(elems: Seq[NCSemanticElement]): Unit =
        if elems == null || elems.isEmpty then throw new NCException("Elements cannot be null or empty.") // TODO: error text.
        if elems.contains(null) then throw new NCException("Some elements are null.") // TODO: error text.

        // Duplicates.
        val ids = mutable.HashSet.empty[String]

        for (id <- elems.map(_.getId))
            if ids.contains(id) then throw new NCException(s"Duplicate element ID [element=$id]") // TODO: error text.
            else ids += id

        for (e <- elems)
            val elemId = e.getId

            if elemId == null || elemId.isEmpty then
                throw new NCException(s"Some element IDs are not provided or empty.") // TODO: error text.
            else if !elemId.matches(ID_REGEX) then
                throw new NCException(s"Element ID does not match regex [element=$elemId, regex=$ID_REGEX]") // TODO: error text.
            else if elemId.exists(_.isWhitespace) then
                throw new NCException(s"Element ID cannot have whitespaces [element=$elemId]") // TODO: error text.

            checkSynonyms(e.getSynonyms, elemId)

            val vals = e.getValues

            if vals != null then
                if hasNullOrEmpty(vals.keySet().asScala) then
                    throw new NCException(s"Some values names are null or empty [element=$elemId]") // TODO: error text.

                for ((name, syns) <- vals.asScala)
                    checkSynonyms(syns, elemId, Some(name))

    /**
      *
      * @param stemmer
      * @param tokParser
      * @param macroParser
      * @param elemId
      * @param syns
      * @return
      */
    private def convertSynonyms(
        stemmer: NCSemanticStemmer,
        tokParser: NCTokenParser,
        macroParser: NCMacroParser,
        elemId: String,
        syns: JSet[String]
    ): Seq[Seq[NCSemanticSynonymChunk]] =
        case class RegexHolder(text: String, var used: Boolean = false):
            private def stripSuffix(fix: String, s: String): String = s.slice(fix.length, s.length - fix.length)

            def mkChunk(): NCSemanticSynonymChunk =
                val ptrn = stripSuffix(REGEX_FIX, text)

                if ptrn.nonEmpty then
                    try
                        NCSemanticSynonymChunk(REGEX, text, regex = Pattern.compile(ptrn))
                    catch
                        case e: PatternSyntaxException =>
                            // TODO: error text.
                            throw new NCException(s"Invalid regex synonym syntax detected [element=$elemId, chunk=$text]", e)
                else
                    throw new NCException(s"Empty regex synonym detected [element=$elemId]") // TODO: error text.

        val regexes = mutable.HashMap.empty[Int, RegexHolder]

        def findRegex(t: NCToken): Option[RegexHolder] =
            if regexes.nonEmpty then
                (t.getStartCharIndex to t.getEndCharIndex).flatMap(regexes.get).to(LazyList).headOption
            else
                None

        syns.asScala.flatMap(macroParser.expand).
            map(syn => {
                // Drops redundant spaces without any warnings.
                val normSyn = syn.split(" ").map(_.strip).filter(_.nonEmpty)

                var start = 0
                var end = -1
                regexes.clear()

                // Saves regex chunks positions. Regex chunks can be found without tokenizer, just split by spaces.
                for (ch <- normSyn)
                    start = end + 1
                    end = start + ch.length

                    if ch.startsWith(REGEX_FIX) && ch.endsWith(REGEX_FIX) then
                        val r = RegexHolder(ch)

                        (start to end).foreach(regexes += _ -> r)

                // Tokenizes synonym without regex chunks. Regex chunks are used as is, without tokenization.
                tokParser.tokenize(normSyn.mkString(" ")).asScala.flatMap(tok =>
                    findRegex(tok) match
                        case Some(regex) =>
                            if regex.used then
                                None
                            else
                                regex.used = true
                                Some(regex.mkChunk())
                        case None => Some(NCSemanticSynonymChunk(TEXT, tok.getText, stemmer.stem(tok.getText)))
                ).toSeq
            }).toSeq

    /**
      *
      * @param stemmer
      * @param tokParser
      * @param macros
      * @param elements
      * @return
      */
    def prepare(
        stemmer: NCSemanticStemmer,
        tokParser: NCTokenParser,
        macros: Map[String, String],
        elements: Seq[NCSemanticElement]
    ): NCSemanticSynonymsHolder =
        require(stemmer != null && tokParser != null)

        // Order is important.
        checkElements(elements)
        checkMacros(macros, elements)

        val macroParser = new NCMacroParser

        if macros != null then
            for ((name, body) <- macros) macroParser.addMacro(name, body)

        case class Holder(synonym: NCSemanticSynonym, elementId: String) {
            lazy val root: String = synonym.chunks.map(p => if p.isText then p.stem else p.text).mkString(" ")
        }

        val buf = mutable.ArrayBuffer.empty[Holder]

        for (e <- elements)
            val elemId = e.getId

            def add(syns: Seq[NCSemanticSynonym]): Unit = buf ++= syns.map(Holder(_, elemId))
            def addSpec(txt: String, value: String = null): Unit =
                buf += Holder(NCSemanticSynonym(Seq(NCSemanticSynonymChunk(TEXT, txt, stemmer.stem(txt))), value), elemId)

            addSpec(elemId)

            if e.getSynonyms != null then
                add(convertSynonyms(stemmer, tokParser, macroParser, elemId, e.getSynonyms).map(NCSemanticSynonym(_)))

            if e.getValues != null then
                for ((name, syns) <- e.getValues.asScala)
                    addSpec(name, value = name)

                    if syns != null then
                        add(
                            convertSynonyms(stemmer, tokParser, macroParser, elemId, syns).
                                map(chunks => NCSemanticSynonym(chunks, value = name))
                        )

        buf.groupBy(_.root).values.foreach(hs => {
            val elemIds = hs.map(_.elementId).toSet

            if elemIds.size > 1 then
                for (s <- hs.map(_.synonym).distinct)
                    // TODO: error text.
                    logger.warn(
                        s"Synonym is related to various elements " +
                        s"[synonym='${s.chunks.mkString(" ")}'" +
                        s", elements=${elemIds.mkString("{", ",", "}")}" +
                        s"]")
        })

        val txtBuf = buf.filter(_.synonym.isText)

        val txtSyns =
            txtBuf.groupBy(_.synonym.stem).
            map { (stem, hs) =>
                stem ->
                    hs.map(h =>
                        NCSemanticSynonymsElementData(h.elementId, Option.when(h.synonym.value != null)(h.synonym.value))
                    ).toSet
            }

        buf --= txtBuf

        val mixedSyns = buf.groupBy(_.synonym.size).
            map { (size, hs) => size -> hs.groupBy(_.elementId).map { (id, hs) => id -> hs.map(_.synonym).toSeq } }

        NCSemanticSynonymsHolder(txtSyns, mixedSyns)