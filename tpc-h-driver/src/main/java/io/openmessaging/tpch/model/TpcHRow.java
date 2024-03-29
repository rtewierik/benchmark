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


import java.math.BigDecimal;
import java.util.Date;

public class TpcHRow {
    public Integer orderKey;
    public Integer partKey;
    public Integer suppKey;
    public Integer lineNumber;
    public BigDecimal quantity;
    public BigDecimal extendedPrice;
    public BigDecimal discount;
    public BigDecimal tax;
    public Character returnFlag;
    public Character lineStatus;
    public Date shipDate;
    public Date commitDate;
    public Date receiptDate;
    public String shipInstruct;
    public String shipMode;
    public String comment;

    @Override
    public String toString() {
        return "TpcHRow{"
                + "orderKey="
                + orderKey
                + ", partKey="
                + partKey
                + ", suppKey="
                + suppKey
                + ", lineNumber="
                + lineNumber
                + ", quantity="
                + quantity
                + ", extendedPrice="
                + extendedPrice
                + ", discount="
                + discount
                + ", tax="
                + tax
                + ", returnFlag="
                + returnFlag
                + ", lineStatus="
                + lineStatus
                + ", shipDate="
                + shipDate
                + ", commitDate="
                + commitDate
                + ", receiptDate="
                + receiptDate
                + ", shipInstruct='"
                + shipInstruct
                + '\''
                + ", shipMode='"
                + shipMode
                + '\''
                + ", comment='"
                + comment
                + '\''
                + '}';
    }
}
