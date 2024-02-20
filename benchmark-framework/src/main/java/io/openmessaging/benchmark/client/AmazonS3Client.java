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
package io.openmessaging.benchmark.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import io.openmessaging.benchmark.tpch.TpcHRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AmazonS3Client {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final AmazonS3 s3Client;

    public AmazonS3Client() {
        this.s3Client = AmazonS3ClientBuilder.standard().build();
    }

    public List<List<TpcHRow>> readAndParseChunksFromS3Folder(String s3Uri)
            throws IOException {
        try {
            List<List<TpcHRow>> csvChunks = new ArrayList<>();
            String bucketName = extractBucketName(s3Uri);
            String folderPath = extractFolderPath(s3Uri);

            ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(folderPath)
                    .withDelimiter("/");

            ListObjectsV2Result listObjectsResponse;
            do {
                listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);
                for (S3ObjectSummary objectSummary : listObjectsResponse.getObjectSummaries()) {
                    String objectKey = objectSummary.getKey();
                    List<TpcHRow> csvRows = readAndParseCsvFromS3(bucketName, objectKey);
                    csvChunks.add(csvRows);
                }

                listObjectsRequest.setContinuationToken(listObjectsResponse.getNextContinuationToken());
            } while (listObjectsResponse.isTruncated());

            return csvChunks;
        } catch (Exception exception) {
            throw new IOException("Failed to read CSV from S3 folder: " + exception.getMessage(), exception);
        }
    }

    private List<TpcHRow> readAndParseCsvFromS3(String bucketName, String objectKey)
            throws IOException {
        try {
            S3Object s3Object = this.s3Client.getObject(new GetObjectRequest(bucketName, objectKey));
            S3ObjectInputStream objectInputStream = s3Object.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(objectInputStream));
            reader.readLine();
            List<TpcHRow> csvRows = new ArrayList<>();
            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                String[] values = dataLine.split("\\|");
                TpcHRow row = parseCsvRow(values);
                csvRows.add(row);
            }
            return csvRows;
        } catch (Exception exception) {
            throw new IOException("Failed to read and CSV from S3: " + exception.getMessage(), exception);
        }
    }

    private static TpcHRow parseCsvRow(String[] values) {
        try {
            TpcHRow row = new TpcHRow();
            row.orderKey = Integer.parseInt(values[0]);
            row.partKey = Integer.parseInt(values[1]);
            row.suppKey = Integer.parseInt(values[2]);
            row.lineNumber = Integer.parseInt(values[3]);
            row.quantity = Float.parseFloat(values[4]);
            row.extendedPrice = Float.parseFloat(values[5]);
            row.discount = Float.parseFloat(values[6]);
            row.tax = Float.parseFloat(values[7]);
            row.returnFlag = values[8].charAt(0);
            row.lineStatus = values[9].charAt(0);
            row.shipDate = SIMPLE_DATE_FORMAT.parse(values[10]);
            row.commitDate = SIMPLE_DATE_FORMAT.parse(values[11]);
            row.receiptDate = SIMPLE_DATE_FORMAT.parse(values[12]);
            row.shipInstruct = values[13];
            row.shipMode = values[14];
            row.comment = values[15];
            return row;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to parse row: " + exception.getMessage(), exception);
        }
    }

    private static String extractBucketName(String s3Uri) {
        return s3Uri.split("/")[2];
    }

    private static String extractFolderPath(String s3Uri) {
        String[] parts = s3Uri.split("/", 4);
        return parts.length == 4 ? parts[3] : "";
    }
}
