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

package org.apache.nlpcraft.model.intent.impl.ver2

import org.antlr.v4.runtime.tree.{TerminalNode ⇒ TN}
import org.apache.commons.lang3.StringUtils
import org.apache.nlpcraft.model.NCToken
import org.apache.nlpcraft.model.intent.utils.ver2.{NCDslTermContext, NCDslTermRetVal}
import org.apache.nlpcraft.common._

import java.lang.{Double ⇒ JDouble, IllegalArgumentException ⇒ IAE, Long ⇒ JLong}
import java.time.LocalDate
import java.util.{Collections, ArrayList ⇒ JArrayList, HashMap ⇒ JHashMap}
import scala.collection.mutable

//noinspection DuplicatedCode
trait NCBaseDslCompiler {
    type StackType = mutable.ArrayStack[NCDslTermRetVal]
    type Instr = (NCToken, StackType,  NCDslTermContext) ⇒ Unit

    def isJLong(v: AnyRef): Boolean = v.isInstanceOf[JLong]
    def isJDouble(v: AnyRef): Boolean = v.isInstanceOf[JDouble]
    def isString(v: AnyRef): Boolean = v.isInstanceOf[String]
    def isBoolean(v: AnyRef): Boolean = v.isInstanceOf[Boolean]
    def asJLong(v: AnyRef): Long = v.asInstanceOf[JLong].longValue()
    def asJDouble(v: AnyRef): Double = v.asInstanceOf[JDouble].doubleValue()
    def asString(v: AnyRef): String = v.asInstanceOf[String]
    def asBool(v: AnyRef): Boolean = v.asInstanceOf[Boolean]

    def pushAny(any: AnyRef, usedTok: Boolean)(implicit stack: StackType): Unit =
        stack.push(NCDslTermRetVal(any, usedTok))
    def pushLong(any: Long, usedTok: Boolean)(implicit stack: StackType): Unit =
        stack.push(NCDslTermRetVal(Long.box(any), usedTok))
    def pushDouble(any: Double, usedTok: Boolean)(implicit stack: StackType): Unit =
        stack.push(NCDslTermRetVal(Double.box(any), usedTok))
    def pushBool(any: Boolean, usedTok: Boolean)(implicit stack: StackType): Unit =
        stack.push(NCDslTermRetVal(Boolean.box(any), usedTok))

    def errUnaryOp(op: String, v: AnyRef): IAE =
        new IAE(s"Unexpected '$op' DSL operation for value: $v")
    def errBinaryOp(op: String, v1: AnyRef, v2: AnyRef): IAE =
        new IAE(s"Unexpected '$op' DSL operation for values: $v1, $v2")
    def errUnknownFun(fun: String): IAE =
        new IAE(s"Unknown DSL function: $fun()")
    def errMinParamNum(min: Int, fun: String): IAE =
        new IAE(s"Invalid number of parameters for DSL function ($min is required): $fun()")
    def errParamNum(fun: String): IAE =
        new IAE(s"Invalid number of parameters for DSL function: $fun()")
    def errParamType(fun: String, param: AnyRef, expectType: String): IAE =
        new IAE(s"Expecting '$expectType' type of parameter for DSL function '$fun()', found: $param")

    /**
     *
     * @param stack
     * @return
     */
    def pop2()(implicit stack: StackType): (AnyRef, AnyRef, Boolean, Boolean) = {
        require(stack.size >= 2)

        // Stack pops in reverse order of push...
        val NCDslTermRetVal(val2, f2) = stack.pop()
        val NCDslTermRetVal(val1, f1) = stack.pop()

        (val1, val2, f1, f2)
    }
    /**
     *
     * @param stack
     * @return
     */
    def pop3()(implicit stack: StackType): (AnyRef, AnyRef, AnyRef, Boolean, Boolean, Boolean) = {
        require(stack.size >= 3)

        // Stack pops in reverse order of push...
        val NCDslTermRetVal(val3, f3) = stack.pop()
        val NCDslTermRetVal(val2, f2) = stack.pop()
        val NCDslTermRetVal(val1, f1) = stack.pop()

        (val1, val2, val3, f1, f2, f3)
    }

    /**
     *
     * @param stack
     * @return
     */
    def pop1()(implicit stack: StackType): (AnyRef, Boolean) = {
        require(stack.nonEmpty)

        val NCDslTermRetVal(v, f) = stack.pop()

        (v, f)
    }

    /**
     *
     * @param lt
     * @param gt
     * @param lteq
     * @param gteq
     */
    def parseCompExpr(lt: TN, gt: TN, lteq: TN, gteq: TN): Instr = (_, stack: StackType, _) ⇒ {
        implicit val s = stack

        val (v1, v2, f1, f2) = pop2()
        val usedTok = f1 || f2

        if (lt != null) {
            if (isJLong(v1) && isJLong(v2)) pushBool(asJLong(v1) < asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushBool(asJLong(v1) < asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushBool(asJDouble(v1) < asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushBool(asJDouble(v1) < asJDouble(v2), usedTok)
            else
                throw errBinaryOp("<", v1, v2)
        }
        else if (gt != null) {
            if (isJLong(v1) && isJLong(v2)) pushBool(asJLong(v1) > asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushBool(asJLong(v1) > asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushBool(asJDouble(v1) > asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushBool(asJDouble(v1) > asJDouble(v2), usedTok)
            else
                throw errBinaryOp(">", v1, v2)
        }
        else if (lteq != null) {
            if (isJLong(v1) && isJLong(v2)) pushBool(asJLong(v1) <= asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushBool(asJLong(v1) <= asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushBool(asJDouble(v1) <= asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushBool(asJDouble(v1) <= asJDouble(v2), usedTok)
            else
                throw errBinaryOp("<=", v1, v2)
        }
        else {
            assert(gteq != null)

            if (isJLong(v1) && isJLong(v2)) pushBool(asJLong(v1) >= asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushBool(asJLong(v1) >= asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushBool(asJDouble(v1) >= asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushBool(asJDouble(v1) >= asJDouble(v2), usedTok)
            else
                throw errBinaryOp(">=", v1, v2)
        }
    }

    /**
     *
     * @param mult
     * @param mod
     * @param div
     */
    def parseMultExpr(mult: TN, mod: TN, div: TN): Instr = (_, stack: StackType, _) ⇒ {
        implicit val s = stack

        val (v1, v2, f1, f2) = pop2()
        val usedTok = f1 || f2

        if (mult != null) {
            if (isJLong(v1) && isJLong(v2)) pushLong(asJLong(v1) * asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushDouble(asJLong(v1) * asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushDouble(asJDouble(v1) * asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushDouble(asJDouble(v1) * asJDouble(v2), usedTok)
            else
                throw errBinaryOp("*", v1, v2)
        }
        else if (mod != null) {
            if (isJLong(v1) && isJLong(v2)) pushLong(asJLong(v1) % asJLong(v2), usedTok)
            else
                throw errBinaryOp("%", v1, v2)
        }
        else {
            assert(div != null)

            if (isJLong(v1) && isJLong(v2)) pushLong(asJLong(v1) / asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushDouble(asJLong(v1) / asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushDouble(asJDouble(v1) / asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushDouble(asJDouble(v1) / asJDouble(v2), usedTok)
            else
                throw errBinaryOp("/", v1, v2)
        }
    }

    /**
     *
     * @param and
     * @param or
     * @return
     */
    def parseLogExpr(and: TN, or : TN): Instr = (_, stack: StackType, _) ⇒ {
        implicit val s = stack

        val (v1, v2, f1, f2) = pop2()

        if (!isBoolean(v1) || !isBoolean(v2))
            throw errBinaryOp(if (and != null) "&&" else "||", v1, v2)

        if (and != null)
            pushBool(asBool(v1) && asBool(v2), f1 || f2) // Note logical OR for used token flag.
        else {
            assert(or != null)

            pushBool(asBool(v1) || asBool(v2), f1 && f2) // Note local AND for used token flag.
        }
    }

    /**
     *
     * @param eq
     * @param neq
     * @return
     */
    def parseEqExpr(eq: TN, neq: TN): Instr = (_, stack: StackType, _) ⇒ {
        implicit val s = stack

        val (v1, v2, f1, f2) = pop2()
        val usedTok = f1 || f2

        def doEq(op: String): Boolean = {
            if (isJLong(v1) && isJLong(v2))
                asJLong(v1) == asJLong(v2)
            if (isJLong(v1) && isJLong(v2))
                asJLong(v1) == asJLong(v2)
            else
                throw errBinaryOp(op, v1, v2)

        }

        if (eq != null)
            pushBool(doEq("=="), usedTok)
        else {
            assert(neq != null)

            pushBool(!doEq("!='"), usedTok)
        }
    }

    /**
     *
     * @param plus
     * @param minus
     */
    def parsePlusExpr(plus: TN, minus: TN): Instr = (_, stack: StackType, _) ⇒ {
        implicit val s = stack

        val (v1, v2, f1, f2) = pop2()
        val usedTok = f1 || f2

        if (plus != null) {
            if (isString(v1) && isString(v2)) pushAny(asString(v1) + asString(v2), usedTok)
            else if (isJLong(v1) && isJLong(v2)) pushLong(asJLong(v1) + asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushDouble(asJLong(v1) + asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushDouble(asJDouble(v1) + asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushDouble(asJDouble(v1) + asJDouble(v2), usedTok)
            else
                throw errBinaryOp("+", v1, v2)
        }
        else {
            assert(minus != null)

            if (isJLong(v1) && isJLong(v2)) pushLong(asJLong(v1) - asJLong(v2), usedTok)
            else if (isJLong(v1) && isJDouble(v2)) pushDouble(asJLong(v1) - asJDouble(v2), usedTok)
            else if (isJDouble(v1) && isJLong(v2)) pushDouble(asJDouble(v1) - asJLong(v2), usedTok)
            else if (isJDouble(v1) && isJDouble(v2)) pushDouble(asJDouble(v1) - asJDouble(v2), usedTok)
            else
                throw errBinaryOp("-", v1, v2)
        }
    }


    /**
     * @param minus
     * @param not
     * @return
     */
    def parseUnaryExpr(minus: TN, not: TN): Instr = (_, stack: StackType, _) ⇒ {
        implicit val s = stack

        val (v, usedTok) = pop1()

        if (minus != null) {
            if (isJDouble(v)) pushDouble(-asJDouble(v), usedTok)
            else if (isJLong(v)) pushLong(-asJLong(v), usedTok)
            else
                throw errUnaryOp("-", v)
        }
        else {
            assert(not != null)

            if (isBoolean(v)) pushBool(!asBool(v), usedTok)
            else
                throw errUnaryOp("!", v)
        }
    }

    /**
     *
     * @param txt
     * @return
     */
    def parseAtom(txt: String): Instr = {
        val atom =
            if (txt == "null") null // Try 'null'.
            else if (txt == "true") Boolean.box(true) // Try 'boolean'.
            else if (txt == "false") Boolean.box(false) // Try 'boolean'.
            // Only numeric or string values below...
            else {
                // Strip '_' from numeric values.
                val num = txt.replaceAll("_", "")

                try
                    Long.box(JLong.parseLong(num)) // Try 'long'.
                catch {
                    case _: NumberFormatException ⇒
                        try
                            Double.box(JDouble.parseDouble(num)) // Try 'double'.
                        catch {
                            case _: NumberFormatException ⇒ txt // String by default (incl. quotes).
                        }
                }
            }

        (_, stack, _) ⇒ pushAny(atom, false)(stack)
    }

    /**
     *
     * @param id
     * @return
     */
    def parseCallExpr(id: TN): Instr = {
        val fun = id.getText

        (tok, stack: StackType, termCtx) ⇒ {
            implicit val evidence = stack

            def ensureStack(min: Int): Unit =
                if (stack.size < min)
                    throw errMinParamNum(min, fun)

            def get1Str(): (String, Boolean) = {
                ensureStack(1)

                val (v, f) = pop1()

                if (!isString(v))
                    throw errParamType(fun, v, "string")

                (asString(v), f)
            }
            def get1Double(): (JDouble, Boolean) = {
                ensureStack(1)

                val (v, f) = pop1()

                if (!isJDouble(v))
                    throw errParamType(fun, v, "double")

                (asJDouble(v), f)
            }
            def get2Doubles(): (JDouble, JDouble, Boolean) = {
                ensureStack(2)

                val (v1, v2, f1, f2) = pop2()

                if (!isJDouble(v1))
                    throw errParamType(fun, v1, "double")
                if (!isJDouble(v2))
                    throw errParamType(fun, v2, "double")

                (asJDouble(v1), asJDouble(v2), f1 || f2)
            }
            def get1Any(): (AnyRef, Boolean) = {
                ensureStack(1)

                pop1()
            }

            /*
             * String operations.
             */
            def doTrim(): Unit = get1Str() match { case (s, f) ⇒ pushAny(s.trim, f) }
            def doUppercase(): Unit = get1Str() match { case (s, f) ⇒ pushAny(s.toUpperCase, f) }
            def doLowercase(): Unit = get1Str() match { case (s, f) ⇒ pushAny(s.toLowerCase, f) }
            def doIsAlpha(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isAlpha(asString(s)), f) }
            def doIsNum(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isNumeric(asString(s)), f) }
            def doIsAlphaNum(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isAlphanumeric(asString(s)), f) }
            def doIsWhitespace(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isWhitespace(asString(s)), f) }
            def doIsAlphaSpace(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isAlphaSpace(asString(s)), f) }
            def doIsAlphaNumSpace(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isAlphanumericSpace(asString(s)), f) }
            def doIsNumSpace(): Unit = get1Str() match { case (s, f) ⇒ pushBool(StringUtils.isNumericSpace(asString(s)), f) }

            def doSplit(): Unit = {
                ensureStack(2)

                val (v1, v2, f1, f2) = pop2()

                if (!isString(v1))
                    errParamType(fun, v1, "string")
                if (!isString(v2))
                    errParamType(fun, v2, "string")

                asString(v1).split(asString(v2)).foreach { pushAny(_, f1 || f2) }
            }
            def doSplitTrim(): Unit = {
                ensureStack(2)

                val (v1, v2, f1, f2) = pop2()

                if (!isString(v1))
                    errParamType(fun, v1, "string")
                if (!isString(v2))
                    errParamType(fun, v2, "string")

                asString(v1).split(asString(v2)).foreach { s ⇒ pushAny(s.strip, f1 || f2) }
            }

            /*
             * Collection, statistical operations.
             */
            def doList(): Unit = {
                val jl = new JArrayList[Object]() // Empty list is allowed.
                var f = false

                stack.drain { x ⇒
                    jl.add(x.retVal)
                    f = f || x.usedTok
                }

                Collections.reverse(jl)

                pushAny(jl, f)
            }
            def doMap(): Unit = {
                if (stack.size % 2 != 0)
                    errParamNum(fun)

                val jm = new JHashMap[Object, Object]()
                var f = false

                val keys = mutable.Buffer.empty[AnyRef]
                val vals = mutable.Buffer.empty[AnyRef]

                var idx = 0

                stack.drain { x ⇒
                    if (idx % 2 == 0) keys += x.retVal else vals += x.retVal
                    f = f || x.usedTok

                    idx += 1
                }

                for ((k, v) ← keys zip vals)
                    jm.put(k, v)

                pushAny(jm, f)
            }

            /*
             * Metadata operations.
             */
            def doTokenMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(tok.meta(s), true) }
            def doModelMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(tok.getModel.meta(s), false) }
            def doReqMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(termCtx.reqMeta.get(s).orNull, false) }
            def doSysMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(U.sysEnv(s).orNull, false) }
            def doUserMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(termCtx.usrMeta.get(s).orNull, false) }
            def doConvMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(termCtx.convMeta.get(s).orNull, false) }
            def doCompMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(termCtx.compMeta.get(s).orNull, false) }
            def doIntentMeta(): Unit = get1Str() match { case (s, _) ⇒ pushAny(termCtx.intentMeta.get(s).orNull, false) }

            /*
             * Math operations.
             */
            def doAbs(): Unit = get1Any() match {
                case (a: JLong, f) ⇒ pushLong(Math.abs(a), f)
                case (a: JDouble, f) ⇒ pushDouble(Math.abs(a), f)
                case x ⇒ errParamType(fun, x, "numeric")
            }
            def doSquare(): Unit = get1Any() match {
                case (a: JLong, f) ⇒ pushLong(a * a, f)
                case (a: JDouble, f) ⇒ pushDouble(a * a, f)
                case x ⇒ errParamType(fun, x, "numeric")
            }
            def doCeil(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.ceil(a), f) }
            def doFloor(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.floor(a), f) }
            def doSignum(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.signum(a), f) }
            def doAcos(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.acos(a), f) }
            def doAsin(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.asin(a), f) }
            def doSin(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.sin(a), f) }
            def doCos(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.cos(a), f) }
            def doRint(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.rint(a), f) }
            def doRound(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushLong(Math.round(a), f) }
            def doSqrt(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.sqrt(a), f) }
            def doCbrt(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.cbrt(a), f) }
            def doAtan(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.atan(a), f) }
            def doTan(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.tan(a), f) }
            def doCosh(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.cosh(a), f) }
            def doSinh(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.sinh(a), f) }
            def doTanh(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.tanh(a), f) }
            def doLog(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.log(a), f) }
            def doLog1p(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.log1p(a), f) }
            def doLog10(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.log10(a), f) }
            def doDegrees(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.toDegrees(a), f) }
            def doRadians(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.toRadians(a), f) }
            def doExp(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.exp(a), f) }
            def doExpm1(): Unit = get1Double() match { case (a: JDouble, f) ⇒ pushDouble(Math.expm1(a), f) }
            def doRandom(): Unit = pushDouble(Math.random, false)
            def doPi(): Unit = pushDouble(Math.PI, false)
            def doEuler(): Unit = pushDouble(Math.E, false)
            def doPow(): Unit = get2Doubles() match { case (a1: JDouble, a2: JDouble, f) ⇒ pushDouble(Math.pow(a1, a2), f) }
            def doHypot(): Unit = get2Doubles() match { case (a1: JDouble, a2: JDouble, f) ⇒ pushDouble(Math.hypot(a1, a2), f) }
            def doAtan2(): Unit = get2Doubles() match { case (a1: JDouble, a2: JDouble, f) ⇒ pushDouble(Math.atan2(a1, a2), f) }

            /*
             * User operations.
             */
            def doUserId(): Unit = pushLong(termCtx.req.getUser.getId, false)

            /*
             * Company operations.
             */
            def doCompId(): Unit = pushLong(termCtx.req.getCompany.getId, false)

            /*
             * Request operations.
             */
            def doReqId(): Unit = pushAny(termCtx.req.getServerRequestId, false)

            /*
             * Date-time operations.
             */
            def doYear(): Unit = pushLong(LocalDate.now.getYear,false)
            def doMonth(): Unit = pushLong(LocalDate.now.getMonthValue,false)
            def doDayOfMonth(): Unit = pushLong(LocalDate.now.getDayOfMonth,false)
            def doDayOfWeek(): Unit = pushLong(LocalDate.now.getDayOfWeek.getValue,false)
            def doDayOfYear(): Unit = pushLong(LocalDate.now.getDayOfYear,false)

            def doJson(): Unit = get1Str() match { case (s, f) ⇒ pushAny(U.jsonToJavaMap(asString(s)), f) }
            def doIf(): Unit = {
                ensureStack(3)

                val (v1, v2, v3, f1, f2, f3) = pop3()

                if (!isBoolean(v1))
                    throw errParamType(fun, v1, "boolean")

                if (asBool(v1))
                    pushAny(v2, f1 || f2)
                else
                    pushAny(v3, f1 || f3)
            }

            fun match {
                // Metadata access.
                case "token_meta" ⇒ doTokenMeta()
                case "model_meta" ⇒ doModelMeta()
                case "intent_meta" ⇒ doIntentMeta()
                case "req_meta" ⇒ doReqMeta()
                case "user_meta" ⇒ doUserMeta()
                case "company_meta" ⇒ doCompMeta()
                case "sys_meta" ⇒ doSysMeta()
                case "conv_meta" ⇒ doConvMeta()

                // Converts JSON to map.
                case "json" ⇒ doJson()

                // Inline if-statement.
                case "if" ⇒ doIf()

                // Token functions.
                case "id" ⇒ pushAny(tok.getId, true)
                case "ancestors" ⇒ pushAny(tok.getAncestors, true)
                case "parent" ⇒ pushAny(tok.getParentId, true)
                case "groups" ⇒ pushAny(tok.getGroups, true)
                case "value" ⇒ pushAny(tok.getValue, true)
                case "aliases" ⇒ pushAny(tok.getAliases, true)
                case "start_idx" ⇒ pushLong(tok.getStartCharIndex, true)
                case "end_idx" ⇒ pushLong(tok.getEndCharIndex, true)

                // Request data.
                case "req_id" ⇒ doReqId()
                case "req_normtext" ⇒
                case "req_tstamp" ⇒
                case "req_addr" ⇒
                case "req_agent" ⇒

                // User data.
                case "user_id" ⇒ doUserId()
                case "user_fname" ⇒
                case "user_lname" ⇒
                case "user_email" ⇒
                case "user_admin" ⇒
                case "user_signup_tstamp" ⇒

                // Company data.
                case "comp_id" ⇒ doCompId()
                case "comp_name" ⇒
                case "comp_website" ⇒
                case "comp_country" ⇒
                case "comp_region" ⇒
                case "comp_city" ⇒
                case "comp_addr" ⇒
                case "comp_postcode" ⇒

                // String functions.
                case "trim" ⇒ doTrim()
                case "strip" ⇒ doTrim()
                case "uppercase" ⇒ doUppercase()
                case "lowercase" ⇒ doLowercase()
                case "is_alpha" ⇒ doIsAlpha()
                case "is_alphanum" ⇒ doIsAlphaNum()
                case "is_whitespace" ⇒ doIsWhitespace()
                case "is_num" ⇒ doIsNum()
                case "is_numspace" ⇒ doIsNumSpace()
                case "is_alphaspace" ⇒ doIsAlphaSpace()
                case "is_alphanumspace" ⇒ doIsAlphaNumSpace()
                case "substring" ⇒
                case "index" ⇒
                case "regex" ⇒
                case "soundex" ⇒
                case "split" ⇒ doSplit()
                case "split_trim" ⇒ doSplitTrim()
                case "replace" ⇒

                // Math functions.
                case "abs" ⇒ doAbs()
                case "ceil" ⇒ doCeil()
                case "floor" ⇒ doFloor()
                case "rint" ⇒ doRint()
                case "round" ⇒ doRound()
                case "signum" ⇒ doSignum()
                case "sqrt" ⇒ doSqrt()
                case "cbrt" ⇒ doCbrt()
                case "pi" ⇒ doPi()
                case "euler" ⇒ doEuler()
                case "acos" ⇒ doAcos()
                case "asin" ⇒ doAsin()
                case "atan" ⇒ doAtan()
                case "cos" ⇒ doCos()
                case "sin" ⇒ doSin()
                case "tan" ⇒ doTan()
                case "cosh" ⇒ doCosh()
                case "sinh" ⇒ doSinh()
                case "tanh" ⇒ doTanh()
                case "atn2" ⇒ doAtan2()
                case "degrees" ⇒ doDegrees()
                case "radians" ⇒ doRadians()
                case "exp" ⇒ doExp()
                case "expm1" ⇒ doExpm1()
                case "hypot" ⇒ doHypot()
                case "log" ⇒ doLog()
                case "log10" ⇒ doLog10()
                case "log1p" ⇒ doLog1p()
                case "pow" ⇒ doPow()
                case "rand" ⇒ doRandom()
                case "square" ⇒ doSquare()

                // Collection, statistical (incl. string) functions.
                case "list" ⇒ doList()
                case "map" ⇒ doMap()
                case "avg" ⇒
                case "max" ⇒ // Works for numerics as well.
                case "min" ⇒ // Works for numerics as well.
                case "stdev" ⇒
                case "sum" ⇒
                case "get" ⇒
                case "index" ⇒
                case "contains" ⇒
                case "tail" ⇒
                case "add" ⇒
                case "remove" ⇒
                case "first" ⇒
                case "last" ⇒
                case "keys" ⇒
                case "values" ⇒
                case "length" ⇒
                case "count" ⇒
                case "take" ⇒
                case "drop" ⇒
                case "size" ⇒
                case "length" ⇒
                case "reverse" ⇒
                case "is_empty" ⇒
                case "non_empty" ⇒
                case "to_string" ⇒

                // Date-time functions.
                case "year" ⇒ doYear()
                case "month" ⇒ doMonth()
                case "day_of_month" ⇒ doDayOfMonth()
                case "day_of_week" ⇒ doDayOfWeek()
                case "day_of_year" ⇒ doDayOfYear()
                case "hour" ⇒
                case "min" ⇒
                case "sec" ⇒
                case "week" ⇒
                case "quarter" ⇒
                case "msec" ⇒
                case "now" ⇒

                case _ ⇒ errUnknownFun(fun)
            }
        }
    }
}
