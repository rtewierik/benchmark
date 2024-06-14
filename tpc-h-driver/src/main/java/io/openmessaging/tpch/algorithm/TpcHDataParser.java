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
package io.openmessaging.tpch.algorithm;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.openmessaging.tpch.model.TpcHRow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TpcHDataParser {
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public static List<TpcHRow> readTpcHRowsFromStream(InputStream stream) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(stream, UTF_8)) {
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                List<TpcHRow> csvRows = new ArrayList<>();
                String dataLine;
                while ((dataLine = reader.readLine()) != null) {
                    String[] values = dataLine.split("\\|");
                    TpcHRow row = parseCsvRow(values);
                    csvRows.add(row);
                }
                reader.close();
                inputStreamReader.close();
                stream.close();
                return csvRows;
            }
        }
    }

    public static TpcHRow readTpcHRowFromLine(String line) throws IOException {
        String[] values = line.split("\\|");
        return parseCsvRow(values);
    }

    private static TpcHRow parseCsvRow(String[] values) throws RuntimeException {
        try {
            TpcHRow row = new TpcHRow();
            row.quantity = new BigDecimal(values[0]);
            row.extendedPrice = new BigDecimal(values[1]);
            row.discount = new BigDecimal(values[2]);
            row.tax = new BigDecimal(values[3]);
            row.returnFlag = values[4].charAt(0);
            row.lineStatus = values[5].charAt(0);
            row.shipDate = SIMPLE_DATE_FORMAT.get().parse(values[6]);
            return row;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to parse row: " + exception.getMessage(), exception);
        }
    }
}
