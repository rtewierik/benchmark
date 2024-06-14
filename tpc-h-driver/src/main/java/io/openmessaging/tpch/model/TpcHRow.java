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
    public BigDecimal quantity;

    @CsvBindByPosition(position = 1)
    public BigDecimal extendedPrice;

    @CsvBindByPosition(position = 2)
    public BigDecimal discount;

    @CsvBindByPosition(position = 3)
    public BigDecimal tax;

    @CsvBindByPosition(position = 4)
    public Character returnFlag;

    @CsvBindByPosition(position = 5)
    public Character lineStatus;

    @CsvDate("yyyy-MM-dd")
    @CsvBindByPosition(position = 6)
    public Date shipDate;
}
