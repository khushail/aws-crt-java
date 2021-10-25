/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.crt.http;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.crt.http.HttpStream;

import static software.amazon.awssdk.crt.CRT.awsLastError;

/**
 * This class wraps aws-c-http to provide the basic HTTP request/response functionality via the AWS Common Runtime.
 *
 * HttpClientConnection represents a single connection to a HTTP service endpoint.
 *
 * This class is not thread safe and should not be called from different threads.
 */
public class HttpClientConnection extends CrtResource {
    /**
     * HTTP protocol version.
     */
    public enum AwsHTTPProtocolVersion {
        HTTP_1_0(1),
        HTTP_1_1(2),
        HTTP_2(3);

        private int protocolVersion;
        private static Map<Integer, AwsHTTPProtocolVersion> enumMapping = buildEnumMapping();

        AwsHTTPProtocolVersion(int value) {
            protocolVersion = value;
        }

        public static AwsHTTPProtocolVersion getEnumValueFromInteger(int value) {
            AwsHTTPProtocolVersion enumValue = enumMapping.get(value);
            if (enumValue != null) {
                return enumValue;
            }

            throw new RuntimeException("Illegal signature type value in signing configuration");
        }
        private static Map<Integer, AwsHTTPProtocolVersion> buildEnumMapping() {
            Map<Integer, AwsHTTPProtocolVersion> enumMapping = new HashMap<Integer, AwsHTTPProtocolVersion>();
            enumMapping.put(HTTP_1_0.getValue(), HTTP_1_0);
            enumMapping.put(HTTP_1_1.getValue(), HTTP_1_1);
            enumMapping.put(HTTP_2.getValue(), HTTP_2);

            return enumMapping;
        }

        public int getValue() {
            return protocolVersion;
        }
    }

    protected HttpClientConnection(long connectionBinding) {
        acquireNativeHandle(connectionBinding);
    }

    /**
     * Schedules an HttpRequest on the Native EventLoop for this HttpClientConnection.
     *
     * @param request The Request to make to the Server.
     * @param streamHandler The Stream Handler to be called from the Native EventLoop
     * @throws CrtRuntimeException if stream creation fails
     * @return The HttpStream that represents this Request/Response Pair. It can be closed at any time during the
     *          request/response, but must be closed by the user thread making this request when it's done.
     */
    public HttpStream makeRequest(HttpRequest request, HttpStreamResponseHandler streamHandler) throws CrtRuntimeException {
        if (isNull()) {
            throw new IllegalStateException("HttpClientConnection has been closed, can't make requests on it.");
        }

        HttpStream stream = httpClientConnectionMakeRequest(getNativeHandle(),
            request.marshalForJni(),
            request.getBodyStream(),
            new HttpStreamResponseHandlerNativeAdapter(streamHandler));
        if (stream == null || stream.isNull()) {
            throw new CrtRuntimeException(awsLastError());
        }

        return stream;
    }

    /**
     * Determines whether a resource releases its dependencies at the same time the native handle is released or if it waits.
     * Resources that wait are responsible for calling releaseReferences() manually.
     */
    @Override
    protected boolean canReleaseReferencesImmediately() { return true; }

    /**
     * Releases this HttpClientConnection back into the Connection Pool, and allows another Request to acquire this connection.
     */
    @Override
    protected void releaseNativeHandle() {
        if (!isNull()){
            httpClientConnectionReleaseManaged(getNativeHandle());
        }
    }

    /**
     * Shuts down the underlying http connection.  Even if this function is called, you still need to properly close
     * the connection as well in order to release the native resources.
     */
    public void shutdown() {
        httpClientConnectionShutdown(getNativeHandle());
    }

    public AwsHTTPProtocolVersion getVersion() {
        short version = httpClientConnectionGetVersion(getNativeHandle());
        return AwsHTTPProtocolVersion.getEnumValueFromInteger((int)version);
    };

    /** Called from Native when a new connection is acquired **/
    private static void onConnectionAcquired(CompletableFuture<HttpClientConnection> acquireFuture, long nativeConnectionBinding, int errorCode) {
        if (errorCode != CRT.AWS_CRT_SUCCESS) {
            acquireFuture.completeExceptionally(new HttpException(errorCode));
            return;
        }

        HttpClientConnection conn = new HttpClientConnection(nativeConnectionBinding);
        if(conn.getVersion() == AwsHTTPProtocolVersion.HTTP_2) {
            HttpClientConnection h2Conn = new Http2ClientConnection(nativeConnectionBinding);
            acquireFuture.complete(h2Conn);
        } else{
            acquireFuture.complete(conn);
        }
    }

    /*******************************************************************************
     * Native methods
     ******************************************************************************/
    private static native HttpStream httpClientConnectionMakeRequest(long connectionBinding,
                                                                     byte[] marshalledRequest,
                                                                     HttpRequestBodyStream bodyStream,
                                                                     HttpStreamResponseHandlerNativeAdapter responseHandler) throws CrtRuntimeException;

    private static native void httpClientConnectionShutdown(long connectionBinding) throws CrtRuntimeException;

    private static native void httpClientConnectionReleaseManaged(long connectionBinding) throws CrtRuntimeException;
    private static native short httpClientConnectionGetVersion(long connectionBinding) throws CrtRuntimeException;
}
