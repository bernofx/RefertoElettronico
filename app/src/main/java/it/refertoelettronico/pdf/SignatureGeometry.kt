package it.refertoelettronico.pdf

import it.refertoelettronico.model.*
import kotlin.math.min

/** Only writable spaces in the unchanged FIPAV template. Team columns are home/away, with A/B filled after the toss. */
object SignatureGeometry {
    fun cell(role:SignatureRole):TextCell = when(role) {
        SignatureRole.CAPTAIN_HOME->TextCell(932f,693f,1052f,704f)
        SignatureRole.COACH_HOME->TextCell(932f,713f,1052f,724f)
        SignatureRole.CAPTAIN_AWAY->TextCell(1062f,693f,1182f,704f)
        SignatureRole.COACH_AWAY->TextCell(1062f,713f,1182f,724f)
        SignatureRole.FIRST_REFEREE->TextCell(517f,649f,659f,658f)
        SignatureRole.SECOND_REFEREE->TextCell(517f,662.15f,659f,671.15f)
        SignatureRole.SCORER->TextCell(517f,675.3f,659f,684.3f)
    }
    fun fit(record:MatchSignature,cell:TextCell):List<List<SignaturePoint>> {
        val all=record.strokes.flatten()
        val left=all.minOf{it.x}*record.aspectRatio;val top=all.minOf{it.y}
        val width=((all.maxOf{it.x}-all.minOf{it.x})*record.aspectRatio).coerceAtLeast(.0001f)
        val height=(all.maxOf{it.y}-top).coerceAtLeast(.0001f)
        val scale=min((cell.width-2f)/width,(cell.height-2f)/height)
        val x=cell.left+(cell.width-width*scale)/2;val y=cell.top+(cell.height-height*scale)/2
        return record.strokes.map{stroke->stroke.map{p->SignaturePoint(x+(p.x*record.aspectRatio-left)*scale,y+(p.y-top)*scale)}}
    }
}
