package com.orbitz.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.logging.Logger;


/**
 * <p>
 * This class implements an HTTP get method.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public class HTTPGetMethod extends HTTPBaseMethod {
    private static final Logger log = Logger.getLogger(HTTPGetMethod.class.getName());
    public HTTPGetMethod(URL url) {
        super(url);
    }

    /**
     * Converts the HTTP get method to an HTTP request message String.
     *
     * @return  The HTTP request message String and never null.
     * @throws  java.io.IOException If the message String couldn't be built.
     */
    public String toHTTP() throws IOException {
        String req = getRequestString();
        String headers = getHeaderString();

        StringBuffer sb = new StringBuffer();
        sb.append(req);
        sb.append(headers);
        log.fine(sb.toString());
        return sb.toString();
    }

    /**
     * Appends the parameter String to the path.
     *
     * @param   path The path to append the parameter String to.
     * @return  The path with any parameters.
     * @throws  java.io.UnsupportedEncodingException If the parameters could not be encoded
     *          properly.
     */
    protected String buildPath(String path) throws UnsupportedEncodingException {
        String params = getParameterString();
        if (params.length() > 0) {
            path = path + "?" + params;
        }

        return path;
    }

    /**
     * Returns GET.
     */
    public String getMethodName() {
        return "GET";
    }
}