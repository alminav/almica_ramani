/*
 *  Licensed to Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  Peter Karich licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import com.graphhopper.util.BitUtil;
import com.graphhopper.util.DistanceCalc3D;

/**
 * Stores two speed values into an edge to support avoiding too much incline
 * <p>
 * @author Peter Karich
 */
public class Bike2WeightFlagEncoder extends BikeFlagEncoder
{
    private final DistanceCalc3D distCalc = new DistanceCalc3D();
    private EncodedDoubleValue reverseSpeed;

    @Override
    public int defineWayBits( int index, int shift )
    {
        shift = super.defineWayBits(index, shift);
        reverseSpeed = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, getHighwaySpeed("cycleway"), 30);
        shift += reverseSpeed.getBits();
        return shift;
    }

    @Override
    public double getReverseSpeed( long flags )
    {
        return reverseSpeed.getDoubleValue(flags);
    }

    @Override
    public long setReverseSpeed( long flags, double speed )
    {
        if (speed < 0)
            throw new IllegalArgumentException("Speed cannot be negative: " + speed + ", flags:" + BitUtil.LITTLE.toBitString(flags));

        if (speed > getMaxSpeed())
            speed = getMaxSpeed();

        return reverseSpeed.setDoubleValue(flags, speed);
    }


    @Override
    public long flagsDefault( boolean forward, boolean backward )
    {
        long flags = super.flagsDefault(forward, backward);
        if (backward)
            return reverseSpeed.setDefaultValue(flags);

        return flags;
    }

    @Override
    public long setProperties( double speed, boolean forward, boolean backward )
    {
        long flags = super.setProperties(speed, forward, backward);
        if (backward)
            return setReverseSpeed(flags, speed);

        return flags;
    }

    @Override
    public long reverseFlags( long flags )
    {
        // swap access
        flags = super.reverseFlags(flags);

        // swap speeds 
        double otherValue = reverseSpeed.getDoubleValue(flags);
        flags = setReverseSpeed(flags, speedEncoder.getDoubleValue(flags));
        return setSpeed(flags, otherValue);
    }

    @Override
    public String toString()
    {
        return "bike2";
    }
}
