package com.lumacam.feature.ai.vision

/** Kind of subject a detector found in the frame. */
enum class SubjectType { FACE, PERSON, OBJECT, UNKNOWN }

/**
 * A single detected subject in normalized coordinates. Detector-agnostic so the
 * scoring core never depends on ML Kit / MediaPipe types.
 */
data class DetectedSubject(
    val type: SubjectType,
    val box: NormalizedBox,
    val confidence: Float,
    val label: String? = null
)

/**
 * The combined output of one detection pass: located subjects plus coarse scene
 * labels (e.g. from image labeling) used for scene classification.
 */
data class DetectionOutput(
    val subjects: List<DetectedSubject> = emptyList(),
    val labels: List<String> = emptyList()
) {
    val faceCount: Int get() = subjects.count { it.type == SubjectType.FACE }

    /**
     * The most prominent subject: prefer faces, then break ties by area then
     * confidence. Null when nothing was detected.
     */
    fun primarySubject(): DetectedSubject? {
        if (subjects.isEmpty()) return null
        val faces = subjects.filter { it.type == SubjectType.FACE }
        val pool = faces.ifEmpty { subjects }
        return pool.maxWithOrNull(
            compareBy<DetectedSubject> { it.box.area }.thenBy { it.confidence }
        )
    }
}
