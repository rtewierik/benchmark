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
package io.openmessaging.tpch.model;


import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import java.math.BigDecimal;
import java.util.Date;

public class TpcHRow {
    @CsvBindByPosition(position = 0)
    public Integer orderKey;

    @CsvBindByPosition(position = 1)
    public Integer partKey;

    @CsvBindByPosition(position = 2)
    public Integer suppKey;

    @CsvBindByPosition(position = 3)
    public Integer lineNumber;

    @CsvBindByPosition(position = 4)
    public BigDecimal quantity;

    @CsvBindByPosition(position = 5)
    public BigDecimal extendedPrice;

    @CsvBindByPosition(position = 6)
    public BigDecimal discount;

    @CsvBindByPosition(position = 7)
    public BigDecimal tax;

    @CsvBindByPosition(position = 8)
    public Character returnFlag;

    @CsvBindByPosition(position = 9)
    public Character lineStatus;

    @CsvDate("yyyy-MM-dd")
    @CsvBindByPosition(position = 10)
    public Date shipDate;

    @CsvDate("yyyy-MM-dd")
    @CsvBindByPosition(position = 11)
    public Date commitDate;

    @CsvDate("yyyy-MM-dd")
    @CsvBindByPosition(position = 12)
    public Date receiptDate;

    @CsvBindByPosition(position = 13)
    public String shipInstruct;

    @CsvBindByPosition(position = 14)
    public String shipMode;

    @CsvBindByPosition(position = 15)
    public String comment;
}
