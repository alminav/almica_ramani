package com.almica.ramani

object FastMath {
    fun abs(value: Float): Float {
        return if (value < 0) -value else value
    }

    fun absMax(value1: Float, value2: Float): Float {
        val a1 = if (value1 < 0) -value1 else value1
        val a2 = if (value2 < 0) -value2 else value2
        return if (a2 < a1) a1 else a2
    }

    /**
     * test if any absolute value is greater than 'cmp'
     */
    fun absMaxCmp(value1: Float, value2: Float, cmp: Float): Boolean {
        return value1 < -cmp || value1 > cmp || value2 < -cmp || value2 > cmp
    }

    /**
     * test if any absolute value is greater than 'cmp'
     */
    fun absMaxCmp(value1: Int, value2: Int, cmp: Int): Boolean {
        return value1 < -cmp || value1 > cmp || value2 < -cmp || value2 > cmp
    }

    fun clamp(value: Int, min: Int, max: Int): Int {
        return if (value < min) min else if (value > max) max else value
    }

    fun clamp(value: Float, min: Float, max: Float): Float {
        return if (value < min) min else if (value > max) max else value
    }

    fun clamp(value: Double, min: Double, max: Double): Double {
        return if (value < min) min else if (value > max) max else value
    }

    /**
     * Returns normalized degree in range of -180° to +180°
     */
    fun clampDegree(degree: Double): Double {
        var degree = degree
        while (degree > 180) degree -= 360.0
        while (degree < -180) degree += 360.0
        return degree
    }

    fun clampN(value: Float): Float {
        return if (value < 0f) 0f else if (value > 1f) 1f else value
    }

    /**
     * Returns normalized radian in range of -PI to +PI
     */
    fun clampRadian(radian: Double): Double {
        var radian = radian
        while (radian > Math.PI) radian -= 2 * Math.PI
        while (radian < -Math.PI) radian += 2 * Math.PI
        return radian
    }

    fun clampToByte(value: Int): Byte {
        return (if (value < 0) 0 else if (value > 255) 255 else value).toByte()
    }

    /**
     * Integer version of log2(x)
     *
     *
     * from http://graphics.stanford.edu/~seander/bithacks.html#IntegerLog
     */
    fun log2(x: Int): Int {
        var x = x
        var r = 0 // result of log2(v) will go here
        if (x and -0x10000 != 0) {
            x = x shr 16
            r = r or 16
        }
        if (x and 0xFF00 != 0) {
            x = x shr 8
            r = r or 8
        }
        if (x and 0xF0 != 0) {
            x = x shr 4
            r = r or 4
        }
        if (x and 0xC != 0) {
            x = x shr 2
            r = r or 2
        }
        if (x and 0x2 != 0) {
            r = r or 1
        }
        return r
    }

    /**
     * Integer version of 2^x
     */
    fun pow(x: Int): Float {
        if (x == 0) return 1f
        return if (x > 0) (1 shl x).toFloat() else 1.0f / (1 shl -x)
    }

    fun round2(value: Float): Float {
        return Math.round(value * 100) / 100f
    }

    fun withinSquaredDist(dx: Int, dy: Int, distance: Int): Boolean {
        return dx * dx + dy * dy < distance
    }

    fun withinSquaredDist(dx: Float, dy: Float, distance: Float): Boolean {
        return dx * dx + dy * dy < distance
    }
}

