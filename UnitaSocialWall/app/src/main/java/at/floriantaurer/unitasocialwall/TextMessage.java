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

package at.floriantaurer.unitasocialwall;

import java.nio.charset.StandardCharsets;

import at.floriantaurer.unitabeaconmodule.Peer;

public class TextMessage extends at.floriantaurer.unitabeaconmodule.TextMessage {

    private boolean isPublic;

    public TextMessage(Peer sender, Peer receiver, Peer communicationPartner, String messageBody, boolean isPublic){
        super(sender, receiver, communicationPartner, messageBody.getBytes(StandardCharsets.UTF_8));
        this.isPublic = isPublic;
    }

    public boolean isPublic() {
        return isPublic;
    }

}
