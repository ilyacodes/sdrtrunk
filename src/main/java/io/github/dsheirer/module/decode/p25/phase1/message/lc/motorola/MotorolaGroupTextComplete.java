/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer, 2024 Ilya Smirnov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.TimeslotMessage;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola completely assembled group text.  Note: this is not a true link control word.  It is reassembled from a
 * header and data blocks.
 */
public class MotorolaGroupTextComplete extends TimeslotMessage implements IMessage
{
    private static final IntField MESSAGE_LENGTH = IntField.length16(OCTET_0_BIT_0);
    private static final IntField STYLE_COLOR = IntField.length8(OCTET_4_BIT_32);
    private static final IntField TG = IntField.length16(OCTET_5_BIT_40);
    private static final IntField SOURCE_WACN = IntField.length20(OCTET_7_BIT_56);
    private static final IntField SOURCE_SYSTEM = IntField.length12(OCTET_9_BIT_72 + 4);
    private static final IntField SOURCE_ID = IntField.length24(OCTET_11_BIT_88);
    private static final IntField CHUNK = IntField.length16(0);
    private static final int ENCODED_ALIAS_START = OCTET_14_BIT_112;
    private static final int CHUNK_SIZE = 16;

    private APCO25Talkgroup mTalkgroup;
    private APCO25FullyQualifiedRadioIdentifier mSourceRadio;
    private String mTextMessage;
    private List<Identifier> mIdentifiers;
    private int mSequence;
    private Protocol mProtocol;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message assembled from the data blocks
     * @param talkgroup from the header
     * @param dataBlockCount from the header
     * @param sequence number for the alias
     * @param timeslot for the message
     * @param timestamp of the most recent header or data block
     * @param protocol for the message
     */
    public MotorolaGroupTextComplete(CorrectedBinaryMessage message, APCO25Talkgroup talkgroup, int sequence,
                                       int timeslot, long timestamp, Protocol protocol)
    {
        super(message, timeslot, timestamp);
        mTalkgroup = talkgroup;
        mSequence = sequence;
        mProtocol = protocol;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(getTimeslot() > TIMESLOT_0)
        {
            sb.append("TS").append(getTimeslot()).append(" ");
        }
        sb.append("MOTOROLA GROUP TEXT COMPLETE");
        sb.append(" SOURCE_RADIO:").append(getSourceRadio());
        sb.append(" TG:").append(getTalkgroup());
        sb.append(" ENCODED:").append(getEncodedMessage().toHexString());
        sb.append(" STYLE:").append(getStyle());
        sb.append(" COLOR:").append(getColor());
        sb.append(" MESSAGE:").append(getTextMessage());
        sb.append(" SEQUENCE:").append(mSequence);
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Protocol - P25 Phase 1 or Phase 2
     */
    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    /**
     * Sequence number for the alias.
     */
    public int getSequence()
    {
        return mSequence;
    }

    /**
     * Decoded notification style
     */
    public String getStyle()
    {
        int style = getInt(STYLE_COLOR) >> 6;

        switch (style) {
            case 0: return "0-NO_POPUP";
            case 2: return "2-POPUP";
            case 3: return "3-POPUP_WITH_TONE";
            default: return String.valueOf(style) + "-UNKNOWN";
        }
    }

    /**
     * Decoded notification color
     */
    public String getColor()
    {
        int color = (getInt(STYLE_COLOR) >> 3) & 0x7;

        switch (color) {
            case 0: return "0-NO_COLOR";
            case 1: return "1-GREEN";
            case 2: return "2-ORANGE";
            case 3: return "3-RED";
            default: return String.valueOf(color) + "-UNKNOWN";
        }
    }

    /**
     * Decoded text message
     */
    public String getTextMessage()
    {
        if(mTextMessage == null)
        {
            byte[] encoded = getEncodedMessage().toByteArray();

            // TODO: check CRC in last two bytes to validate alias has no errors
            //
            //   Data:  llll 0000 nn tttt wwwww sss rrrrrr mmmm...mmmm cccc
            //
            //   - l = length of message
            //   - n = notification style
            //   - t = talkgroup
            //   - w = WACN
            //   - s = system
            //   - r = radio id
            //   - m = encoded message
            //   - c = CRC-16/GSM of the previous bytes

            // Get number of bytes and characters excluding checksum
            char bytes = (char)(encoded.length - 2);
            char chars = (char)(bytes / 2);

            // Copy bytes (as chars) to our message string
            String message = "";
            for (char i = 0; i < chars; i++) {
                message += (char)((encoded[i * 2] << 8) | encoded[i * 2 + 1]);
            }

            mTextMessage = message;
        }

        return mTextMessage;
    }

    /**
     * Extracts the encoded message payload.
     * @return encoded message binary message
     */
    private BinaryMessage getEncodedMessage()
    {
        int length = getInt(MESSAGE_LENGTH);

        return getMessage().getSubMessage(ENCODED_ALIAS_START, ENCODED_ALIAS_START + ((length + 2) * 8));
    }

    /**
     * Talkgroup
     */
    public APCO25Talkgroup getTalkgroup()
    {
        return mTalkgroup;
    }

    /**
     * Radio (console) that is originating the text message
     */
    public APCO25FullyQualifiedRadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            int wacn = getInt(SOURCE_WACN);
            int system = getInt(SOURCE_SYSTEM);
            int id = getInt(SOURCE_ID);

            mSourceRadio = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mSourceRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
