package com.orbitz.nio;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>
 * This object is the HTTPResponse that is returned from a
 * successful HTTPClient transaction.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public class HTTPResponse {
    private int code;
    private String body;
    private Set headers;
    private long runningTime;
    private int contentLength;
    private HTTPMethod method;

    /**
     * Constructs a new HTTPResponse.
     *
     * @param   method The HTTPMethod that generated this response.
     */
    public HTTPResponse(HTTPMethod method) {
        this.method = method;
        this.runningTime = -1L;
    }

    /**
     * Returns the HTTP response code.
     *
     * @return  The response code;
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the HTTP response code.
     *
     * @param   code The HTTP response code.
     */
    void setCode(int code) {
        this.code = code;
    }

    /**
     * Returns the HTTP body.
     *
     * @return  The HTTP body.
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the HTTP body.
     *
     * @param   body The HTTP body.
     */
    void setBody(String body) {
        this.body = body;
    }

    /**
     * Returns an array of the all the HTTP headers.
     *
     * @return  The HTTP headers.
     */
    public NameValuePair[] getHeaders() {
        NameValuePair[] headers = new NameValuePair[this.headers.size()];
        this.headers.toArray(headers);
        return headers;
    }

    /**
     * Sets the HTTP headers.
     *
     * @param   headers The HTTP headers.
     */
    void setHeaders(NameValuePair[] headers) {
        if (headers == null) {
            this.headers = new LinkedHashSet(Arrays.asList(headers));
        }
    }

    /**
     * Returns the HTTPMethod that generated this HTTP response.
     *
     * @return  The HTTPMethod and never null.
     */
    public HTTPMethod getMethod() {
        return method;
    }

    /**
     * Returns the running time of the HTTP request.
     *
     * @return  The running time.
     */
    public long getRunningTime() {
        return this.runningTime;
    }

    /**
     * Sets the running time of the HTTP reqeust.
     *
     * @param   runningTime The running time.
     */
    void setRunningTime(long runningTime) {
        this.runningTime = runningTime;
    }

    /**
     * Returns the content length of the HTTP body.
     *
     * @return  The HTTP content body length.
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Sets the content length of the HTTP body.
     *
     * @param   contentLength The HTTP content body length.
     */
    void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
}