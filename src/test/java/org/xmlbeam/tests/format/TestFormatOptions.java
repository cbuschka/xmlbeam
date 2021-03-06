/**
 *  Copyright 2014 Sven Ewald
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.xmlbeam.tests.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;
import org.xmlbeam.util.intern.duplex.DuplexExpression;
import org.xmlbeam.util.intern.duplex.DuplexXPathParser;

/**
 * @author sven
 */
@SuppressWarnings("javadoc")
public class TestFormatOptions {

    @Test
    public void testFormatOptions1() {
        //                                                                       111111111122222222
        //                                                             0123456789012345678901234567
        DuplexExpression expression = new DuplexXPathParser(Collections.<String, String> emptyMap()).compile("/foo/bar/date using YYYYMMDD");
        //expression.dump();
        assertEquals("YYYYMMDD", expression.getExpressionFormatPattern());
        assertEquals("/foo/bar/date", expression.getExpressionAsStringWithoutFormatPatterns());
    }

    @Test
    public void testFormatOptions2() {
        //                                                                       111111111122222222
        //                                                             0123456789012345678901234567
        DuplexExpression expression = new DuplexXPathParser(Collections.<String, String> emptyMap()).compile("/foo/bar/date(:using yyyyMMdd:)");
        // expression.dump();
        assertEquals("yyyyMMdd", expression.getExpressionFormatPattern());
        assertEquals("/foo/bar/date", expression.getExpressionAsStringWithoutFormatPatterns());
    }

    @Test
    public void testFormatOptions2b() {
        //                                                                       111111111122222222
        //                                                             0123456789012345678901234567
        DuplexExpression expression = new DuplexXPathParser(Collections.<String, String> emptyMap()).compile("/foo/bar/date(:yyyyMMdd:)");
        // expression.dump();
        assertEquals("yyyyMMdd", expression.getExpressionFormatPattern());
        assertEquals("/foo/bar/date", expression.getExpressionAsStringWithoutFormatPatterns());
    }

    @Test
    public void testFormatOptions3() {
        //                                                                       111111111122222222
        //                                                             0123456789012345678901234567
        DuplexExpression expression = new DuplexXPathParser(Collections.<String, String> emptyMap()).compile("/foo/$bar(:yyyyMMdd:)/date");
        // expression.dump();
        assertNull(expression.getExpressionFormatPattern());
        assertEquals("/foo/$bar/date", expression.getExpressionAsStringWithoutFormatPatterns());
        assertEquals("yyyyMMdd", expression.getVariableFormatPattern("bar"));
    }

    @Test
    public void testFormatOptions4() {
        //                                                                       111111111122222222
        //                                                             0123456789012345678901234567
        DuplexExpression expression = new DuplexXPathParser(Collections.<String, String> emptyMap()).compile("/foo/$bar(:using yyyyMMdd:)/date");
        // expression.dump();
        assertNull(expression.getExpressionFormatPattern());
        assertEquals("/foo/$bar/date", expression.getExpressionAsStringWithoutFormatPatterns());
        assertEquals("yyyyMMdd", expression.getVariableFormatPattern("bar"));
    }

}
