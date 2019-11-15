/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.asyncweb.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.apache.asyncweb.common.codec.HttpCodecUtils;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * A default implementation of {@link MutableHttpResponse}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultHttpResponse extends DefaultHttpMessage implements
        MutableHttpResponse {

    private static final long serialVersionUID = -3733889080525034446L;

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private String statusReasonPhrase = HttpResponseStatus.OK.getDescription();

    /**
     * Creates a new instance
     *
     */
    public DefaultHttpResponse() {
    }

    public void addCookie(String headerValue) {
        // TODO Implement addCookie(String headerValue)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setStatus(HttpResponseStatus status) {
        setStatus(status, status.getDescription());
    }

    public void setStatus(HttpResponseStatus status, String statusReasonPhrase) {
        if (status == null) {
            throw new NullPointerException("status");
        }
        this.status = status;

        setStatusReasonPhrase(statusReasonPhrase);
    }

    public String getStatusReasonPhrase() {
        return statusReasonPhrase;
    }

    public void setStatusReasonPhrase(String statusReasonPhrase) {
        if (statusReasonPhrase == null) {
            throw new NullPointerException("statusReasonPhrase");
        }
        this.statusReasonPhrase = statusReasonPhrase;
    }

    /**
     * Thread-local DateFormat for old-style cookies
     */
    private static final ThreadLocal<DateFormat> EXPIRY_FORMAT_LOCAL = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat(
                    "EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone(
                    HttpCodecUtils.DEFAULT_TIME_ZONE_NAME));
            return format;
        }
    };

    /**
     * A date long long ago, formatted in the old style cookie expire format
     */
    private static final String EXPIRED_DATE = getFormattedExpiry(0);

    private static String getFormattedExpiry(long time) {
        DateFormat format = EXPIRY_FORMAT_LOCAL.get();
        return format.format(new Date(time));
    }


    public void normalize(HttpRequest request) {
        updateConnectionHeader(request);

        setHeader(HttpHeaderConstants.KEY_DATE, HttpDateFormat
                .getCurrentHttpDate());

        int contentLength;
        if (isBodyAllowed(request)) {
            contentLength = getContent().remaining();
        } else {
            setContent(IoBuffer.allocate(0));
            contentLength = 0;
        }

        if (!containsHeader(HttpHeaderConstants.KEY_TRANSFER_CODING)) {
            setHeader(HttpHeaderConstants.KEY_CONTENT_LENGTH, String
                    .valueOf(contentLength));
        }

        // Encode Cookies
        Set<Cookie> cookies = getCookies();
        if (!cookies.isEmpty()) {
            // Clear previous values.
            removeHeader(HttpHeaderConstants.KEY_SET_COOKIE);

            // And encode.
            for (Cookie c: cookies) {
                StringBuilder buf = new StringBuilder();
                buf.append(c.getName());
                buf.append('=');
                buf.append(c.getValue());
                if (c.getVersion() > 0) {
                    buf.append("; version=");
                    buf.append(c.getVersion());
                }
                if (c.getPath() != null) {
                    buf.append("; path=");
                    buf.append(c.getPath());
                }
                if (c.getDomain() != null) {
                    buf.append("; domain=");
                    buf.append(c.getDomain());
                }

                long expiry = c.getMaxAge();
                int version = c.getVersion();
                if (expiry >= 0) {
                    if (version == 0) {
                        String expires = expiry == 0 ? EXPIRED_DATE
                                : getFormattedExpiry(System.currentTimeMillis()
                                        + 1000 * expiry);
                        buf.append("; Expires=");
                        buf.append(expires);
                    } else {
                        buf.append("; max-age=");
                        buf.append(c.getMaxAge());
                    }
                }

                if (c.isSecure()) {
                    buf.append("; secure");
                }
                if (c.isHttpOnly()) {
                   buf.append("; HTTPOnly");
                }

                addHeader(HttpHeaderConstants.KEY_SET_COOKIE, buf.toString());
            }
        }

    }

    /**
     * Updates our "Connection" header based on we are "keep-aliving" the connection
     * after the response.
     */
    private void updateConnectionHeader(HttpRequest request) {
        if (getStatus().forcesConnectionClosure()) {
            setHeader(HttpHeaderConstants.KEY_CONNECTION,
                    HttpHeaderConstants.VALUE_CLOSE);
        } else if (request.isKeepAlive()) {
            setHeader(HttpHeaderConstants.KEY_CONNECTION,
                    HttpHeaderConstants.VALUE_KEEP_ALIVE);
        } else {
            setHeader(HttpHeaderConstants.KEY_CONNECTION,
                    HttpHeaderConstants.VALUE_CLOSE);
        }
    }

    /**
     * Determines whether we are allowed a response body.
     * A response body is allowed iff our response status allows a
     * body, and the original request method allows a body
     *
     * @return  <code>true</code> if a body is allowed
     */
    private boolean isBodyAllowed(HttpRequest request) {
        HttpMethod method = request.getMethod();
        return getStatus().allowsMessageBody() && method != null
                && method.isResponseBodyAllowed();
    }
}
