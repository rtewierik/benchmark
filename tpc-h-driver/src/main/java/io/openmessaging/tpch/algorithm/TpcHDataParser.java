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
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static List<TpcHRow> readTpcHRowsFromStream(InputStream stream) throws IOException  {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<TpcHRow> csvRows = new ArrayList<>();
        String dataLine;
        while ((dataLine = reader.readLine()) != null) {
            String[] values = dataLine.split("\\|");
            TpcHRow row = parseCsvRow(values);
            csvRows.add(row);
        }
        stream.close();
        return csvRows;
    }

    private static TpcHRow parseCsvRow(String[] values) throws RuntimeException {
        try {
            TpcHRow row = new TpcHRow();
            row.orderKey = Integer.parseInt(values[0]);
            row.partKey = Integer.parseInt(values[1]);
            row.suppKey = Integer.parseInt(values[2]);
            row.lineNumber = Integer.parseInt(values[3]);
            row.quantity = new BigDecimal(values[4]);
            row.extendedPrice = new BigDecimal(values[5]);
            row.discount = new BigDecimal(values[6]);
            row.tax = new BigDecimal(values[7]);
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
}
