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

import static com.graphhopper.routing.util.BikeCommonFlagEncoder.PriorityCode.*;

import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.Translation;

import java.util.*;

/**
 * Defines bit layout of bicycles (not motorbikes) for speed, access and relations (network).
 * <p/>
 * @author Peter Karich
 * @author Nop
 * @author ratrun
 */
public class BikeCommonFlagEncoder extends AbstractFlagEncoder
{
    protected static final int PUSHING_SECTION_SPEED = 4;
    private int unpavedBit = 0;
    // Pushing section heighways are parts where you need to get off your bike and push it (German: Schiebestrecke)
    protected final HashSet<String> pushingSections = new HashSet<String>();
    protected final HashSet<String> oppositeLanes = new HashSet<String>();
    protected final Set<String> preferHighwayTags = new HashSet<String>();
    protected final Set<String> avoidHighwayTags = new HashSet<String>();
    protected final Set<String> unpavedSurfaceTags = new HashSet<String>();
    private final Map<String, Integer> trackTypeSpeed = new HashMap<String, Integer>();
    private final Map<String, Integer> surfaceSpeed = new HashMap<String, Integer>();
    private final Set<String> roadValues = new HashSet<String>();
    private final Map<String, Integer> highwaySpeed = new HashMap<String, Integer>();
    //Convert network tag of bicycle routes into a way route code stored in the wayMAP
    private final Map<String, Integer> bikeNetworkToCode = new HashMap<String, Integer>();
    EncodedValue relationCodeEncoder;
    private EncodedValue wayTypeEncoder;
    private EncodedValue preferWayEncoder;

    /**
     * Should be only instantied via EncodingManager
     */
    protected BikeCommonFlagEncoder()
    {
        this(4, 2);
    }

    protected BikeCommonFlagEncoder( int speedBits, double speedFactor )
    {
        super(speedBits, speedFactor);
        // strict set, usually vehicle and agricultural/forestry are ignored by cyclists
        restrictions = new String[]
        {
            "bicycle", "access"
        };
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");

        oppositeLanes.add("opposite");
        oppositeLanes.add("opposite_lane");
        oppositeLanes.add("opposite_track");

        // With a bike one usually can pass all those barriers:
        // potentialBarriers.add("gate");
        // potentialBarriers.add("lift_gate");
        // potentialBarriers.add("swing_gate");
        // potentialBarriers.add("cycle_barrier");
        // potentialBarriers.add("block");
        absoluteBarriers.add("kissing_gate");
        absoluteBarriers.add("stile");
        absoluteBarriers.add("turnstile");

        unpavedSurfaceTags.add("unpaved");
        unpavedSurfaceTags.add("gravel");
        unpavedSurfaceTags.add("ground");
        unpavedSurfaceTags.add("dirt");
        unpavedSurfaceTags.add("grass");
        unpavedSurfaceTags.add("compacted");
        unpavedSurfaceTags.add("earth");
        unpavedSurfaceTags.add("fine_gravel");
        unpavedSurfaceTags.add("grass_paver");
        unpavedSurfaceTags.add("ice");
        unpavedSurfaceTags.add("mud");
        unpavedSurfaceTags.add("salt");
        unpavedSurfaceTags.add("sand");
        unpavedSurfaceTags.add("wood");

        roadValues.add("living_street");
        roadValues.add("road");
        roadValues.add("service");
        roadValues.add("unclassified");
        roadValues.add("residential");
        roadValues.add("trunk");
        roadValues.add("trunk_link");
        roadValues.add("primary");
        roadValues.add("primary_link");
        roadValues.add("secondary");
        roadValues.add("secondary_link");
        roadValues.add("tertiary");
        roadValues.add("tertiary_link");

        setTrackTypeSpeed("grade1", 18); // paved
        setTrackTypeSpeed("grade2", 12); // now unpaved ...
        setTrackTypeSpeed("grade3", 8);
        setTrackTypeSpeed("grade4", 6);
        setTrackTypeSpeed("grade5", 4); // like sand/grass     

        setSurfaceSpeed("paved", 18);
        setSurfaceSpeed("asphalt", 18);
        setSurfaceSpeed("cobblestone", 8);
        setSurfaceSpeed("cobblestone:flattened", 10);
        setSurfaceSpeed("sett", 10);
        setSurfaceSpeed("concrete", 18);
        setSurfaceSpeed("concrete:lanes", 16);
        setSurfaceSpeed("concrete:plates", 16);
        setSurfaceSpeed("paving_stones", 12);
        setSurfaceSpeed("paving_stones:30", 12);
        setSurfaceSpeed("unpaved", 14);
        setSurfaceSpeed("compacted", 16);
        setSurfaceSpeed("dirt", 10);
        setSurfaceSpeed("earth", 12);
        setSurfaceSpeed("fine_gravel", 18);
        setSurfaceSpeed("grass", 8);
        setSurfaceSpeed("grass_paver", 8);
        setSurfaceSpeed("gravel", 12);
        setSurfaceSpeed("ground", 12);
        setSurfaceSpeed("ice", PUSHING_SECTION_SPEED / 2);
        setSurfaceSpeed("metal", 10);
        setSurfaceSpeed("mud", 10);
        setSurfaceSpeed("pebblestone", 16);
        setSurfaceSpeed("salt", 6);
        setSurfaceSpeed("sand", 6);
        setSurfaceSpeed("wood", 6);

        setHighwaySpeed("living_street", 6);
        setHighwaySpeed("steps", PUSHING_SECTION_SPEED / 2);

        setHighwaySpeed("cycleway", 18);
        setHighwaySpeed("path", 18);
        setHighwaySpeed("footway", 6);
        setHighwaySpeed("pedestrian", 6);
        setHighwaySpeed("track", 12);
        setHighwaySpeed("service", 14);
        setHighwaySpeed("residential", 18);
        // no other highway applies:
        setHighwaySpeed("unclassified", 16);
        // unknown road:
        setHighwaySpeed("road", 12);

        setHighwaySpeed("trunk", 18);
        setHighwaySpeed("trunk_link", 18);
        setHighwaySpeed("primary", 18);
        setHighwaySpeed("primary_link", 18);
        setHighwaySpeed("secondary", 18);
        setHighwaySpeed("secondary_link", 18);
        setHighwaySpeed("tertiary", 18);
        setHighwaySpeed("tertiary_link", 18);

        // special case see tests and #191
        setHighwaySpeed("motorway", 18);
        setHighwaySpeed("motorway_link", 18);
        avoidHighwayTags.add("motorway");
        avoidHighwayTags.add("motorway_link");

        setCyclingNetworkPreference("icn", PriorityCode.BEST.getValue());
        setCyclingNetworkPreference("ncn", PriorityCode.BEST.getValue());
        setCyclingNetworkPreference("rcn", PriorityCode.VERY_NICE.getValue());
        setCyclingNetworkPreference("lcn", PriorityCode.PREFER.getValue());
        setCyclingNetworkPreference("mtb", PriorityCode.UNCHANGED.getValue());

        setCyclingNetworkPreference("deprecated", PriorityCode.AVOID_AT_ALL_COSTS.getValue());
    }

    @Override
    public int defineWayBits( int index, int shift )
    {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, highwaySpeed.get("cycleway"), 30);
        shift += speedEncoder.getBits();

        unpavedBit = 1 << shift++;
        // 2 bits
        wayTypeEncoder = new EncodedValue("WayType", shift, 2, 1, 0, 3, true);
        shift += wayTypeEncoder.getBits();

        preferWayEncoder = new EncodedValue("PreferWay", shift, 3, 1, 0, 7);
        shift += preferWayEncoder.getBits();

        return shift;
    }

    @Override
    public int defineRelationBits( int index, int shift )
    {
        relationCodeEncoder = new EncodedValue("RelationCode", shift, 3, 1, 0, 7);
        return shift + relationCodeEncoder.getBits();
    }


    @Override
    public InstructionAnnotation getAnnotation( long flags, Translation tr )
    {
        int paveType = 0; // paved
        if ((flags & unpavedBit) != 0)
            paveType = 1; // unpaved        

        int wayType = (int) wayTypeEncoder.getValue(flags);
        String wayName = getWayName(paveType, wayType, tr);
        return new InstructionAnnotation(0, wayName);
    }

    String getWayName( int pavementType, int wayType, Translation tr )
    {
        String pavementName = "";
        if (pavementType == 1)
            pavementName = tr.tr("unpaved");

        String wayTypeName = "";
        switch (wayType)
        {
            case 0:
                wayTypeName = tr.tr("road");
                break;
            case 1:
                wayTypeName = tr.tr("off_bike");
                break;
            case 2:
                wayTypeName = tr.tr("cycleway");
                break;
            case 3:
                wayTypeName = tr.tr("way");
                break;
        }

        if (pavementName.isEmpty())
        {
            if (wayType == 0 || wayType == 3)
                return "";
            return wayTypeName;
        } else
        {
            if (wayTypeName.isEmpty())
                return pavementName;
            else
                return wayTypeName + ", " + pavementName;
        }
    }

    /**
     * Returns a double value in [0, 1] to identify ways as good or bad regarding safety or other
     * preferences.
     */
    public double getPriority( long flags )
    {
        double prio = preferWayEncoder.getValue(flags);
        if (prio == 0)
            return (double) UNCHANGED.getValue() / BEST.getValue();

        return prio / BEST.getValue();
    }

    enum PriorityCode
    {
        /* Inspired by http://wiki.openstreetmap.org/wiki/Class:bicycle
         "-3" = Avoid at all cost. 
         "-2" = Only use to reach your destination, not well suited. 
         "-1" = Better take another way 
         "0" = as well as other ways around. 
         Try to to avoid using 0 but decide on -1 or +1. 
         class:bicycle shall only be used as an additional key. 
         "1" = Prefer 
         "2" = Very Nice way to cycle 
         "3" = This way is so nice, it pays out to make a detour also if this means taking 
         many unsuitable ways to get here. Outstanding for its intendedValues usage class.
         */
        //We can't store negative numbers into our map, therefore we add 
        //unspecifiedRelationWeight=4 to the schema from above
        WORST(0),
        AVOID_AT_ALL_COSTS(1),
        REACH_DEST(2),
        AVOID_IF_POSSIBLE(3),
        UNCHANGED(4),
        PREFER(5),
        VERY_NICE(6),
        BEST(7);

        private final int value;

        private PriorityCode( int value )
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    };

    void setHighwaySpeed( String highway, int speed )
    {
        highwaySpeed.put(highway, speed);
    }

    int getHighwaySpeed( String key )
    {
        return highwaySpeed.get(key);
    }

    void setTrackTypeSpeed( String tracktype, int speed )
    {
        trackTypeSpeed.put(tracktype, speed);
    }

    void setSurfaceSpeed( String surface, int speed )
    {
        surfaceSpeed.put(surface, speed);
    }

    void setCyclingNetworkPreference( String network, int code )
    {
        bikeNetworkToCode.put(network, code);
    }

    void addPushingSection( String highway )
    {
        pushingSections.add(highway);
    }
}
