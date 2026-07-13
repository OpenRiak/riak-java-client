package com.basho.riak.client.core;

import com.basho.riak.client.core.netty.RiakResponseException;
import com.basho.riak.protobuf.RiakKvPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.basho.riak.protobuf.RiakPB;
import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpExternal;
import com.ericsson.otp.erlang.OtpInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Encapsulates the raw bytes sent to or received from Riak.
 *
 * @author Brian Roach <roach at basho dot com>
 * @author Sergey Galkin <sgalkin at basho dot com>
 * @since 2.0
 */
public final class RiakMessage {
    private static final Logger logger = LoggerFactory.getLogger(RiakMessage.class);
    private final byte code;
    private final byte[] data;
    private final RiakResponseException riakError;
    private static final String ERROR_RESP = "rpberrorresp";

    public RiakMessage(byte code, byte[] data) {
        this(code, data, true);
    }

    public RiakMessage(byte code, byte[] data, boolean doErrorCheck) {
        this.code = code;
        this.data = data;

        if (doErrorCheck) {
            switch (this.code) {
                case RiakMessageCodes.MSG_ErrorResp:
                    this.riakError = getRiakErrorFromPbuf(this.data);
                    break;
                case RiakMessageCodes.MSG_TsTtbMsg:
                    OtpInputStream ttbInputStream = new OtpInputStream(data);
                    this.riakError = getRiakErrorFromTtb(ttbInputStream);
                    break;
                case RiakMessageCodes.MSG_CloneResp: // CloneResp includes the error in its response
                    this.riakError = getRiakErrorFromCloneResp(data);
                    break;
                default:
                    this.riakError = null;
            }
        } else {
            this.riakError = null;
        }
    }

    private RiakResponseException getRiakErrorFromCloneResp(byte[] data) {
        try {
            RiakKvPB.RpbCloneResp resp = RiakKvPB.RpbCloneResp.parseFrom(data);
            if (resp.hasError()) {
                OtpInputStream is = new OtpInputStream(resp.getError().toByteArray());

                int firstByte = is.read1skip_version();
                is.reset();

                //When present, the error fields will be an ETF-encoded term. Likely values:
                // - atom
                // - tuple(atom, integer)
                // - tuple(atom, integer, integer)

                if (firstByte == OtpExternal.smallTupleTag || firstByte == OtpExternal.largeTupleTag) {
                    int arity = is.read_tuple_head();
                    String atom = is.read_atom();
                    int code = 0;
                    if (arity > 1) {
                        code = is.read_int();
                    }
                    return new RiakResponseException(code, atom);
                } else if (firstByte == OtpExternal.atomTag) {
                    String atom = is.read_atom();
                    return new RiakResponseException(0, atom);
                } else {
                    // Don't know try and decode it and use it
                    String msg = OtpErlangObject.decode(is).toString();
                    return new RiakResponseException(0, msg);
                }
            } else {
                // Doesn't have an error set
                return null;
            }
        } catch (InvalidProtocolBufferException ex) {
            logger.error("Could not parse protobuf message: RpbCloneResp", ex);
            return new RiakResponseException(0, "Could not parse protocol buffers error");
        } catch (OtpErlangDecodeException ex) {
            logger.error("Could not decode error field from RpbCloneRes", ex);
            return new RiakResponseException(0, "Could not decode error field from RpbCloneResp");
        }
    }

    private static RiakResponseException getRiakErrorFromPbuf(byte[] data) {
        try {
            RiakPB.RpbErrorResp err = RiakPB.RpbErrorResp.parseFrom(data);
            return new RiakResponseException(err.getErrcode(), err.getErrmsg().toStringUtf8());
        } catch (InvalidProtocolBufferException ex) {
            logger.error("exception", ex);
            return new RiakResponseException(0, "Could not parse protocol buffers error");
        }
    }

    public byte getCode() {
        return code;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isRiakError() {
        return this.riakError != null;
    }

    public RiakResponseException getRiakError() {
        return this.riakError;
    }

    private RiakResponseException getRiakErrorFromTtb(OtpInputStream ttbInputStream) {
        final String decodeErrorMsg = "Error decoding Riak TTB Response, unexpected format.";
        int ttbMsgArity;

        try {
            int firstByte = ttbInputStream.read1skip_version();

            if (firstByte != OtpExternal.smallTupleTag && firstByte != OtpExternal.largeTupleTag) {
                return null;
            }

            ttbInputStream.reset();

            ttbMsgArity = ttbInputStream.read_tuple_head();
        } catch (OtpErlangDecodeException ex) {
            logger.error(decodeErrorMsg + " Was expecting a tuple head.", ex);
            throw new IllegalArgumentException(decodeErrorMsg, ex);
        }

        if (ttbMsgArity == 3) {
            // NB: may be an error response
            String atom;
            try {
                atom = ttbInputStream.read_atom();
            } catch (OtpErlangDecodeException ex) {
                logger.error(decodeErrorMsg + " Was expecting an atom.", ex);
                throw new IllegalArgumentException(decodeErrorMsg, ex);
            }

            if (ERROR_RESP.equals(atom)) {
                try {
                    String errMsg = new String(ttbInputStream.read_binary(), StandardCharsets.UTF_8);
                    int errCode = ttbInputStream.read_int();
                    return new RiakResponseException(errCode, errMsg);
                } catch (OtpErlangDecodeException ex) {
                    logger.error(decodeErrorMsg, ex);
                    throw new IllegalArgumentException(decodeErrorMsg, ex);
                }
            }
        }

        return null;
    }
}
