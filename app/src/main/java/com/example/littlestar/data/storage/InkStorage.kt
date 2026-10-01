package com.example.littlestar.data.storage

import android.content.Context
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * 악보 페이지 위 필기 획(Stroke)을 페이지별로 저장/로드한다.
 *
 * 저장 경로: filesDir/sheets/sheet-{id}/page-{NNN}-ink.dat
 * 각 획의 입력 배치(StrokeInputBatch)와 브러시 패밀리(BrushFamily)를
 * ink-storage(스트림 직렬화)로 저장하고, 색상/굵기/epsilon은 원시 값으로 함께 저장한다.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
class InkStorage(private val context: Context) {

    private val filesDir: File get() = context.filesDir

    fun inkRelativePath(sheetId: Long, pageOrder: Int): String =
        "sheets/sheet-$sheetId/page-%03d-ink.dat".format(pageOrder)

    suspend fun saveStrokes(sheetId: Long, pageOrder: Int, strokes: List<Stroke>) =
        withContext(Dispatchers.IO) {
            val file = File(filesDir, inkRelativePath(sheetId, pageOrder))
            file.parentFile?.mkdirs()
            if (strokes.isEmpty()) {
                file.delete()
                return@withContext
            }
            DataOutputStream(file.outputStream()).use { out ->
                out.writeInt(strokes.size)
                for (stroke in strokes) {
                    val inputsBytes = ByteArrayOutputStream().use { baos ->
                        stroke.inputs.encode(baos)
                        baos.toByteArray()
                    }
                    val brushBytes = ByteArrayOutputStream().use { baos ->
                        stroke.brush.family.encode(baos)
                        baos.toByteArray()
                    }
                    out.writeInt(inputsBytes.size)
                    out.write(inputsBytes)
                    out.writeInt(brushBytes.size)
                    out.write(brushBytes)
                    out.writeLong(stroke.brush.colorLong)
                    out.writeFloat(stroke.brush.size)
                    out.writeFloat(stroke.brush.epsilon)
                }
            }
        }

    suspend fun loadStrokes(sheetId: Long, pageOrder: Int): List<Stroke> =
        withContext(Dispatchers.IO) {
            val file = File(filesDir, inkRelativePath(sheetId, pageOrder))
            if (!file.exists()) return@withContext emptyList()
            runCatching {
                DataInputStream(file.inputStream()).use { input ->
                    val count = input.readInt()
                    buildList {
                        repeat(count) {
                            val inputsBytes = ByteArray(input.readInt())
                            input.readFully(inputsBytes)
                            val brushBytes = ByteArray(input.readInt())
                            input.readFully(brushBytes)
                            val colorLong = input.readLong()
                            val size = input.readFloat()
                            val epsilon = input.readFloat()

                            val inputs = StrokeInputBatch.decode(ByteArrayInputStream(inputsBytes))
                            val family = BrushFamily.decode(ByteArrayInputStream(brushBytes))
                            val brush = Brush.createWithColorLong(family, colorLong, size, epsilon)
                            add(Stroke(brush, inputs))
                        }
                    }
                }
            }.getOrElse { emptyList() }
        }
}
