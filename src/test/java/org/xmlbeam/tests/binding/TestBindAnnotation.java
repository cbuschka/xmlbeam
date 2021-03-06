/**
 *  Copyright 2016 Sven Ewald
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
package org.xmlbeam.tests.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.xmlbeam.testutils.DOMDiagnoseHelper.assertXMLStringsEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.xmlbeam.XBProjector;
import org.xmlbeam.XBProjector.Flags;
import org.xmlbeam.annotation.XBAuto;
import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.exceptions.XBException;
import org.xmlbeam.types.CloseableMap;
import org.xmlbeam.types.XBAutoList;
import org.xmlbeam.types.XBAutoMap;
import org.xmlbeam.types.XBAutoValue;

/**
 *
 */
@SuppressWarnings("javadoc")
public class TestBindAnnotation {

    private final static XBProjector projector = new XBProjector(Flags.TO_STRING_RENDERS_XML);

    private final Projection projection = projector.projectEmptyDocument(Projection.class);

    private final static String XMLFORMAP = "<root><map><element1>value1</element1><element2><element3 att1=\"attvalue1\" >value2</element3></element2></map></root>";
    private final Projection mapProjection = new XBProjector(Flags.TO_STRING_RENDERS_XML).projectXMLString(XMLFORMAP, Projection.class);

    interface Subprojection {

    }

    interface Projection {

        @XBAuto("/root/first/second/@attr")
        XBAutoValue<String> attr();

        @XBAuto("/root/list/element")
        List<String> list();

        @XBAuto("/root/map")
        XBAutoMap<String> map();

        @XBAuto("/root/map")
        XBAutoMap<Subprojection> mapSubProjection();

        @XBRead("/root/map")
        Map<String, String> map2();

    }

    interface InvalidProjection {
        @XBAuto("/root")
        Map<Integer, Integer> invalidReturnType();
    }

    @Test
    public void testProjectionBindMethod() {
        projection.attr().set("foo");
        assertXMLStringsEquals("<root><first><second attr=\"foo\"/></first></root>", projection.toString());
    }

    @Test
    public void testProjectionBindList() {
        List<String> list = projection.list();
        list.add("foo");
        list.add("bar");
        assertEquals("[foo, bar]", list.toString());
    }

    @Test
    public void testProjectionBindMapCreation() {
        Map<String, String> map = mapProjection.map();
        assertEquals(null, map.get("a/b/c"));
        map.put("./a/b/c", "newValue");
        assertEquals("value1", map.get("./element1"));
        assertEquals("newValue", map.get("a/b/c"));
    }

    @Test
    public void testProjectionAutoMapRemove() {
        Map<String, String> map = mapProjection.map();
        assertEquals("value2", map.get("element2/element3"));
        assertEquals("", map.remove("element2"));
        assertNull(map.remove("nonexisting"));
        assertEquals(null, map.get("element2/element3"));
    }

    @Test
    public void testProjectionBindMapEmptyAndSize() {
        Map<String, String> map = mapProjection.map();
        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
    }

    @Test
    public void testProjectionBindMapValues() {
        Map<String, String> map = mapProjection.map();
        System.out.println(mapProjection);
        System.out.println(map.keySet());
        assertEquals("[value1, value2, attvalue1]", map.values().toString());
        map.clear();
        assertEquals("[]", map.values().toString());
        map.put("ele1/ele2/@att", "someAttValue");
        assertEquals("[./ele1/ele2/@att=someAttValue]", map.entrySet().toString());
    }

    @Test
    public void testProjectionBindMapValues2() {
        Map<String, String> map = mapProjection.map2();
        assertEquals("[value1, value2, attvalue1]", map.values().toString());
        map.clear();
        assertEquals("[]", map.values().toString());
        map.put("ele1/ele2/@att", "someAttValue");
        assertEquals("[./ele1/ele2/@att=someAttValue]", map.entrySet().toString());
    }

    @Test
    public void testProjectionAutoMapFullDocument() {
        XBAutoMap<String> map = projector.autoMapEmptyDocument(String.class);
        map.put("someroot/elements/element1", "value1");
        map.put("someroot/elements/element2", "value2");
        map.put("someroot/elements[with/subelement='oink']/element3", "value3");
        map.put("someroot/elements[with/subelement='oink']/element4", "value4");
        map.put("someroot/elements/element5", "value5");
        System.out.println(projector.asString(map));
    }

    @Test
    public void testAmbigousPaths() {
        XBAutoMap<String> map = projector.autoMapEmptyDocument(String.class);
        map.put("someroot/elements[@pos='first']/sub[@pos='first']/element1", "value1");
        map.put("someroot/elements[@pos='second']/sub[@pos='second']/element2", "value2");
        map.put("someroot/elements[@pos='second']/sub[@pos='second']/element3", "value3");
        assertXMLStringsEquals("<someroot>\n" + "  <elements pos=\"first\">\n" + "    <sub pos=\"first\">\n" + "      <element1>value1</element1>\n" + "    </sub>\n" + "  </elements>\n" + "  <elements pos=\"second\">\n" + "    <sub pos=\"second\">\n"
                + "      <element2>value2</element2>\n" + "      <element3>value3</element3>\n" + "    </sub>\n" + "  </elements>\n" + "</someroot>\n", projector.asString(map));
    }

    @Test
    public void testPutSubprojection() {
        XBAutoMap<Projection> map = projector.autoMapEmptyDocument(Projection.class);
        assertTrue(map.isEmpty());
        map.put("root/a", projector.projectEmptyElement("x", Projection.class));
        map.put("root/b/y", projector.projectEmptyElement("y", Projection.class));
        assertFalse(map.isEmpty());
        assertEquals(4, map.size());
        System.out.println(projector.asString(map));
        assertXMLStringsEquals("<root>\n" + "  <x/>\n" + "  <b>\n" + "    <y/>\n" + "  </b>\n" + "</root>", projector.asString(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutSubprojectionToAttribute() {
        XBAutoMap<Projection> map = projector.autoMapEmptyDocument(Projection.class);
        assertTrue(map.isEmpty());
        map.put("root/@a", projector.projectEmptyElement("x", Projection.class));
        System.out.println(projector.asString(map));
    }

    @Test
    public void testSubProjection() {
        System.out.println(projector.asString(mapProjection));
        XBAutoMap<Subprojection> map = mapProjection.mapSubProjection();
        System.out.println(projector.asString(map));
        System.out.println(projector.asString(map.get("element2/element3")));
    }

    @Test(expected = XBException.class)
    public void testInvalidProjectionReturnType() {
        projector.projectXMLString("<xml></xml>", InvalidProjection.class);
    }

    @Test
    public void testMapNoNeedToEvaluate() {
        CloseableMap<String> map = projector.io().file("test.xml").bindXPath("/does/not/Exist").asMapOf(String.class);
        assertNull(map.get("another/nonexistend/path"));
    }

    @Test
    public void testMaptoString() {
        XBAutoMap<String> map = projector.onXMLString("<root><foo><bar>huhu</bar><bar>huhu2</bar></foo></root>").createMapOf(String.class);
        XBAutoList<String> list = map.getList("root/foo/bar");
        assertEquals("[huhu, huhu2]", list.toString());

    }

}
