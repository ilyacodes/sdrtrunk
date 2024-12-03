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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Services Header - Opcode 145
 */
public class MotorolaGroupServicesHeader extends MacStructureVendor
{
    private static final IntField TALKGROUP = IntField.length16(OCTET_4_BIT_24);
    private static final IntField BLOCK_COUNT = IntField.length8(OCTET_6_BIT_40);
    private static final IntField FORMAT = IntField.length8(OCTET_7_BIT_48);
    private static final IntField UNKNOWN = IntField.length8(OCTET_8_BIT_56); //Always 0x00
    private static final IntField SEQUENCE = IntField.length4(OCTET_9_BIT_64);
    private static final int FRAGMENT_START = OCTET_10_BIT_72;
    private static final int FRAGMENT_END = OCTET_18_BIT_136;
    private List<Identifier> mIdentifiers;
    private APCO25Talkgroup mTalkgroup;
    private APCO25FullyQualifiedRadioIdentifier mRadio;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupServicesHeader(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP SERVICES HEADER TG:").append(getTalkgroup());
        sb.append(" SEQUENCE:").append(getSequence());
        sb.append(" BLOCKS TO FOLLOW:").append(getBlockCount());
        sb.append(" FORMAT:").append(getFormat());
        sb.append(" FRAGMENT:").append(getFragment().toHexString());
        sb.append(" UNK:").append(Integer.toHexString(getInt(UNKNOWN)).toUpperCase());
        sb.append(" MSG:").append(getMessage().get(getOffset(), getMessage().length()).toHexString());
        return sb.toString();
    }

    /**
     * Fragment that is the start of the encoded alias.
     */
    public BinaryMessage getFragment()
    {
        return getMessage().getSubMessage(getOffset() + FRAGMENT_START, getOffset() + FRAGMENT_END);
    }

    /**
     * Alias encoding format
     */
    public String getFormat()
    {
        int format = getInt(FORMAT);

        switch (format) {
            case 0: return "0-GROUP TEXT";
            case 1: return "1-TALKER ALIAS";
            default: return String.valueOf(format) + "-UNKNOWN";
        }
    }

    /**
     * Sequence number that ties the header to each of the data blocks.
     */
    public int getSequence()
    {
        return getInt(SEQUENCE);
    }

    /**
     * Number of data blocks that follow this header.
     */
    public int getBlockCount()
    {
        return getInt(BLOCK_COUNT);
    }

    /**
     * Talkgroup identifier
     */
    public APCO25Talkgroup getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = APCO25Talkgroup.create(getMessage().getInt(TALKGROUP, getOffset()));
        }

        return mTalkgroup;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
        }

        return mIdentifiers;
    }
}