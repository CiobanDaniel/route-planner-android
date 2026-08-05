package com.danielcioban.routeplanner.data.routing

import android.content.res.Resources
import com.danielcioban.routeplanner.R

/** Formats OSRM maneuver type/modifier into a localized instruction string. */
object ManeuverFormatter {
    fun format(resources: Resources, type: String, modifier: String?, road: String): String {
        val onto = if (road.isNotBlank()) {
            resources.getString(R.string.maneuver_onto, road)
        } else {
            ""
        }
        val mod = modifier?.replace('-', ' ').orEmpty()
        return when (type) {
            "depart" -> if (road.isNotBlank()) {
                resources.getString(R.string.maneuver_head_on, road)
            } else {
                resources.getString(R.string.maneuver_head_out)
            }
            "arrive" -> resources.getString(R.string.maneuver_arrive)
            "new name" -> if (road.isNotBlank()) {
                resources.getString(R.string.maneuver_continue_on, road)
            } else {
                resources.getString(R.string.maneuver_continue)
            }
            "notification" -> resources.getString(R.string.maneuver_continue)
            "roundabout", "rotary" -> {
                val exit = if (mod.isNotBlank()) " ($mod)" else ""
                resources.getString(R.string.maneuver_roundabout, exit, onto)
            }
            "merge" -> resources.getString(R.string.maneuver_merge, onto)
            "fork" -> when (modifier) {
                "left", "slight left" -> resources.getString(R.string.maneuver_keep_left, onto)
                "right", "slight right" -> resources.getString(R.string.maneuver_keep_right, onto)
                else -> resources.getString(R.string.maneuver_fork, onto)
            }
            "end of road" -> when (modifier) {
                "left" -> resources.getString(R.string.maneuver_end_left, onto)
                "right" -> resources.getString(R.string.maneuver_end_right, onto)
                else -> resources.getString(R.string.maneuver_end_road, onto)
            }
            "continue" -> when (modifier) {
                "uturn", "u-turn" -> resources.getString(R.string.maneuver_uturn, onto)
                else -> if (road.isNotBlank()) {
                    resources.getString(R.string.maneuver_continue_on, road)
                } else {
                    resources.getString(R.string.maneuver_continue)
                }
            }
            "turn", "ramp", "on ramp", "off ramp", "exit roundabout", "exit rotary" -> {
                val actionRes = when (modifier) {
                    "uturn", "u-turn" -> R.string.maneuver_uturn_action
                    "sharp left" -> R.string.maneuver_sharp_left
                    "sharp right" -> R.string.maneuver_sharp_right
                    "left" -> R.string.maneuver_turn_left
                    "right" -> R.string.maneuver_turn_right
                    "slight left" -> R.string.maneuver_slight_left
                    "slight right" -> R.string.maneuver_slight_right
                    "straight" -> R.string.maneuver_straight
                    else -> null
                }
                val action = when {
                    actionRes != null -> resources.getString(actionRes)
                    mod.isNotBlank() -> resources.getString(R.string.maneuver_turn_mod, mod)
                    else -> resources.getString(R.string.maneuver_continue)
                }
                "$action$onto"
            }
            else -> {
                val action = when {
                    mod.isNotBlank() -> mod.replaceFirstChar { it.uppercase() }
                    else -> type.replaceFirstChar { it.uppercase() }
                }
                "$action$onto"
            }
        }
    }

    fun formatStep(resources: Resources, step: ManeuverStep): String =
        format(resources, step.type, step.modifier, step.name)
}
