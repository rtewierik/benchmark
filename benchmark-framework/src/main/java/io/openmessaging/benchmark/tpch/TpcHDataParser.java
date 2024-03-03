package io.openmessaging.benchmark.tpch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TpcHDataParser {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static List<TpcHRow> readTpcHRowsFromStream(InputStream stream) throws IOException  {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        reader.readLine();
        List<TpcHRow> csvRows = new ArrayList<>();
        String dataLine;
        while ((dataLine = reader.readLine()) != null) {
            String[] values = dataLine.split("\\|");
            TpcHRow row = parseCsvRow(values);
            csvRows.add(row);
        }
        return csvRows;
    }

    private static TpcHRow parseCsvRow(String[] values) throws RuntimeException {
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
}
