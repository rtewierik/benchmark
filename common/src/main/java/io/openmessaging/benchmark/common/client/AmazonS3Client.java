/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openmessaging.benchmark.common.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import io.openmessaging.benchmark.common.utils.RandomGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Slf4j
public class AmazonS3Client {

    private final AmazonS3 s3Client;

    public AmazonS3Client() {
        this.s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion("eu-west-1")
                .build();
    }

    public InputStream readFileFromS3(String bucketName, String key) throws IOException {
        try {
            S3Object s3Object = this.s3Client.getObject(new GetObjectRequest(bucketName, key));
            return s3Object.getObjectContent();
        } catch (Exception exception) {
            throw new IOException("Failed to read and CSV from S3: " + exception.getMessage(), exception);
        }
    }

    public InputStream readFileFromS3(String s3Uri) throws IOException {
        URI uri = URI.create(s3Uri);
        String bucketName = uri.getHost();
        String key = uri.getPath().substring(1);
        return readFileFromS3(bucketName, key);
    }

    public void writeMessageToS3(String bucketName, String key, byte[] message) {
        String fileName = String.format("%s-%s", RandomGenerator.getRandomString(), RandomGenerator.getRandomString());
        String s3Uri = String.format("%s/%s", key, fileName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(message.length);
        PutObjectRequest request = new PutObjectRequest(bucketName, s3Uri, new ByteArrayInputStream(message), metadata);
        s3Client.putObject(request);
    }

    public void writeMessageToS3(String bucketName, String key, String message) {
        this.writeMessageToS3(bucketName, key, message.getBytes());
    }

    public void deleteFileFromS3(String bucketName, String key) {
        s3Client.deleteObject(new DeleteObjectRequest(bucketName, key));
    }
}
