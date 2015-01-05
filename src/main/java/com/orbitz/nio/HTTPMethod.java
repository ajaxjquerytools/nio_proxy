package com.orbitz.nio;

import java.io.IOException;
import java.net.URL;

/**
 * <p>
 * This interface defines the HTTP methosd (GET and POST) and the
 * operations associated with them.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public interface HTTPMethod {
    /**
     * Returns the URL of this method.
     *
     * @return  The URL.
     */
    URL getURL();

    /**
     * Converts the HTTPMethod into an HTTP compliant request message String.
     *
     * @return  The HTTP request message String.
     * @throws  java.io.IOException If the HTTP request message String could not be built.
     */
    String toHTTP() throws IOException;

    /**
     * Returns the HTTP method name as dictated by the HTTP specifciaton.
     *
     * @return   The HTTP method name.
     */
    String getMethodName();

    /**
     * Adds a new HTTP header.
     *
     * @param   header The new header.
     */
    void addHeader(NameValuePair header);

    /**
     * Adds a new HTTP parameter.
     *
     * @param   param The new parameter.
     */
    void addParameter(NameValuePair param);

    /**
     * Gets the parameter, previously added, with the given key.
     *
     * @param   key The parameter key.
     * @return  The parameter or null if one was not previously added with the
     *          given key.
     */
    NameValuePair getParameter(String key);

    /**
     * Gets the HTTP response, if the HTTP request was successful.
     *
     * @return  The response or null if the HTTP request was not successful.
     */
    HTTPResponse getResponse();

    /**
     * Sets the HTTP response, if the HTTP request was successful.
     *
     * @param   response The HTTP response.
     */
    void setResponse(HTTPResponse response);
}