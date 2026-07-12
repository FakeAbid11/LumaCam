package com.lumacam.feature.ai.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [SubjectDetector] backed by bundled, offline Google ML Kit (PRD §4 Tier 2 —
 * "Luma Vision"): face detection, object detection and image labeling. Each pass
 * runs on one frame and never throws — failures degrade to an empty result so the
 * scorer can still work from sensor tilt and brightness alone.
 */
class MlKitSubjectDetector : SubjectDetector {

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(LABEL_CONFIDENCE)
            .build()
    )

    override suspend fun detect(bitmap: Bitmap): DetectionOutput {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return DetectionOutput()

        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(image).await().orEmpty()
            val objects = objectDetector.process(image).await().orEmpty()
            val labels = imageLabeler.process(image).await().orEmpty()

            val subjects = buildList {
                faces.forEach { face ->
                    val b = face.boundingBox
                    add(
                        DetectedSubject(
                            type = SubjectType.FACE,
                            box = NormalizedBox.fromPixels(
                                b.left.toFloat(), b.top.toFloat(),
                                b.right.toFloat(), b.bottom.toFloat(), w, h
                            ),
                            confidence = 1f
                        )
                    )
                }
                objects.forEach { obj ->
                    val b = obj.boundingBox
                    val best = obj.labels.maxByOrNull { it.confidence }
                    add(
                        DetectedSubject(
                            type = SubjectType.OBJECT,
                            box = NormalizedBox.fromPixels(
                                b.left.toFloat(), b.top.toFloat(),
                                b.right.toFloat(), b.bottom.toFloat(), w, h
                            ),
                            confidence = best?.confidence ?: 0.5f,
                            label = best?.text
                        )
                    )
                }
            }

            val labelTexts = (
                labels.map { it.text } + objects.flatMap { o -> o.labels.map { it.text } }
                ).distinct()

            DetectionOutput(subjects = subjects, labels = labelTexts)
        } catch (e: Exception) {
            Log.w(TAG, "ML Kit detection failed", e)
            DetectionOutput()
        }
    }

    override fun close() {
        faceDetector.close()
        objectDetector.close()
        imageLabeler.close()
    }

    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
        addOnCanceledListener { cont.resume(null) }
    }

    private companion object {
        const val TAG = "MlKitSubjectDetector"
        const val LABEL_CONFIDENCE = 0.6f
    }
}
