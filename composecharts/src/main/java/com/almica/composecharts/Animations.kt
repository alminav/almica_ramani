package com.almica.composecharts.charts

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.TweenSpec

/**
 * Created by bytebeats on 2021/9/24 : 10:53
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

fun simpleChartAnimation(): AnimationSpec<Float> = TweenSpec(durationMillis = 800)
fun repeatableChartAnimation(): AnimationSpec<Float> = InfiniteRepeatableSpec(animation = TweenSpec(durationMillis = 800))
