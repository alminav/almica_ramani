/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
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

import java.util.HashSet;
import java.util.Set;

/**
 * Defines bit layout for pedestrians (speed, access, surface, ...).
 * <p>
 * @author Peter Karich
 * @author Nop
 * @author Karl Hübner
 */
public class FootFlagEncoder extends AbstractFlagEncoder
{
    static final int SLOW = 2;
    static final int MEAN = 5;
    static final int FERRY = 10;
    private int safeWayBit = 0;
    protected HashSet<String> sidewalks = new HashSet<String>();
    private final Set<String> safeHighwayTags = new HashSet<String>();
    private final Set<String> allowedHighwayTags = new HashSet<String>();

    /**
     * Should be only instantiated via EncodingManager
     */
    protected FootFlagEncoder()
    {
        this(4, 1);
    }

    protected FootFlagEncoder( int speedBits, double speedFactor )
    {
        super(speedBits, speedFactor);
        restrictions = new String[]
        {
            "foot", "access"
        };
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");

        sidewalks.add("yes");
        sidewalks.add("both");
        sidewalks.add("left");
        sidewalks.add("right");

        potentialBarriers.add("gate");
        //potentialBarriers.add( "lift_gate" );   you can always pass them on foot
        potentialBarriers.add("swing_gate");

        acceptedRailways.add("station");
        acceptedRailways.add("platform");

        safeHighwayTags.add("footway");
        safeHighwayTags.add("path");
        safeHighwayTags.add("steps");
        safeHighwayTags.add("pedestrian");
        safeHighwayTags.add("living_street");
        safeHighwayTags.add("track");
        safeHighwayTags.add("residential");
        safeHighwayTags.add("service");

        allowedHighwayTags.addAll(safeHighwayTags);
        allowedHighwayTags.add("trunk");
        allowedHighwayTags.add("trunk_link");
        allowedHighwayTags.add("primary");
        allowedHighwayTags.add("primary_link");
        allowedHighwayTags.add("secondary");
        allowedHighwayTags.add("secondary_link");
        allowedHighwayTags.add("tertiary");
        allowedHighwayTags.add("tertiary_link");
        allowedHighwayTags.add("unclassified");
        allowedHighwayTags.add("road");
        // disallowed in some countries
        //allowedHighwayTags.add("bridleway");
    }

    @Override
    public int defineWayBits( int index, int shift )
    {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        // larger value required - ferries are faster than pedestrians
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, MEAN, FERRY);
        shift += speedBits;

        safeWayBit = 1 << shift++;
        return shift;
    }

    /**
     * Foot flag encoder does not provide any turn cost / restrictions
     */
    @Override
    public int defineTurnBits( int index, int shift, int numberCostsBits )
    {
        return shift;
    }

    /**
     * Foot flag encoder does not provide any turn cost / restrictions
     * <p>
     * @return <code>false</code>
     */
    @Override
    public boolean isTurnRestricted( long flag )
    {
        return false;
    }

    /**
     * Foot flag encoder does not provide any turn cost / restrictions
     * <p>
     * @return 0
     */
    @Override
    public int getTurnCosts( long flag )
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "foot";
    }

}
