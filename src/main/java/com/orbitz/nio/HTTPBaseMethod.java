package com.orbitz.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This class is an abstract base class for the two HTTP methods,
 * get and post. It contains the majority of the HTTP request
 * message logic.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public abstract class HTTPBaseMethod implements HTTPMethod {
    private URL url;
    private Map params;
    private Map headers;
    private volatile HTTPResponse response;

    /**
     * Constructs a new HTTPBaseMethod that will be used to send an HTTP message to
     * the given URL.
     *
     * @param   url The url.
     */
    protected HTTPBaseMethod(URL url) {
        this.url = url;
        this.params = new LinkedHashMap(3);
        this.headers = new LinkedHashMap(3);
    }

    /**
     * Must be implemented by sub-classes to provide HTTP method specific path
     * information.
     *
     * @param   path The path to convert to the HTTP method specific path.
     * @return  The HTTP method specific path.
     * @throws  java.io.IOException If the path could not be built.
     */
    protected abstract String buildPath(String path) throws IOException;

    /**
     * Builds the HTTP request message string.
     *
     * @return  The HTTP request method.
     * @throws  java.io.IOException If the request String could not be built.
     */
    protected String getRequestString() throws IOException {
        String path = getURL().getPath();
        if (path == null || path.length() == 0) {
            path = "/";
        }

        return getMethodName() + " " + buildPath(path) + " HTTP/1.0\r\n";
    }

    /**
     * Builds the HTTP request header String, that is part of the HTTP request
     * message.
     *
     * @return  The HTTP header String.
     */
    protected String getHeaderString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = this.headers.values().iterator(); i.hasNext(); ) {
            NameValuePair pair = (NameValuePair)i.next();
            sb.append(pair.getKey()).append(": ").append(pair.getValue()).append("\r\n");
        }

        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * Builds the HTTP request parameter String.
     *
     * @return  The HTTP request parameter String.
     * @throws  java.io.UnsupportedEncodingException If the encoding of the URL parameters
     *          is invalid, which in our simple HTTP parser case is impossible because
     *          it only supportsd ISO-8859-1.
     */
    protected String getParameterString() throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = this.params.values().iterator(); i.hasNext(); ) {
            NameValuePair pair = (NameValuePair) i.next();
            String value = pair.getValue();
            value = URLEncoder.encode(value, "ISO-8859-1");
            sb.append(pair.getKey()).append("=").append(value);
            if (i.hasNext()) {
                sb.append("&");
            }
        }

        return sb.toString();
    }

    /**
     * Returns the set of parameters previously added, defensive copied.
     *
     * @return  The set of parameters.
     */
    public Set getParams() {
        return new HashSet(this.params.values());
    }

    /**
     * Returns the set of header parameters previously added, defensive copied.
     *
     * @return  The set of headers
     */
    public Set getHeaders() {
        return new HashSet(this.headers.values());
    }

    /**
     * Returns the URL of this method.
     *
     * @return  The URL.
     */
    public URL getURL() {
        return this.url;
    }

    /**
     * Adds a new HTTP header.
     *
     * @param   header The new header.
     */
    public void addHeader(NameValuePair header) {
        if (header != null) {
            this.headers.put(header.getKey(), header);
        }
    }

    /**
     * Adds a new HTTP parameter.
     *
     * @param   param The new parameter.
     */
    public void addParameter(NameValuePair param) {
        if (param != null) {
            this.params.put(param.getKey(), param);
        }
    }

    /**
     * Gets the parameter, previously added, with the given key.
     *
     * @param   key The parameter key.
     * @return  The parameter or null if one was not previously added with the
     *          given key.
     */
    public NameValuePair getParameter(String key) {
        return (NameValuePair) this.params.get(key);
    }

    /**
     * Gets the HTTP response, if the HTTP request was successful.
     *
     * @return  The response or null if the HTTP request was not successful.
     */
    public HTTPResponse getResponse() {
        return response;
    }

    /**
     * Sets the HTTP response, used by the NIOWorker only.
     *
     * @param   response The response.
     */
    public void setResponse(HTTPResponse response) {
        this.response = response;
    }
}