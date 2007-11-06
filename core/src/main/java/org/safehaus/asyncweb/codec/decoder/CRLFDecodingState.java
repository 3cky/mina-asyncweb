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
package org.safehaus.asyncweb.codec.decoder;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.safehaus.asyncweb.codec.HttpCodecUtils;
import org.safehaus.asyncweb.codec.decoder.support.DecodingState;

/**
 * Decodes a single <code>CRLF</code>.
 * If it is found, the bytes are consumed and <code>Boolean.TRUE</code>
 * is provided as the product. Otherwise, read bytes are pushed back
 * to the stream, and <code>Boolean.FALSE</code> is provided as the
 * product.
 * Note that if we find a CR but do not find a following LF, we raise
 * an error.
 *
 * @author irvingd
 * @author trustin
 * @version $Rev$, $Date$
 */
public abstract class CRLFDecodingState implements DecodingState {

    private boolean hasCR;

    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        boolean found = false;
        boolean finished = false;
        while (in.hasRemaining()) {
            byte b = in.get();
            if (!hasCR) {
                if (b == HttpCodecUtils.CR) {
                    hasCR = true;
                } else {
                    if (b == HttpCodecUtils.LF) {
                        found = true;
                    } else {
                        in.position(in.position() - 1);
                        found = false;
                    }
                    finished = true;
                    break;
                }
            } else {
                if (b == HttpCodecUtils.LF) {
                    found = true;
                    finished = true;
                    break;
                } else {
                    HttpCodecUtils
                            .throwDecoderException("Expected LF after CR but was: "
                                    + b);
                }
            }
        }

        if (finished) {
            hasCR = false;
            return finishDecode(found, out);
        } else {
            return this;
        }
    }

    protected abstract DecodingState finishDecode(boolean foundCRLF,
            ProtocolDecoderOutput out) throws Exception;
}
