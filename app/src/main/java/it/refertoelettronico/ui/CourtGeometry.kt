package it.refertoelettronico.ui

import it.refertoelettronico.model.SetState

enum class CourtEnd { LEFT, RIGHT }
data class CourtPoint(val x: Float, val y: Float)

/** Scorer sits below the court; the net is vertical. Coordinates use the whole playing area. */
object CourtGeometry {
    const val ROTATION_MILLIS = 650
    fun position(end: CourtEnd, zone: Int): CourtPoint {
        val left = when (zone) {
            1 -> CourtPoint(.13f, .8f)
            2 -> CourtPoint(.37f, .8f)
            3 -> CourtPoint(.37f, .5f)
            4 -> CourtPoint(.37f, .2f)
            5 -> CourtPoint(.13f, .2f)
            6 -> CourtPoint(.13f, .5f)
            else -> throw IllegalArgumentException("Zona non valida: $zone")
        }
        return if (end == CourtEnd.LEFT) left else CourtPoint(1f-left.x, 1f-left.y)
    }
    fun rows(end: CourtEnd): List<List<Int>> = if (end == CourtEnd.LEFT)
        listOf(listOf(5,4),listOf(6,3),listOf(1,2))
    else listOf(listOf(2,1),listOf(3,6),listOf(4,5))

    fun leftTeam(set: SetState): Int = if (set.changedEndsAt == null) set.initialLeft else 1-set.initialLeft
}
