package it.refertoelettronico.pdf

import it.refertoelettronico.model.*
import kotlin.math.min

/** Use the writable row, preserving its printed caption and the autograph's proportions. */
object SignatureGeometry {
    private const val padding=.35f
    fun cell(role:SignatureRole,signature:MatchSignature?=null):TextCell {
        val areas=when(role) {
            SignatureRole.CAPTAIN_HOME->listOf(TextCell(931f,692f,1053f,704f),TextCell(954f,687.5f,1053f,704f))
            SignatureRole.COACH_HOME->listOf(TextCell(931f,712f,1053f,723.5f),TextCell(954f,707f,1053f,723.5f))
            SignatureRole.CAPTAIN_AWAY->listOf(TextCell(1061f,692f,1184f,704f),TextCell(1085f,687.5f,1184f,704f))
            SignatureRole.COACH_AWAY->listOf(TextCell(1061f,712f,1184f,723.5f),TextCell(1085f,707f,1184f,723.5f))
            SignatureRole.FIRST_REFEREE->listOf(TextCell(516f,647f,660f,658f))
            SignatureRole.SECOND_REFEREE->listOf(TextCell(516f,660f,660f,671.5f))
            SignatureRole.SCORER->listOf(TextCell(516f,673.5f,660f,684f))
        }
        if(signature==null)return areas.first()
        val bounds=bounds(signature)
        return areas.maxBy{min((it.width-2*padding)/bounds[2],(it.height-2*padding)/bounds[3])}
    }
    private fun bounds(record:MatchSignature):FloatArray {
        val points=record.strokes.flatten()
        val left=points.minOf{it.x}*record.aspectRatio;val top=points.minOf{it.y}
        return floatArrayOf(left,top,((points.maxOf{it.x}-points.minOf{it.x})*record.aspectRatio).coerceAtLeast(.0001f),
            (points.maxOf{it.y}-top).coerceAtLeast(.0001f))
    }
    fun fit(record:MatchSignature,cell:TextCell):List<List<SignaturePoint>> {
        val (left,top,width,height)=bounds(record)
        val scale=min((cell.width-2*padding)/width,(cell.height-2*padding)/height)
        val x=cell.left+(cell.width-width*scale)/2;val y=cell.top+(cell.height-height*scale)/2
        return record.strokes.map{stroke->stroke.map{p->SignaturePoint(x+(p.x*record.aspectRatio-left)*scale,y+(p.y-top)*scale)}}
    }
}
