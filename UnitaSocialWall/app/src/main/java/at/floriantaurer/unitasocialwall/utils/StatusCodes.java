/*
 * Copyright (c) 2019. Florian Taurer.
 *
 * This file is part of Unita SDK.
 *
 * Unita is free a SDK: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unita is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Unita.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.floriantaurer.unitasocialwall.utils;

public enum StatusCodes {

    DEVICE_PAIRED_SUCCESS("201"),
    MESSAGE_PACKET_RECEIVED_SUCCESS("202"),
    DEVICE_PAIRED_FAILED("401"),
    MESSAGE_PACKET_RECEIVED_FAILED("402");

    private String stringValue;

    private StatusCodes(String toString){
        stringValue = toString;
    }

    public String toString(){
        return stringValue;
    }

    public static StatusCodes fromString(String text) throws IllegalArgumentException {
        for (StatusCodes s : StatusCodes.values()) {
            if (s.stringValue.equalsIgnoreCase(text)) {
                return s;
            }
        }
        throw new IllegalArgumentException("No StatusCodes with text " + text + " found");
    }
}
