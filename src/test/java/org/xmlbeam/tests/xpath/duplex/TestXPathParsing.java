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
package org.xmlbeam.tests.xpath.duplex;

import java.io.StringReader;

import org.junit.Test;
import org.xmlbeam.util.intern.duplex.org.w3c.xqparser.SimpleNode;
import org.xmlbeam.util.intern.duplex.org.w3c.xqparser.XParser;

/**
 * @author sven
 */
public class TestXPathParsing {
    @Test
    public void testXPathParsing() throws Exception {
        String xpath = "let $incr :=       function($n) {$n+1}  \n return $incr(2)";
        //String xpath = "//hoo";
        XParser parser = new XParser(new StringReader(xpath));
        SimpleNode node = parser.START();
        node.dump("");
    }
}
