/**
 *  Copyright 2013 Sven Ewald
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
package org.xmlbeam.io;

import java.net.URISyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xmlbeam.annotation.XBDocURL;
import org.xmlbeam.util.intern.DocScope;
import org.xmlbeam.util.intern.Scope;

/**
 * A ProjectionIO is responsible for every IO operation related to projections. Before coding your
 * own IO implementation, you should have a look at the tutorials for usage examples.
 */
public interface ProjectionIO {

    /**
     * Get access to the file IO.
     * @param file
     * @return a XBFileIO for this file.
     */
    @Scope(DocScope.IO)
    FileIO file(File file);

    
    /**
     * Get access to the file IO.
     * @param fileName
     * @return a XBFileIO for this filename.
     */
    @Scope(DocScope.IO)
    FileIO file(String fileName);

    /**
     * Get access to the url IO.
     * @param url
     * @return a XBUrlIO for this url
     */
    @Scope(DocScope.IO)
    UrlIO url(String url);
    
    /**
     * Get access to the stream IO
     * @param is
     * @return a XBStreamInput for this InputStream
     */
    @Scope(DocScope.IO)
    StreamInput stream(InputStream is);

    /**
     * Get access to the stream IO
     * @param os
     * @return a XBStreamOutput for this OutputStream
     */
    @Scope(DocScope.IO)
    StreamOutput stream(OutputStream os);

    /**
     * Create a new projection using a {@link XBDocURL} annotation on this interface. When the
     * XBDocURL starts with the protocol identifier "resource://" the class loader of the projection
     * interface will be used to read the resource from the current class path.
     * 
     * @param projectionInterface
     *            a public interface.
     * @param optionalParams 
     * @return a new projection instance
     * @throws IOException
     */
    @Scope(DocScope.IO)
    <T> T fromURLAnnotation(final Class<T> projectionInterface, Object... optionalParams) throws IOException;

    /**
     * Write projection document to url (file or http post) of {@link XBDocURL} annotation.
     * 
     * @param projection
     * @param optionalParams 
     * @return response of http post or null for file urls.
     * @throws IOException
     * @throws URISyntaxException
     */
    @Scope(DocScope.IO)
    String toURLAnnotationViaPOST(final Object projection, Object... optionalParams) throws IOException, URISyntaxException;

}