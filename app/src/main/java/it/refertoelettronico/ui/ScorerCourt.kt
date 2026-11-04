package it.refertoelettronico.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.refertoelettronico.model.*
import kotlin.math.roundToInt

/** A single court seen from the scorer's table, with two opposing, upright formations. */
@Composable
fun ScorerCourt(config: MatchConfig, state: MatchState, modifier: Modifier = Modifier,compact:Boolean=false) {
    val set = state.current ?: return
    val left = CourtGeometry.leftTeam(set)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${state.letter(left)} · ${if(compact)shortTeamName(config.teams[left].name)else config.teams[left].name}", Modifier.weight(1f), fontSize=12.sp, fontWeight=FontWeight.Bold)
            Text("RETE", fontSize=9.sp, color=MaterialTheme.colorScheme.primary)
            Text("${state.letter(1-left)} · ${if(compact)shortTeamName(config.teams[1-left].name)else config.teams[1-left].name}", Modifier.weight(1f), fontSize=12.sp,
                fontWeight=FontWeight.Bold, textAlign=androidx.compose.ui.text.style.TextAlign.End)
        }
        // Switching ends is a protocol change; never animate athletes through the net.
        key(set.number, left) {
            BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(if(LocalAppearance.current.layout==InterfaceStyle.COURT)1.55f else 1.9f).background(Color(0xFFDCE9E5), RoundedCornerShape(14.dp)).testTag("scorer-court")) {
                val density = LocalDensity.current
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val inset = with(density) { 9.dp.toPx() }
                val token = if (maxWidth < 460.dp) 40.dp else 48.dp
                val tokenPx = with(density) { token.toPx() }
                val courtWidth = w-2*inset
                val courtHeight = h-2*inset
                Canvas(Modifier.fillMaxSize()) { drawCourt(inset) }
                if(!compact)for (end in CourtEnd.entries) for (zone in 1..6) {
                    val p = CourtGeometry.position(end, zone)
                    Text("Z$zone", fontSize=8.sp, color=Color(0xFF775337), modifier=Modifier
                        .offset { IntOffset((inset+p.x*courtWidth-with(density){9.dp.toPx()}).roundToInt(),
                            (inset+p.y*courtHeight+with(density){17.dp.toPx()}).roundToInt()) }
                        .testTag("court-zone-${end.name.lowercase()}-$zone"))
                }
                for (team in 0..1) {
                    val end = if (team == left) CourtEnd.LEFT else CourtEnd.RIGHT
                    val visible = set.visible(team)
                    visible.forEachIndexed { pos, jersey ->
                        // Identity belongs to the athlete, not to the zone she is moving into.
                        key(team, jersey) {
                            val point = CourtGeometry.position(end, pos+1)
                            val target = Offset(inset+point.x*courtWidth-tokenPx/2, inset+point.y*courtHeight-tokenPx/2)
                            val animated by animateOffsetAsState(targetValue=target,
                                animationSpec=tween(CourtGeometry.ROTATION_MILLIS,easing=FastOutSlowInEasing), label="rotation-$team-$jersey")
                            val libero = set.liberos[team]?.number == jersey
                            val unavailable = jersey in state.disqualified[team] || jersey in state.retired[team] || jersey in set.expelled[team]
                            val serves = pos == 0 && set.server == team && !libero && !unavailable
                            val name = config.teams[team].players.find { it.number == jersey }?.name.orEmpty()
                            Box(Modifier.offset { IntOffset(animated.x.roundToInt(),animated.y.roundToInt()) }
                                .size(token).testTag("court-player-$team-$jersey")
                                .semantics { contentDescription="$name, maglia $jersey, zona ${pos+1}${if(libero) ", libero" else ""}${if(serves) ", al servizio" else ""}" },
                                contentAlignment=Alignment.Center) {
                                Surface(shape=CircleShape, modifier=Modifier.size(token-8.dp),
                                    color=if(unavailable)Color(0xFFFFDADB) else if(libero)Color(0xFFEED7EF) else config.teams[team].displayColor(team),
                                    border=BorderStroke(if(serves)2.dp else 1.dp,if(serves)Color(0xFFDCA849) else Color(0xFF89A5A0)),
                                    shadowElevation=2.dp) {
                                    Box(contentAlignment=Alignment.Center) {
                                        Text(jersey.toString(), fontSize=if(token==40.dp)16.sp else 20.sp, fontWeight=FontWeight.Bold,
                                            color=if(unavailable || libero)Color(0xFF183E48) else teamInk(config.teams[team].displayColor(team)))
                                    }
                                }
                                if(libero) Text("L",fontSize=9.sp,fontWeight=FontWeight.Bold,color=if(compact)Color.Black else Color(0xFF743A78),
                                    modifier=Modifier.align(Alignment.TopStart).background(if(compact)Color(0xFF55CFF2)else Color.White,CircleShape).padding(horizontal=3.dp))
                                if(serves) VolleyballIcon(Modifier.align(Alignment.TopEnd).size(19.dp).testTag("service-ball")
                                    .semantics{contentDescription="Pallone da pallavolo: al servizio"})
                            }
                        }
                    }
                }
            }
        }
        if(!compact)Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.size(25.dp,15.dp)) {
                drawRoundRect(Color(0xFF315F67),topLeft=Offset(0f,0f),size=Size(size.width,size.height*.45f),cornerRadius=CornerRadius(2.dp.toPx()))
                drawLine(Color(0xFF315F67),Offset(size.width*.18f,size.height*.4f),Offset(size.width*.18f,size.height),2.dp.toPx())
                drawLine(Color(0xFF315F67),Offset(size.width*.82f,size.height*.4f),Offset(size.width*.82f,size.height),2.dp.toPx())
            }
            Text("TAVOLO DEL SEGNAPUNTI · TU SEI QUI",fontSize=9.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCourt(inset: Float) {
    val l=inset;val t=inset;val r=size.width-inset;val b=size.height-inset
    val mid=size.width/2;val cw=r-l;val ch=b-t
    drawRect(Color(0xFFE9B781),Offset(l,t),Size(cw,ch))
    // Subtle floor boards, boundary and attack lines (3 m from the net).
    repeat(9) { i -> val y=t+ch*i/8;drawLine(Color(0x22935C2E),Offset(l,y),Offset(r,y),.6.dp.toPx()) }
    drawRect(Color.White,Offset(l,t),Size(cw,ch),style=Stroke(1.7.dp.toPx()))
    for(x in listOf(l+cw/3,l+cw*2/3))drawLine(Color.White,Offset(x,t),Offset(x,b),1.5.dp.toPx())
    drawLine(Color.White,Offset(mid,t),Offset(mid,b),1.5.dp.toPx())
    val mesh=7.dp.toPx()
    drawRect(Color(0x770F333A),Offset(mid-mesh/2,t),Size(mesh,ch))
    for(i in 0..3) {
        val x=mid-mesh/2+mesh*i/3
        drawLine(Color(0xFFE5E9E7),Offset(x,t),Offset(x,b),.6.dp.toPx())
    }
    val rows=(ch/5.dp.toPx()).toInt().coerceAtLeast(1)
    for(i in 0..rows) {
        val y=t+ch*i/rows
        drawLine(Color(0xFFE5E9E7),Offset(mid-mesh/2,y),Offset(mid+mesh/2,y),.6.dp.toPx())
    }
    drawLine(Color.White,Offset(mid-mesh/2,t),Offset(mid-mesh/2,b),1.4.dp.toPx())
    drawLine(Color(0xFF234D55),Offset(mid+mesh/2,t),Offset(mid+mesh/2,b),1.1.dp.toPx())
    for(y in listOf(t-2.dp.toPx(),b+2.dp.toPx())) {
        drawCircle(Color(0xFF123D49),4.dp.toPx(),Offset(mid,y))
        drawCircle(Color(0xFFF5F5EB),2.dp.toPx(),Offset(mid,y))
    }
}

/** Volleyball panel seams, deliberately distinct from a football/soccer symbol. */
@Composable
fun VolleyballIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius=size.minDimension/2-1.dp.toPx();val center=Offset(size.width/2,size.height/2)
        drawCircle(Color.White,radius,center)
        for(angle in listOf(0f,120f,240f))rotate(angle,center) {
            val panel=Path().apply {
                moveTo(center.x,center.y)
                cubicTo(center.x-radius*.1f,center.y-radius*.8f,center.x+radius*.4f,center.y-radius,center.x+radius*.72f,center.y-radius*.69f)
                cubicTo(center.x+radius*.35f,center.y-radius*.6f,center.x+radius*.35f,center.y-radius*.2f,center.x,center.y)
                close()
            }
            drawPath(panel,Color(0xFFF2C34C))
            val seam=Path().apply {moveTo(center.x,center.y);cubicTo(center.x-radius*.1f,center.y-radius*.7f,center.x+radius*.25f,center.y-radius*.94f,center.x+radius*.66f,center.y-radius*.74f)}
            drawPath(seam,Color(0xFF22618B),style=Stroke(1.dp.toPx()))
            val outer=Path().apply {moveTo(center.x+radius*.29f,center.y-radius*.18f);cubicTo(center.x+radius*.69f,center.y-radius*.28f,center.x+radius*.89f,center.y-radius*.17f,center.x+radius*.97f,center.y+radius*.17f)}
            drawPath(outer,Color(0xFF22618B),style=Stroke(.8.dp.toPx()))
        }
        drawCircle(Color(0xFF174756),radius,center,style=Stroke(.8.dp.toPx()))
    }
}
