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
package org.xmlbeam.tutorial.e16_schemaHandling;

import org.xmlbeam.annotation.XBDocURL;
import org.xmlbeam.annotation.XBRead;

@SuppressWarnings("javadoc")
//START SNIPPET: SchemaHandling
@XBDocURL("resource://vegetables.xml")
public interface Vegetables {
    @XBRead("/xbdefaultns:Vegetables/xbdefaultns:Vegetable[@name='{0}']")
    Vegetable getVegetable(String name);
}
//END SNIPPET: SchemaHandling
