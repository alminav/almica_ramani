package com.almica.ramani.routes

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.utils.format
import com.graphhopper.util.Instruction
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

private const val logtag = "AlminavInstruction"
class AlminavInstruction {
    var text: String?
    var sign: Int = 0
    var nameBefore: String?
    var nameAfter: String?
    var distanceM: Int = 0
    val legDistanceM: Int
    val distRatio: Float
    val latLong: LatLngH?
    val icon: Drawable?

    constructor(
        context: Context,
        sign: Int?,
        nameBefore: String?,
        nameAfter: String?,
        legDistanceM: Int,
        distanceM: Int,
        latLong: LatLngH?,
        distRatio: Float
    ) {
        if (sign != null) {
            this.sign = sign
        }
        this.nameBefore = nameBefore
        this.nameAfter = nameAfter
        this.legDistanceM = legDistanceM
        this.latLong = latLong
        this.distanceM = distanceM
        this.icon = getInstructionIcon(context)
        this.text = getInstructionText(context)
        this.distRatio = distRatio
    }

    fun getInstructionIcon(context: Context): Drawable? {
        when (sign) {
            Instruction.TURN_SHARP_LEFT -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_turn_sharp_left, null
            )!!

            Instruction.TURN_LEFT -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_turn_left, null
            )

            Instruction.TURN_SLIGHT_LEFT -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_turn_slight_left, null
            )

            Instruction.CONTINUE_ON_STREET -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_continue_straight, null
            )

            Instruction.TURN_SLIGHT_RIGHT -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_turn_slight_right, null
            )

            Instruction.TURN_RIGHT -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_turn_right, null
            )

            Instruction.TURN_SHARP_RIGHT -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_turn_sharp_right, null
            )

            Instruction.FINISH -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_pin_drop_dark_30dp, null
            )

            Instruction.REACHED_VIA -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_arrive_straight, null
            )
            Instruction.START -> return VectorDrawableCompat.create(
                context.resources,
                R.drawable.baseline_route_24, null
            )
        }
        return null
    }

    fun getInstructionIconId(): Int {
        //Log.i(logtag, "${Thread.currentThread().getStackTrace()[2].lineNumber}: sign:$sign")
        when (sign) {
            Instruction.TURN_SHARP_LEFT -> return R.drawable.ic_turn_sharp_left
            Instruction.TURN_LEFT -> return R.drawable.ic_turn_left
            Instruction.TURN_SLIGHT_LEFT -> return R.drawable.ic_turn_slight_left
            Instruction.CONTINUE_ON_STREET -> return R.drawable.ic_continue_straight
            Instruction.TURN_SLIGHT_RIGHT -> return R.drawable.ic_turn_slight_right
            Instruction.TURN_RIGHT -> return R.drawable.ic_turn_right
            Instruction.TURN_SHARP_RIGHT -> return R.drawable.ic_turn_sharp_right
            Instruction.FINISH -> return R.drawable.ic_pin_drop_dark_30dp
            Instruction.REACHED_VIA -> return R.drawable.ic_arrive_straight
            Instruction.START -> return R.drawable.baseline_route_24
        }
        return R.drawable.ic_pin_drop_dark_30dp
    }

    private fun getInstructionText(context: Context): String? {
        if (sign == Instruction.FINISH) return context.getString(R.string.navigation_end) //4

        var dir: String? = ""
        when (sign) {
            Instruction.TURN_SHARP_LEFT -> dir = context.getString(R.string.turn_sharp_left)
            Instruction.TURN_LEFT -> dir = context.getString(R.string.turn_left)
            Instruction.TURN_SLIGHT_LEFT -> dir = context.getString(R.string.turn_slight_left)
            Instruction.CONTINUE_ON_STREET -> dir = context.getString(R.string.continue_on_street)
            Instruction.TURN_SLIGHT_RIGHT -> dir = context.getString(R.string.turn_slight_right)
            Instruction.TURN_RIGHT -> dir = context.getString(R.string.turn_right)
            Instruction.TURN_SHARP_RIGHT -> dir = context.getString(R.string.turn_sharp_right)
            Instruction.REACHED_VIA -> dir = context.getString(R.string.reached_via)
            Instruction.START -> dir = ""
        }
        return dir
    }

    @Throws(JSONException::class)
    fun toJson(): String {
        Log.i(logtag, "${Thread.currentThread().getStackTrace()[2].lineNumber}: $json")
        return this.json.toString()
    }

    @get:Throws(JSONException::class)
    val json: JSONObject
        get() {
            val jo = JSONObject()
            if (sign == Instruction.CONTINUE_ON_STREET)
                jo.put(INSTRUCTION_NAME, nameAfter)
            else
                jo.put(INSTRUCTION_NAME, "${nameBefore}/${nameAfter}")
            jo.put(INSTRUCTION_DISTANCE, distanceM)
            jo.put(INSTRUCTION_LEG_DISTANCE, legDistanceM)
            jo.put(INSTRUCTION_SIGN, sign)
            latLong?.let {
                jo.put(INSTRUCTION_LATITUDE, it.latitude)
                jo.put(INSTRUCTION_LONGITUDE, it.longitude)
            }
            return jo
        }

    override fun toString(): String {
        return if (sign == Instruction.CONTINUE_ON_STREET)
            "$text ${nameAfter} ${legDistanceM}m ${distanceM}m ${(100*distRatio).format(0)}%"
        else
            "$text ${nameBefore}/${nameAfter} ${legDistanceM}m ${distanceM}m ${(100*distRatio).format(0)}%"
    }

    companion object {
        private const val INSTRUCTION_SIGN = "instruction.sign"
        private const val INSTRUCTION_NAME = "instruction.name"
        private const val INSTRUCTION_DISTANCE = "instruction.distance"
        private const val INSTRUCTION_LEG_DISTANCE = "instruction.leg.distance"
        private const val INSTRUCTION_LATITUDE = "instruction.latitude"
        private const val INSTRUCTION_LONGITUDE = "instruction.longitude"
    }
}
