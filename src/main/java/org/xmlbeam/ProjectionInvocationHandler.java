/**
 *  Copyright 2012 Sven Ewald
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
package org.xmlbeam;

import java.text.MessageFormat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlbeam.XBProjector.InternalProjection;
import org.xmlbeam.annotation.XBDelete;
import org.xmlbeam.annotation.XBDocURL;
import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.annotation.XBValue;
import org.xmlbeam.annotation.XBWrite;
import org.xmlbeam.dom.DOMAccess;
import org.xmlbeam.types.TypeConverter;
import org.xmlbeam.util.intern.DOMHelper;
import org.xmlbeam.util.intern.ReflectionHelper;

/**
 * @author <a href="https://github.com/SvenEwald">Sven Ewald</a>
 */
@SuppressWarnings("serial")
final class ProjectionInvocationHandler implements InvocationHandler, Serializable {
    private static final Pattern LEGAL_XPATH_SELECTORS_FOR_SETTERS = Pattern.compile("(?!^$)(^\\.?((/[a-z:A-Z0-9]+)*(/\\.\\.)*)*((/?@[a-z:A-Z0-9]+)|(/\\*))?$)");
    private final Node node;
    private final Class<?> projectionInterface;
    private final XBProjector projector;
    private final Map<Class<?>, Object> defaultInvokers ;//= new HashMap<Class<?>, Object>();

    ProjectionInvocationHandler(final XBProjector projector, final Node node, final Class<?> projectionInterface,Map<Class<?>, Object> defaultInvokers) {
        this.projector = projector;
        this.node = node;
        this.projectionInterface = projectionInterface;
        this.defaultInvokers=defaultInvokers;
     
    }

    /**
     * @param collection
     * @param parentElement
     * @param elementName
     */
    private void applyCollectionSetOnElement(Collection<?> collection, Element parentElement, String elementName) {
        final Document document = parentElement.getOwnerDocument();
        for (Object o : collection) {
            if (o == null) {
                continue;
            }
            if (!(o instanceof InternalProjection)) {
                final Element newElement = document.createElement(elementName);
                newElement.setTextContent(o.toString());
                parentElement.appendChild(newElement);
                continue;
            }
            final InternalProjection p = (InternalProjection) o;
            Element pElement = Node.DOCUMENT_NODE == p.getDOMNode().getNodeType() ? p.getDOMOwnerDocument().getDocumentElement() : (Element) p.getDOMNode();
            if (pElement == null) {
                continue;
            }
            Element clone = (Element) pElement.cloneNode(true);
            if (!elementName.equals(clone.getNodeName())) {
                clone = DOMHelper.renameElement(clone, elementName);
            }
            DOMHelper.ensureOwnership(document, clone);
            parentElement.appendChild(clone);
        }
    }

    private void applySingleSetProjectionOnElement(final InternalProjection projection, final Node parentNode) {
        DOMHelper.removeAllChildrenByName(parentNode, projection.getDOMNode().getNodeName());
        final Element newElement = (Element) projection.getDOMBaseElement().cloneNode(true);
        DOMHelper.ensureOwnership(parentNode.getOwnerDocument(), newElement);
        parentNode.appendChild(newElement);
    }

    private List<?> evaluateAsList(final XPathExpression expression, final Node node, final Method method) throws XPathExpressionException {
        final NodeList nodes = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
        final List<Object> linkedList = new LinkedList<Object>();
        final Class<?> targetType = findTargetComponentType(method);
        final TypeConverter typeConverter = projector.config().getTypeConverter();
        if (typeConverter.isConvertable(targetType)) {
            for (int i = 0; i < nodes.getLength(); ++i) {
                linkedList.add(typeConverter.convertTo(targetType, nodes.item(i).getTextContent()));
            }
            return linkedList;
        }
        if (targetType.isInterface()) {
            for (int i = 0; i < nodes.getLength(); ++i) {
                InternalProjection subprojection = (InternalProjection) projector.projectDOMNode(nodes.item(i), targetType);
                linkedList.add(subprojection);
            }
            return linkedList;
        }
        throw new IllegalArgumentException("Return type " + targetType + " is not valid for list or array component type returning from method " + method + " using the current type converter:" + projector.config().getTypeConverter()
                + ". Please change the return type to a sub projection or add a conversion to the type converter.");
    }

    /**
     * Setter projection methods may have multiple parameters. One of them may
     * be annotated with {@link XBValue} to select it as value to be set.
     * @param method
     * @return index of fist parameter annotated with {@link XBValue} annotation.
     */
    private int findIndexOfValue(Method method) {
        int index = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation a : annotations) {
                if (XBValue.class.equals(a.annotationType())) {
                    return index;
                }
            }
            ++index;
        }
        return 0; // If no attribute is annotated, the first one is taken.
    }

    /**
     * When reading collections, determine the collection component type. 
     * @param method
     * @return
     */
    private Class<?> findTargetComponentType(final Method method) {        
        if (method.getReturnType().isArray()) {
            return method.getReturnType().getComponentType();
        }
        assert method.getAnnotation(XBRead.class)!=null;
        final Class<?> targetType = method.getAnnotation(XBRead.class).targetComponentType();
        if (XBRead.class.equals(targetType)) {
            throw new IllegalArgumentException("When using List as return type for method " + method + ", please specify the list content type in the " + XBRead.class.getSimpleName() + " annotaion. I can not determine it from the method signature.");
        }
        return targetType;
    }

    private Node getNodeForMethod(final Method method, final Object[] args) throws SAXException, IOException, ParserConfigurationException {
        final XBDocURL docURL = method.getAnnotation(XBDocURL.class);
        if (docURL != null) {
            String uri = projector.config().getExternalizer().resolveURL(docURL.value(), method, args);
            final Map<String, String> requestParams = projector.io().filterRequestParamsFromParams(uri, args);
            uri = MessageFormat.format(uri, args);
            return DOMHelper.getDocumentFromURL(projector.config().createDocumentBuilder(), uri, requestParams, projectionInterface);
        }
        return node;
    }

    /**
     * Determine a methods return value that does not depend on the methods execution. Possible
     * values are void or the proxy itself (would be "this").
     * 
     * @param method
     * @return
     */
    private Object getProxyReturnValueForMethod(final Object proxy, final Method method) {
        if (!ReflectionHelper.hasReturnType(method)) {
            return null;
        }
        if (method.getReturnType().equals(method.getDeclaringClass())) {
            return proxy;
        }
        throw new IllegalArgumentException("Method " + method + " has illegal return type \"" + method.getReturnType() + "\". I don't know what to return. I expected void or " + method.getDeclaringClass().getSimpleName());
    }

    /**
     * Find the "me" attribute (which is a replacement for "this") and inject 
     * the projection proxy instance.
     * @param me
     * @param target
     */
    private void injectMeAttribute(InternalProjection me, Object target) {
        final Class<?> projectionInterface = me.getProjectionInterface();
        for (Field field : target.getClass().getDeclaredFields()) {
            if (!isValidMeField(field, projectionInterface)) {
                continue;
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                field.set(target, me);
                return;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Mixin " + target.getClass().getSimpleName() + " needs an attribute \"private " + projectionInterface.getSimpleName() + " me;\" to be able to access the projection.");
    }

    private boolean isValidMeField(Field field, Class<?> projInterface) {
        if (field == null) {
            return false;
        }
        if (!"me".equalsIgnoreCase(field.getName())) {
            return false;
        }
        if (DOMAccess.class.equals(field.getType())) {
            return true;
        }
        return field.getType().isAssignableFrom(projInterface);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final XBRead readAnnotation = method.getAnnotation(XBRead.class);
        if (readAnnotation != null) {
            return invokeGetter(proxy, method, MessageFormat.format(projector.config().getExternalizer().resolveXPath(readAnnotation.value(), method, args), args), args);
        }

        final XBWrite writeAnnotation = method.getAnnotation(XBWrite.class);
        if (writeAnnotation != null) {
            return invokeSetter(proxy, method, MessageFormat.format(projector.config().getExternalizer().resolveXPath(writeAnnotation.value(), method, args), args), args);
        }

        final XBDelete delAnnotation = method.getAnnotation(XBDelete.class);
        if (delAnnotation != null) {
            return invokeDeleter(proxy, method, MessageFormat.format(projector.config().getExternalizer().resolveXPath(delAnnotation.value(), method, args), args));
        }

        final Class<?> methodsDeclaringInterface = ReflectionHelper.findDeclaringInterface(method, projectionInterface);
        final Object customInvoker = projector.mixins().getProjectionMixin(projectionInterface, methodsDeclaringInterface);

        if (customInvoker != null) {
            injectMeAttribute((InternalProjection) proxy, customInvoker);
            return method.invoke(customInvoker, args);
        }

        final Object defaultInvoker = defaultInvokers.get(methodsDeclaringInterface);
        if (defaultInvoker != null) {
            return method.invoke(defaultInvoker, args);
        }

        throw new IllegalArgumentException("I don't known how to invoke method " + method + ". Did you forget to add a XB*-annotation or to register a mixin?");
    }

    /**
     * @param proxy
     * @param format
     */
    private Object invokeDeleter(Object proxy, Method method, String path) throws Throwable {
        final Document document = DOMHelper.getOwnerDocumentFor(node);
        final XPath xPath = projector.config().createXPath(document);
        final XPathExpression expression = xPath.compile(path);
        NodeList nodes = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); ++i) {
            if (Node.ATTRIBUTE_NODE == nodes.item(i).getNodeType()) {
                Attr attr = (Attr) nodes.item(i);
                attr.getOwnerElement().removeAttributeNode(attr);
                continue;
            }
            Node parentNode = nodes.item(i).getParentNode();
            if (parentNode == null) {
                continue;
            }
            parentNode.removeChild(nodes.item(i));
        }
        return getProxyReturnValueForMethod(proxy, method);
    }

    private Object invokeGetter(final Object proxy, final Method method, final String path, final Object[] args) throws Throwable {
        final Node node = getNodeForMethod(method, args);
        final Document document = DOMHelper.getOwnerDocumentFor(node);
        final XPath xPath = projector.config().createXPath(document);
        final XPathExpression expression = xPath.compile(path);
        final Class<?> returnType = method.getReturnType();
        if (projector.config().getTypeConverter().isConvertable(returnType)) {
            String data = (String) expression.evaluate(node, XPathConstants.STRING);
            try {
                return projector.config().getTypeConverter().convertTo(returnType, data);
            } catch (NumberFormatException e) {
                throw new NumberFormatException(e.getMessage() + " XPath was:" + path);
            }
        }
        if (List.class.equals(returnType)) {
            return evaluateAsList(expression, node, method);
        }
        if (returnType.isArray()) {
            List<?> list = evaluateAsList(expression, node, method);
            return list.toArray((Object[]) java.lang.reflect.Array.newInstance(returnType.getComponentType(), list.size()));
        }
        if (returnType.isInterface()) {
            Node newNode = (Node) expression.evaluate(node, XPathConstants.NODE);
            if (newNode == null) {
                return null;
            }
            InternalProjection subprojection = (InternalProjection) projector.projectDOMNode(newNode, returnType);
            return subprojection;
        }
        throw new IllegalArgumentException("Return type " + returnType + " of method " + method + " is not supported. Please change to an projection interface, a List, an Array or one of current type converters types:" + projector.config().getTypeConverter());
    }

    private Object invokeSetter(final Object proxy, final Method method, final String path, final Object[] args) throws Throwable {
        if (!LEGAL_XPATH_SELECTORS_FOR_SETTERS.matcher(path).matches()) {
            throw new IllegalArgumentException("Method " + method + " was invoked as setter and did not have an XPATH expression with an absolute path to an element or attribute:\"" + path + "\"");
        }
        if (!ReflectionHelper.hasParameters(method)) {
            throw new IllegalArgumentException("Method " + method + " was invoked as setter but has no parameter. Please add a parameter so this method could actually change the DOM.");
        }
        if (method.getAnnotation(XBDocURL.class) != null) {
            throw new IllegalArgumentException("Method " + method + " was invoked as setter but has a @" + XBDocURL.class.getSimpleName() + " annotation. Defining setters on external projections is not valid, because setters always change parts of documents.");
        }
        final String pathToElement = path.replaceAll("/?@.*", "");
        final Node settingNode = getNodeForMethod(method, args);
        final Document document = DOMHelper.getOwnerDocumentFor(settingNode);
        assert document != null;
        final int findIndexOfValue = findIndexOfValue(method);
        final Object valueToSet = args[findIndexOfValue];
        final Class<?> typeToSet = method.getParameterTypes()[findIndexOfValue];
        final boolean isMultiValue = isMultiValue(typeToSet);

        if ("/*".equals(pathToElement)) { // Setting a new root element.
            if (isMultiValue) {
                throw new IllegalArgumentException("Method " + method + " was invoked as setter changing the document root element, but tries to set multiple values.");
            }
            if (valueToSet == null) {
                DOMHelper.setDocumentElement(document, null);
                return getProxyReturnValueForMethod(proxy, method);
            }
            if (!(valueToSet instanceof InternalProjection)) {
                throw new IllegalArgumentException("Method " + method + " was invoked as setter changing the document root element. Expected value type was a projection so I can determine a element name. But you provided a " + valueToSet);
            }
            InternalProjection projection = (InternalProjection) valueToSet;
            Element element = projection.getDOMBaseElement();
            assert element != null;
            DOMHelper.setDocumentElement(document, element);
            return getProxyReturnValueForMethod(proxy, method);
        }

        if (isMultiValue) {
            if (path.contains("@")) {
                throw new IllegalArgumentException("Method " + method + " was invoked as setter changing some attribute, but was declared to set multiple values. I can not create multiple attributes for one path.");
            }
            final String path2Parent = pathToElement.replaceAll("/[^/]+$", "");
            final String elementName = pathToElement.replaceAll(".*/", "");
            final Element parentElement = DOMHelper.ensureElementExists(document, path2Parent);
            DOMHelper.removeAllChildrenByName(parentElement, elementName);
            if (valueToSet == null) {
                return getProxyReturnValueForMethod(proxy, method);
            }
            Collection<?> collection2Set = valueToSet.getClass().isArray() ? ReflectionHelper.array2ObjectList(valueToSet) : (Collection<?>) valueToSet;
            applyCollectionSetOnElement(collection2Set, parentElement, elementName);
            return getProxyReturnValueForMethod(proxy, method);
        }

        if (valueToSet instanceof InternalProjection) {
            String pathToParent = pathToElement.replaceAll("/[^/]*$", "");
            Element parentNode = DOMHelper.ensureElementExists(document, pathToParent);
            applySingleSetProjectionOnElement((InternalProjection) valueToSet, parentNode);
            return getProxyReturnValueForMethod(proxy, method);
        }

        Element elementToChange;
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            elementToChange = DOMHelper.ensureElementExists(document, pathToElement);
        } else {
            assert node.getNodeType() == Node.ELEMENT_NODE;
            elementToChange = DOMHelper.ensureElementExists(document, (Element) node, pathToElement);
        }

        if (path.contains("@")) {
            String attributeName = path.replaceAll(".*@", "");
            DOMHelper.setOrRemoveAttribute(elementToChange, attributeName, valueToSet == null ? null : valueToSet.toString());
            return getProxyReturnValueForMethod(proxy, method);
        }
        if (valueToSet == null) {
            DOMHelper.removeAllChildrenByName(elementToChange, "*");
        } else {
            elementToChange.setTextContent(valueToSet.toString());
        }
        return getProxyReturnValueForMethod(proxy, method);
    }

    /**
     * @param typeToSet
     * @return
     */
    private boolean isMultiValue(Class<?> type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }
}
