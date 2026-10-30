package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.*
import org.junit.Assert.*
import org.junit.Test

class PdfLayoutTest {
    @Test fun longValuesUseReadableFontSizesAndAbbreviationInsideTheirField() {
        val box=TextCell(10f,20f,40f,30f,CellAlign.CENTER)
        val result=TextFit.fit("Nome lunghissimo ".repeat(30),box,7f,.93f,-.24f){it.length*.6f}
        assertEquals(5.5f,result.size,.001f);assertTrue(result.text.endsWith("…"))
        assertTrue(result.x>=box.left);assertTrue(result.x+result.width<=box.right+.001f)
        assertTrue(result.baseline-.93f*result.size>=box.top-.001f)
        assertTrue(result.baseline+.24f*result.size<=box.bottom+.001f)
    }
    @Test fun allPrintedTextHasAnExplicitWritingAreaWithinThePage() {
        val pages=SheetLayout.build(PdfFixtures.completeWithLongFields())
        pages.forEach { page->page.marks.filter{it.type=="text"}.forEach { mark->
            val cell=mark.cell!!
            assertTrue(cell.left>=0 && cell.top>=0 && cell.right<=1190 && cell.bottom<=730)
        }}
        val complete=pages.drop(1).flatMap{it.marks}.joinToString(" "){it.text}
        assertTrue(complete.contains("Ufficiale 6 CognomeComposto"))
        assertTrue(complete.contains("Panchina 1-4 CognomeComposto"))
        assertTrue(complete.contains("punti oltre 48"))
        assertTrue(complete.contains("Nota aggiuntiva"))
    }
    @Test fun officialsStayInNameAndCityColumnsAndJudgesFollowPrintedOneThreeTwoFourOrder() {
        val config=Demo.config().copy(officials=(0..6).map{"Official-$it"},officialCities=(0..6).map{"City-$it"})
        val marks=SheetLayout.build(Journal(config=config)).first().marks
        for(i in 0..2) {
            val name=marks.single{it.text=="Official-$i"}.cell!!
            val city=marks.single{it.text=="City-$i"}.cell!!
            assertTrue(name.right<410);assertTrue(city.left>410 && city.right<514)
            assertTrue(name.top>=647+i*13 && name.bottom<661+i*13)
        }
        val judges=(3..6).map{i->marks.single{it.text=="Official-$i"}.cell!!}
        assertEquals(judges[0].top,judges[1].top,.01f)
        assertEquals(judges[2].top,judges[3].top,.01f)
        assertTrue(judges[0].left<judges[1].left)
        assertTrue(judges.all{it.bottom<712})
    }
    @Test fun homeAndAwayOrderIsPreservedWhenTheHomeTeamDrawsTheRightEnd() {
        val journal=Journal(config=Demo.config(),events=listOf(Demo.start().copy(left=1)))
        val marks=SheetLayout.build(journal).first().marks
        assertEquals(journal.config.teams[0].name,marks.single{it.cell?.left==43f}.text)
        assertEquals("B",marks.single{it.cell?.left==23f && it.cell.top==48f}.text)
        assertEquals("A",marks.single{it.cell?.left==594f && it.cell.top==48f}.text)
        assertTrue(marks.any{it.text==journal.config.teams[0].players[0].name && it.cell!!.left<1050})
    }
    @Test fun durationsDoNotCoverPrintedParenthesesAndTimesStayBeforeTheirUnits() {
        val marks=SheetLayout.build(PdfFixtures.completeWithLongFields()).first().marks
        val durations=marks.filter{it.cell?.left==803f}
        assertEquals(5,durations.size)
        assertTrue(durations.all{it.cell!!.right<=820 && it.cell.top>=570 && it.cell.bottom<=663})
        val timeCells=marks.filter{it.cell?.top==695f}.map{it.cell!!}
        assertEquals(6,timeCells.size)
        assertEquals(listOf(706f,729f,788f,810f,861f,884f),timeCells.map{it.right})
        val checkPoint=marks.single{it.cell?.left==806f && it.cell.top==366f}
        assertTrue(checkPoint.text.toInt() in 0..8)
    }
    @Test fun threeSetMatchUsesTheDecidingGridAndOnlyReplacesThePrintedFive() {
        var journal=Journal(config=Demo.config().copy(format=Format.BEST_OF_THREE))
        for(index in 0..1) {
            journal=MatchEngine.add(journal,Demo.start().copy(left=index%2,server=index%2))
            repeat(25){journal=MatchEngine.add(journal,Event(Kind.POINT,index))}
        }
        journal=MatchEngine.add(journal,Demo.start().copy(left=0,server=0))
        val marks=SheetLayout.build(journal).first().marks
        val replacement=marks.single{it.type=="fill"}
        assertEquals(75f,replacement.x,.001f);assertEquals(461f,replacement.y,.001f)
        assertTrue(replacement.x2<=13f && replacement.y2<=19f)
        assertEquals("3",marks.single{it.cell?.top==463f}.text)
        assertFalse(marks.any{it.cell?.top==73f && it.text in listOf("A","B")})
    }

}
