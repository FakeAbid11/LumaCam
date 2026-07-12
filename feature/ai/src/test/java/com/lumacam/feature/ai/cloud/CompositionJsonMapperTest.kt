package com.lumacam.feature.ai.cloud

import com.lumacam.feature.ai.MoveDirection
import com.lumacam.feature.ai.NormalizedPoint
import com.lumacam.feature.ai.RecommendedAction
import com.lumacam.feature.ai.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionJsonMapperTest {

    @Test
    fun parsesFullValidJson() {
        val json = """
            {"sceneType":"landscape","compositionScore":82,"suggestedDirection":"left",
             "tiltAngle":-3.5,"lighting":{"label":"Golden hour","description":"Warm, soft light"},
             "suggestions":["Level the horizon","Move left a touch"]}
        """.trimIndent()
        val result = CompositionJsonMapper.parse(json)
        assertNotNull(result)
        result!!
        assertEquals(SceneType.LANDSCAPE, result.sceneType)
        assertEquals(82, result.compositionScore)
        assertEquals(MoveDirection.LEFT, result.suggestedDirection)
        assertEquals(-3.5f, result.tiltAngle, 1e-3f)
        assertEquals("Golden hour", result.lighting.label)
        assertEquals(2, result.suggestions.size)
    }

    @Test
    fun parsesJsonWrappedInCodeFencesAndProse() {
        val raw = """
            Sure! Here is the analysis:
            ```json
            {"sceneType":"PORTRAIT","compositionScore":70,"suggestedDirection":"UP"}
            ```
            Hope that helps.
        """.trimIndent()
        val result = CompositionJsonMapper.parse(raw)
        assertNotNull(result)
        assertEquals(SceneType.PORTRAIT, result!!.sceneType)
        assertEquals(MoveDirection.UP, result.suggestedDirection)
    }

    @Test
    fun clampsScoreOutOfRange() {
        assertEquals(100, CompositionJsonMapper.parse("""{"compositionScore":150}""")!!.compositionScore)
        assertEquals(0, CompositionJsonMapper.parse("""{"compositionScore":-20}""")!!.compositionScore)
    }

    @Test
    fun normalizesEnumSynonyms() {
        val r = CompositionJsonMapper.parse(
            """{"sceneType":"city","suggestedDirection":"downward"}"""
        )!!
        assertEquals(SceneType.ARCHITECTURE, r.sceneType)
        assertEquals(MoveDirection.DOWN, r.suggestedDirection)
    }

    @Test
    fun appliesDefaultsForMissingFields() {
        val r = CompositionJsonMapper.parse("{}")!!
        assertEquals(0, r.compositionScore)
        assertEquals(MoveDirection.NONE, r.suggestedDirection)
        assertEquals(SceneType.UNKNOWN, r.sceneType)
        assertTrue(r.suggestions.isEmpty())
        assertNull(r.targetCrop)
        assertEquals("Lighting", r.lighting.label)
    }

    @Test
    fun cappsSuggestionsAtThree() {
        val r = CompositionJsonMapper.parse(
            """{"suggestions":["a","b","c","d","e"]}"""
        )!!
        assertEquals(3, r.suggestions.size)
    }

    @Test
    fun dropsInvalidCrop() {
        val r = CompositionJsonMapper.parse(
            """{"targetCrop":{"left":0.8,"top":0.1,"right":0.2,"bottom":0.9}}"""
        )!!
        assertNull(r.targetCrop)
    }

    @Test
    fun keepsValidCrop() {
        val r = CompositionJsonMapper.parse(
            """{"targetCrop":{"left":0.1,"top":0.1,"right":0.9,"bottom":0.9}}"""
        )!!
        assertNotNull(r.targetCrop)
        assertEquals(0.9f, r.targetCrop!!.right, 1e-3f)
    }

    @Test
    fun returnsNullForNonJson() {
        assertNull(CompositionJsonMapper.parse("I could not analyze this image."))
    }

    @Test
    fun returnsNullForNullOrBlank() {
        assertNull(CompositionJsonMapper.parse(null))
        assertNull(CompositionJsonMapper.parse("   "))
    }

    @Test
    fun extractJsonObjectWithNesting() {
        val extracted = CompositionJsonMapper.extractJsonObject(
            "prefix {\"a\":{\"b\":1},\"c\":2} suffix"
        )
        assertEquals("{\"a\":{\"b\":1},\"c\":2}", extracted)
    }

    @Test
    fun parsesNewGuidanceFieldsWhenPresent() {
        val json = """
            {"sceneType":"food","compositionScore":64,"suggestedDirection":"none",
             "subjectPoint":{"x":0.6,"y":0.4},
             "recommendedAction":"reposition",
             "primaryGuidance":"Food shot — nudge your subject onto a rule-of-thirds line."}
        """.trimIndent()
        val r = CompositionJsonMapper.parse(json)!!
        assertEquals(NormalizedPoint(0.6f, 0.4f), r.subjectPoint)
        assertEquals(RecommendedAction.REPOSITION, r.recommendedAction)
        assertEquals(
            "Food shot — nudge your subject onto a rule-of-thirds line.",
            r.primaryGuidance
        )
    }

    @Test
    fun dropsMissingGuidanceFieldsForBackwardCompat() {
        val r = CompositionJsonMapper.parse(
            """{"sceneType":"landscape","compositionScore":80}"""
        )!!
        assertNull(r.subjectPoint)
        assertNull(r.recommendedAction)
        assertNull(r.primaryGuidance)
    }

    @Test
    fun clampsSubjectPointIntoUnitSquare() {
        val r = CompositionJsonMapper.parse(
            """{"subjectPoint":{"x":1.4,"y":-0.2}}"""
        )!!
        assertEquals(NormalizedPoint(1f, 0f), r.subjectPoint)
    }

    @Test
    fun dropsActionWhenUnrecognized() {
        val r = CompositionJsonMapper.parse(
            """{"recommendedAction":"wobble"}"""
        )!!
        assertEquals(RecommendedAction.NONE, r.recommendedAction)
    }
}
}
