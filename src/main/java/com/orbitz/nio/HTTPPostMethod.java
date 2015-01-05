package com.orbitz.nio;

import java.io.IOException;
import java.net.URL;

/**
 * <p>
 * This class implements the HTTP post method.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public class HTTPPostMethod extends HTTPBaseMethod {
    public HTTPPostMethod(URL url) {
        super(url);
    }

    /**
     * Converts the HTTP post method to an HTTP request message String.
     *
     * @return  The HTTP request message String and never null.
     * @throws  java.io.IOException If the message String couldn't be built.
     */
    public String toHTTP() throws IOException {
        String req = getRequestString();
        String params = getParameterString();
        addHeader(new NameValuePair("Content-type", "application/x-www-form-urlencoded"));
        addHeader(new NameValuePair("Content-length", String.valueOf(params.length())));
        String headers = getHeaderString();

        StringBuffer sb = new StringBuffer();
        sb.append(req);
        sb.append(headers);
        sb.append(params);
        return sb.toString();
    }

    /**
     * Returns the URL path. HTTP posts do not contain additional information on the
     * path.
     *
     * @param   path The path to convert or add to if necessary. This method just
     *          returns the path as given.
     * @return  The path parameter.
     */
    protected String buildPath(String path) {
        return path;
    }

    /**
     * Returns POST.
     */
    public String getMethodName() {
        return "POST";
    }
}