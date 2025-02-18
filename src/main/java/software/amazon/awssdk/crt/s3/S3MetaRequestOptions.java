/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.awssdk.crt.s3;

import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class S3MetaRequestOptions {

    /**
     * A Meta Request represents a group of generated requests that are being done on behalf of the
     * original request. For example, one large GetObject request can be transformed into a series
     * of ranged GetObject requests that are executed in parallel to improve throughput.
     *
     * The MetaRequestType is a hint of transformation to be applied.
     */
    public enum MetaRequestType {
        /**
         * The Default meta request type sends any request to S3 as-is (with no transformation). For example,
         * it can be used to pass a CreateBucket request.
         */
        DEFAULT(0),

        /**
         * The GetObject request will be split into a series of ranged GetObject requests that are
         * executed in parallel to improve throughput, when possible.
         */
        GET_OBJECT(1),

        /**
         * The PutObject request will be split into MultiPart uploads that are executed in parallel
         * to improve throughput, when possible.
         */
        PUT_OBJECT(2),

        /**
         * The CopyObject meta request performs a multi-part copy using multiple S3 UploadPartCopy requests
         * in parallel, or bypasses a CopyObject request to S3 if the object size is not large enough for
         * a multipart upload.
         */
        COPY_OBJECT(3);

        MetaRequestType(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }

        public static MetaRequestType getEnumValueFromInteger(int value) {
            MetaRequestType enumValue = enumMapping.get(value);
            if (enumValue != null) {
                return enumValue;
            }

            throw new RuntimeException("Invalid S3 Meta Request type");
        }

        private static Map<Integer, MetaRequestType> buildEnumMapping() {
            Map<Integer, MetaRequestType> enumMapping = new HashMap<Integer, MetaRequestType>();
            enumMapping.put(DEFAULT.getNativeValue(), DEFAULT);
            enumMapping.put(GET_OBJECT.getNativeValue(), GET_OBJECT);
            enumMapping.put(PUT_OBJECT.getNativeValue(), PUT_OBJECT);
            enumMapping.put(COPY_OBJECT.getNativeValue(), COPY_OBJECT);
            return enumMapping;
        }

        private int nativeValue;

        private static Map<Integer, MetaRequestType> enumMapping = buildEnumMapping();
    }

    private MetaRequestType metaRequestType;
    private ChecksumConfig checksumConfig;
    private HttpRequest httpRequest;
    private Path requestFilePath;
    private S3MetaRequestResponseHandler responseHandler;
    private CredentialsProvider credentialsProvider;
    private AwsSigningConfig signingConfig;
    private URI endpoint;
    private ResumeToken resumeToken;

    public S3MetaRequestOptions withMetaRequestType(MetaRequestType metaRequestType) {
        this.metaRequestType = metaRequestType;
        return this;
    }

    public MetaRequestType getMetaRequestType() {
        return metaRequestType;
    }

    /**
     * The config related to checksum used for the meta request. See {@link ChecksumConfig} for details.
     * @param checksumConfig The checksum config used for the meta request
     * @return this
     */
    public S3MetaRequestOptions withChecksumConfig(ChecksumConfig checksumConfig) {
        this.checksumConfig = checksumConfig;
        return this;
    }

    public ChecksumConfig getChecksumConfig() {
        return this.checksumConfig;
    }

    /**
     * @deprecated Please use {@link #withChecksumConfig(ChecksumConfig)} instead.
     * Specify the checksum algorithm to use use for put requests, if unset defaults to NONE and no checksum will be calculated.
     * The location of the checksum will be default to trailer.
     *
     * @param checksumAlgorithm the checksum algorithm to use use for put requests
     * @return this
     */
    public S3MetaRequestOptions withChecksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
        ChecksumConfig config = new ChecksumConfig().withChecksumAlgorithm(checksumAlgorithm).withChecksumLocation(ChecksumConfig.ChecksumLocation.TRAILER);
        this.checksumConfig = config;
        return this;
    }

    /**
     * @deprecated
     * @return the checksum algorithm to use use for put requests
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return this.checksumConfig.getChecksumAlgorithm();
    }

    /**
     * @deprecated  Please use {@link #withChecksumConfig(ChecksumConfig)} instead.
     * validateChecksum defaults to false, if set to true, it will cause the client to compare a streamed
     * calculation of the objects checksum to a remotely stored checksum assigned to the object if one exists.
     * The checksumValidated field passed in parameter of the finished callback will inform
     * the user if validation ocurred. A mismatch will result in a AWS_ERROR_S3_RESPONSE_CHECKSUM_MISMATCH error
     *
     * @param validateChecksum Validate the checksum of response if server provides.
     * @return this
     */
    public S3MetaRequestOptions withValidateChecksum(boolean validateChecksum) {
        ChecksumConfig config = new ChecksumConfig().withValidateChecksum(validateChecksum);
        this.checksumConfig = config;
        return this;
    }

    /**
     * @deprecated
     * @return Validate the checksum of response if server provides.
     */
    public boolean getValidateChecksum() {
        return checksumConfig.getValidateChecksum();
    }

    /**
     * Set the initial HTTP request.
     *
     * Note: When uploading a file, you can get better performance by setting
     * {@link withRequestFilePath} instead of setting a body stream on the HttpRequest.
     * (If both are set, the file path is used and body stream is ignored)
     *
     * @param httpRequest initial HTTP request message.
     * @return this
     */
    public S3MetaRequestOptions withHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
        return this;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * If set, this file is sent as the request's body, and the {@link withHttpRequest} body stream is ignored.
     *
     * This can give better upload performance than sending data using the body stream.
     *
     * @param requestFilePath path to file to send as the request's body.
     * @return this
     */
    public S3MetaRequestOptions withRequestFilePath(Path requestFilePath) {
        this.requestFilePath = requestFilePath;
        return this;
    }

    public Path getRequestFilePath() {
        return requestFilePath;
    }

    public S3MetaRequestOptions withResponseHandler(S3MetaRequestResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    public S3MetaRequestResponseHandler getResponseHandler() {
        return responseHandler;
    }

    /**
     * @deprecated Please use {@link #withSigningConfig(AwsSigningConfig)} instead.
     * The credentials provider will be used to create the signing Config to override the client level config.
     * The client config will be used.
     *
     * @param credentialsProvider provide credentials for signing.
     * @return this
     */
    public S3MetaRequestOptions withCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    public CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    /**
     * The configuration related to signing used by S3 client. It will override the client level configuration if provided.
     * `AwsSigningConfig.getDefaultS3SigningConfig(region, credentialsProvider);` can be used as helper to create the default configuration to be used for S3.
     *
     * @param signingConfig configuration related to signing via an AWS signing process.
     * @return this
     */
    public S3MetaRequestOptions withSigningConfig(AwsSigningConfig signingConfig) {
        this.signingConfig = signingConfig;
        return this;
    }

    public AwsSigningConfig getSigningConfig() {
        return signingConfig;
    }

    public S3MetaRequestOptions withEndpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public S3MetaRequestOptions withResumeToken(ResumeToken resumeToken) {
        this.resumeToken = resumeToken;
        return this;
    }

    public ResumeToken getResumeToken() {
        return resumeToken;
    }
}
