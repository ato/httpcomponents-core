/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.core5.http.io.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.List;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.ByteArrayBuffer;
import org.apache.hc.core5.util.CharArrayBuffer;

/**
 * Static helpers for dealing with {@link HttpEntity}s.
 *
 * @since 4.0
 */
public final class EntityUtils {

    private EntityUtils() {
    }

    /**
     * Ensures that the entity content is fully consumed and the content stream, if exists,
     * is closed. The process is done, <i>quietly</i> , without throwing any IOException.
     *
     * @param entity the entity to consume.
     *
     *
     * @since 4.2
     */
    public static void consumeQuietly(final HttpEntity entity) {
        try {
          consume(entity);
        } catch (final IOException ignore) {
        }
    }

    /**
     * Ensures that the entity content is fully consumed and the content stream, if exists,
     * is closed.
     *
     * @param entity the entity to consume.
     * @throws IOException if an error occurs reading the input stream
     *
     * @since 4.1
     */
    public static void consume(final HttpEntity entity) throws IOException {
        if (entity == null) {
            return;
        }
        if (entity.isStreaming()) {
            final InputStream instream = entity.getContent();
            if (instream != null) {
                instream.close();
            }
        }
    }

    /**
     * Updates an entity in a response by first consuming an existing entity, then setting the new one.
     *
     * @param response the response with an entity to update; must not be null.
     * @param entity the entity to set in the response.
     * @throws IOException if an error occurs while reading the input stream on the existing
     * entity.
     * @throws IllegalArgumentException if response is null.
     *
     * @since 4.3
     */
    public static void updateEntity(
            final ClassicHttpResponse response, final HttpEntity entity) throws IOException {
        Args.notNull(response, "Response");
        consume(response.getEntity());
        response.setEntity(entity);
    }

    /**
     * Extracts {@code Content-Type} value from {@link HttpEntity} exactly as
     * specified by the {@code Content-Type} header of the entity. Returns {@code null}
     * if not specified.
     *
     * @param entity HTTP entity
     * @return content type
     * {@code Content-Type} value.
     * @throws UnsupportedCharsetException Thrown when the named charset is not available in
     * this instance of the Java virtual machine
     */
    public static ContentType getContentType(final HttpEntity entity) throws UnsupportedCharsetException {
        if (entity == null) {
            return null;
        }
        final String contentType = entity.getContentType();
        if (contentType != null) {
            return ContentType.parse(contentType);
        }
        return null;
    }

    /**
     * Extracts {@code Content-Type} value from {@link HttpEntity}. Returns {@code null}
     * if not specified or incorrect (could not be parsed)..
     *
     * @param entity HTTP entity
     * @return content type
     *
     * @since 4.4
     *
     */
    public static ContentType getContentTypeLenient(final HttpEntity entity) {
        if (entity == null) {
            return null;
        }
        final String contentType = entity.getContentType();
        if (contentType != null) {
            return ContentType.parseLenient(contentType);
        }
        return null;
    }

    /**
     * Extracts {@code Content-Type} value from {@link HttpEntity} or returns the default value
     * {@link ContentType#DEFAULT_TEXT} if not explicitly specified.
     *
     * @param entity HTTP entity
     * @return content type
     * {@code Content-Type} value.
     * @throws UnsupportedCharsetException Thrown when the named charset is not available in
     * this instance of the Java virtual machine
     */
    public static ContentType getContentTypeOrDefault(final HttpEntity entity) throws UnsupportedCharsetException {
        final ContentType contentType = getContentType(entity);
        return contentType != null ? contentType : ContentType.DEFAULT_TEXT;
    }

    /**
     * Extracts {@code Content-Type} value from {@link HttpEntity} or returns the default value
     * {@link ContentType#DEFAULT_TEXT} if not explicitly specified or incorrect (could not be parsed).
     *
     * @param entity HTTP entity
     * @return content type
     *
     * @since 4.4
     */
    public static ContentType getContentTypeLenientOrDefault(final HttpEntity entity) throws UnsupportedCharsetException {
        final ContentType contentType = getContentType(entity);
        return contentType != null ? contentType : ContentType.DEFAULT_TEXT;
    }

    /**
     * Read the contents of an entity and return it as a byte array.
     *
     * @param entity the entity to read from=
     * @return byte array containing the entity content. May be null if
     *   {@link HttpEntity#getContent()} is null.
     * @throws IOException if an error occurs reading the input stream
     * @throws IllegalArgumentException if entity is null or if content length &gt; Integer.MAX_VALUE
     */
    public static byte[] toByteArray(final HttpEntity entity) throws IOException {
        Args.notNull(entity, "Entity");
        final InputStream instream = entity.getContent();
        if (instream == null) {
            return null;
        }
        try {
            int i = (int) Args.checkContentLength(entity);
            if (i < 0) {
                i = 4096;
            }
            final ByteArrayBuffer buffer = new ByteArrayBuffer(i);
            final byte[] tmp = new byte[4096];
            int l;
            while((l = instream.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
            return buffer.toByteArray();
        } finally {
            instream.close();
        }
    }

    private static String toString(
            final HttpEntity entity,
            final ContentType contentType) throws IOException {
        final InputStream instream = entity.getContent();
        if (instream == null) {
            return null;
        }
        try {
            int i = (int) Args.checkContentLength(entity);
            if (i < 0) {
                i = 4096;
            }
            Charset charset = null;
            if (contentType != null) {
                charset = contentType.getCharset();
                if (charset == null) {
                    final ContentType defaultContentType = ContentType.getByMimeType(contentType.getMimeType());
                    charset = defaultContentType != null ? defaultContentType.getCharset() : null;
                }
            }
            if (charset == null) {
                charset = StandardCharsets.ISO_8859_1;
            }
            final Reader reader = new InputStreamReader(instream, charset);
            final CharArrayBuffer buffer = new CharArrayBuffer(i);
            final char[] tmp = new char[1024];
            int l;
            while((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
            return buffer.toString();
        } finally {
            instream.close();
        }
    }

    /**
     * Get the entity content as a String, using the provided default character set
     * if none is found in the entity.
     * If defaultCharset is null, the default "ISO-8859-1" is used.
     *
     * @param entity must not be null
     * @param defaultCharset character set to be applied if none found in the entity,
     * or if the entity provided charset is invalid or not available.
     * @return the entity content as a String. May be null if
     *   {@link HttpEntity#getContent()} is null.
     * @throws ParseException if header elements cannot be parsed
     * @throws IllegalArgumentException if entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws IOException if an error occurs reading the input stream
     * @throws java.nio.charset.UnsupportedCharsetException Thrown when the named entity's charset is not available in
     * this instance of the Java virtual machine and no defaultCharset is provided.
     */
    public static String toString(
            final HttpEntity entity, final Charset defaultCharset) throws IOException, ParseException {
        Args.notNull(entity, "Entity");
        ContentType contentType = null;
        try {
            contentType = getContentType(entity);
        } catch (final UnsupportedCharsetException ex) {
            if (defaultCharset == null) {
                throw new UnsupportedEncodingException(ex.getMessage());
            }
        }
        if (contentType != null) {
            if (contentType.getCharset() == null) {
                contentType = contentType.withCharset(defaultCharset);
            }
        } else {
            contentType = ContentType.DEFAULT_TEXT.withCharset(defaultCharset);
        }
        return toString(entity, contentType);
    }

    /**
     * Get the entity content as a String, using the provided default character set
     * if none is found in the entity.
     * If defaultCharset is null, the default "ISO-8859-1" is used.
     *
     * @param entity must not be null
     * @param defaultCharset character set to be applied if none found in the entity
     * @return the entity content as a String. May be null if
     *   {@link HttpEntity#getContent()} is null.
     * @throws ParseException if header elements cannot be parsed
     * @throws IllegalArgumentException if entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws IOException if an error occurs reading the input stream
     * @throws java.nio.charset.UnsupportedCharsetException Thrown when the named charset is not available in
     * this instance of the Java virtual machine
     */
    public static String toString(
            final HttpEntity entity, final String defaultCharset) throws IOException, ParseException {
        return toString(entity, defaultCharset != null ? Charset.forName(defaultCharset) : null);
    }

    /**
     * Read the contents of an entity and return it as a String.
     * The content is converted using the character set from the entity (if any),
     * failing that, "ISO-8859-1" is used.
     *
     * @param entity the entity to convert to a string; must not be null
     * @return String containing the content.
     * @throws ParseException if header elements cannot be parsed
     * @throws IllegalArgumentException if entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws IOException if an error occurs reading the input stream
     * @throws java.nio.charset.UnsupportedCharsetException Thrown when the named charset is not available in
     * this instance of the Java virtual machine
     */
    public static String toString(final HttpEntity entity) throws IOException, ParseException {
        Args.notNull(entity, "Entity");
        return toString(entity, getContentType(entity));
    }

    /**
     * Returns a list of {@link NameValuePair NameValuePairs} as parsed from an {@link HttpEntity}.
     * The encoding is taken from the entity's Content-Encoding header.
     * <p>
     * This is typically used while parsing an HTTP POST.
     *
     * @param entity
     *            The entity to parse
     * @return a list of {@link NameValuePair} as built from the URI's query portion.
     * @throws IOException
     *             If there was an exception getting the entity's data.
     */
    public static List<NameValuePair> parse(final HttpEntity entity) throws IOException {
        Args.notNull(entity, "HTTP entity");
        final ContentType contentType = EntityUtils.getContentType(entity);
        if (contentType == null || !contentType.getMimeType().equalsIgnoreCase(URLEncodedUtils.CONTENT_TYPE)) {
            return Collections.emptyList();
        }
        final long len = entity.getContentLength();
        Args.checkRange(len, 0, Integer.MAX_VALUE, "HTTP entity is too large");
        final Charset charset = contentType.getCharset() != null ? contentType.getCharset() : StandardCharsets.ISO_8859_1;
        final InputStream instream = entity.getContent();
        if (instream == null) {
            return Collections.emptyList();
        }
        final CharArrayBuffer buf;
        try {
            buf = new CharArrayBuffer(len > 0 ? (int) len : 1024);
            final Reader reader = new InputStreamReader(instream, charset);
            final char[] tmp = new char[1024];
            int l;
            while((l = reader.read(tmp)) != -1) {
                buf.append(tmp, 0, l);
            }

        } finally {
            instream.close();
        }
        if (buf.isEmpty()) {
            return Collections.emptyList();
        }
        return URLEncodedUtils.parse(buf, charset, '&');
    }

}
