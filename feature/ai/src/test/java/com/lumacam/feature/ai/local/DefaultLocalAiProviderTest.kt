package com.lumacam.feature.ai.local

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLocalAiProviderTest {

    private val image = LocalImage(ByteArray(4), "image/jpeg")
    private val spec = LocalModelCatalog.models.first()
    private val active = ActiveLocalModel(spec = spec, filePath = "/models/${spec.fileName}")

    @Test
    fun `no selected model yields NoModelSelected`() = runTest {
        val provider = DefaultLocalAiProvider(
            engine = FakeLocalInferenceEngine(),
            activeModel = { null }
        )
        val outcome = provider.analyze(image)
        assertTrue(outcome is LocalAiOutcome.Failure)
        assertTrue((outcome as LocalAiOutcome.Failure).error is LocalAiError.NoModelSelected)
    }

    @Test
    fun `missing file yields ModelNotDownloaded`() = runTest {
        val provider = DefaultLocalAiProvider(
            engine = FakeLocalInferenceEngine(),
            activeModel = { active },
            fileExists = { false }
        )
        val outcome = provider.analyze(image)
        val error = (outcome as LocalAiOutcome.Failure).error
        assertTrue(error is LocalAiError.ModelNotDownloaded)
        assertEquals(spec.name, (error as LocalAiError.ModelNotDownloaded).modelName)
    }

    @Test
    fun `valid model output normalizes to CompositionResult`() = runTest {
        val json = """
            {"sceneType":"landscape","compositionScore":82,"suggestedDirection":"left",
             "tiltAngle":-1.2,"lighting":{"label":"Golden","description":"warm"},
             "suggestions":["Level the horizon"]}
        """.trimIndent()
        val engine = FakeLocalInferenceEngine(response = { json })
        val provider = DefaultLocalAiProvider(
            engine = engine,
            activeModel = { active },
            fileExists = { true }
        )
        val outcome = provider.analyze(image, context = "test")
        assertTrue(outcome is LocalAiOutcome.Success)
        val result = (outcome as LocalAiOutcome.Success).result
        assertEquals(82, result.compositionScore)
        assertEquals(1, engine.loadCount)
        assertEquals(1, engine.analyzeCount)
        assertEquals(1, engine.closeCount)
    }

    @Test
    fun `unparseable output yields InferenceFailed`() = runTest {
        val engine = FakeLocalInferenceEngine(response = { "not json at all" })
        val provider = DefaultLocalAiProvider(
            engine = engine,
            activeModel = { active },
            fileExists = { true }
        )
        val outcome = provider.analyze(image)
        assertTrue((outcome as LocalAiOutcome.Failure).error is LocalAiError.InferenceFailed)
    }

    @Test
    fun `out of memory is surfaced as OutOfMemory not crash`() = runTest {
        val engine = FakeLocalInferenceEngine(onLoad = { throw OutOfMemoryError("boom") })
        val provider = DefaultLocalAiProvider(
            engine = engine,
            activeModel = { active },
            fileExists = { true }
        )
        val outcome = provider.analyze(image)
        assertTrue((outcome as LocalAiOutcome.Failure).error is LocalAiError.OutOfMemory)
        assertEquals(1, engine.closeCount)
    }

    @Test
    fun `runtime unavailable is mapped from engine exception`() = runTest {
        val engine = FakeLocalInferenceEngine(onLoad = {
            throw LocalInferenceException(LocalInferenceError.RUNTIME_UNAVAILABLE)
        })
        val provider = DefaultLocalAiProvider(
            engine = engine,
            activeModel = { active },
            fileExists = { true }
        )
        val outcome = provider.analyze(image)
        assertTrue((outcome as LocalAiOutcome.Failure).error is LocalAiError.RuntimeUnavailable)
    }

    @Test
    fun `placeholder engine fails gracefully with RuntimeUnavailable`() = runTest {
        val provider = DefaultLocalAiProvider(
            engine = PlaceholderLocalInferenceEngine(),
            activeModel = { active },
            fileExists = { true }
        )
        val outcome = provider.analyze(image)
        assertTrue((outcome as LocalAiOutcome.Failure).error is LocalAiError.RuntimeUnavailable)
    }
}
