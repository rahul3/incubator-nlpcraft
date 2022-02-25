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

package org.apache.nlpcraft.internal.util

import org.apache.nlpcraft.NCException

import java.io.*
import java.net.URL
import scala.util.Using
import scala.io.Source
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.codec.digest.DigestUtils
import java.nio.file.Files

/**
  *
  */
object NCResourceReaderImpl extends LazyLogging:
    private final val DFLT_DIR = new File(System.getProperty("user.home"), ".nlpcraft/extcfg").getAbsolutePath
    private final val BASE_URL = "https://github.com/apache/incubator-nlpcraft/raw/external_config/external"
    private final val MD5_FILE_URL = s"$BASE_URL/md5.txt"

    /**
      *
      * @param dir
      * @return
      */
    private def mkDir(dir: String): File =
        val normDir = if dir != null then dir else DFLT_DIR
        val f = new File(normDir)

        if f.exists then
            if !f.isDirectory then throw new NCException(s"Invalid folder: $normDir")
        else
            if !f.mkdirs then throw new NCException(s"Cannot create folder: $normDir")

        f

    /**
      *
      * @param url
      * @return
      */
    private def readMd5(url: String): Map[String, String] =
        try
            Using.resource(Source.fromURL(url)) { src =>
                src.getLines().map(_.trim()).filter(s => s.nonEmpty && !s.startsWith("#")).map(p => {
                    val seq = p.split(" ").map(_.strip)

                    if seq.length != 2 || seq.exists(_.isEmpty) then
                        throw new NCException(s"Unexpected '$url' file line format: '$p'")

                    seq.head -> seq.last
                }).toList.toMap
            }
        catch case e: IOException => throw new NCException(s"Failed to read: '$url'", e)

    /**
      *
      * @param f
      */
    private def delete(f: File): Unit =
        if !f.delete() then throw new NCException(s"Couldn't delete file: ${f.getAbsolutePath}")
        else logger.info(s"File deleted: ${f.getAbsolutePath}")

    /**
      *
      * @param dir
      * @return
      */
    def apply(dir: String): NCResourceReaderImpl = new NCResourceReaderImpl(mkDir(dir))

    /**
      *
      * @return
      */
    def apply(): NCResourceReaderImpl = new NCResourceReaderImpl(mkDir(null))

import NCResourceReaderImpl.*

/**
  *
  * @param dir
  */
class NCResourceReaderImpl(dir: File) extends LazyLogging:
    private val md5 = readMd5(MD5_FILE_URL)

    /**
      *
      * @param f
      * @return
      */
    private def isExists(f: File): Boolean = f.exists() && f.isFile

    /**
      *
      * @param f
      * @return
      */
    private def getMd5(f: File): String =
        val path = f.getAbsolutePath
        val nameLen = f.getName.length

        md5.
            flatMap { (resPath, md5) => if path.endsWith(resPath) && resPath.length >= nameLen then Some(md5) else None }.
            to(LazyList).
            headOption.
            getOrElse(throw new NCException(s"MD5 data not found for: '$path'"))

    /**
      *
      * @param f
      * @return
      */
    private def isValid(f: File): Boolean =
        val v1 = getMd5(f)

        val v2 =
            try Using.resource(Files.newInputStream(f.toPath)) { in => DigestUtils.md5Hex(in) }
            catch case e: IOException => throw new NCException(s"Failed to get MD5 for: '${f.getAbsolutePath}'", e)

        v1 == v2

    /**
      *
      * @param path
      * @param outFile
      * @return
      */
    private def download(path: String, outFile: String): File =
        mkDir(new File(outFile).getParent)

        val url = s"$BASE_URL/$path"

        try
            Using.resource(new BufferedInputStream(new URL(url).openStream())) { src =>
                Using.resource(new FileOutputStream(outFile)) { out => IOUtils.copy(src, out) }
                logger.info(s"One-time download for external config [url='$url', file='$outFile']")

                val f = new File(outFile)
                if !isValid(f) then throw new NCException(s"Invalid downloaded file [url='$url'")
                f
            }
        catch case e: IOException => throw new NCException(s"Failed to download external config [url='$url', file='$outFile']", e)

    /**
      *
      * @param path
      * @return
      */
    def get(path: String): File =
        var f = new File(path)

        def process(f: File): File =
            if isValid(f) then
                logger.info(s"File found: ${f.getAbsolutePath}")
                f
            else
                delete(f)
                download(path, f.getAbsolutePath)

        if isExists(f) then
            process(f)
        else
            f = new File(DFLT_DIR, path)
            if isExists(f) then process(f) else download(path, f.getAbsolutePath)