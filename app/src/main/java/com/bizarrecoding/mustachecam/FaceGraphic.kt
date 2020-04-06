package com.bizarrecoding.mustachecam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.bizarrecoding.mustachecam.GraphicOverlay.Graphic
import com.google.android.gms.vision.face.Face

/**
 * Created by Herik on 14/12/2015.
 */
private const val ID_TEXT_SIZE = 40.0f
private const val BOX_STROKE_WIDTH = 5.0f

class FaceGraphic(overlay: GraphicOverlay?, private val ctx: Context) :  Graphic(overlay) {

    private val mFacePositionPaint: Paint = Paint()
    private val mIdPaint: Paint = Paint()
    private val mBoxPaint: Paint = Paint()
    private val COLOR_CHOICES = intArrayOf(
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    )
    private var mCurrentColorIndex = 0

    @Volatile
    private var mFace: Face? = null

    init {
        mCurrentColorIndex =  (mCurrentColorIndex + 1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[mCurrentColorIndex]
        mFacePositionPaint.color = selectedColor
        mIdPaint.apply {
            color = selectedColor
            textSize = ID_TEXT_SIZE
        }
        mBoxPaint.apply {
            color = selectedColor
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE_WIDTH
        }
    }


    fun updateFace(face: Face?) {
        mFace = face
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        val face = mFace ?: return
        val x = translateX(face.position.x + face.width / 2)
        val y = translateY(face.position.y + face.height / 2)

        //MUSTACHE
        val xOffset = scaleX(face.width / 2.0f)
        val yOffset = scaleY(face.height / 2.0f)
        val left = (x - xOffset / 2).toInt()
        val top = (y + yOffset / 2).toInt() - 20
        val right = (x + xOffset / 2).toInt()
        val bottom = (y + yOffset * 3 / 4).toInt() - 20
        val d = ctx.resources.getDrawable(R.drawable.mustache,null)
        d.setBounds(left, top, right, bottom)
        d.draw(canvas)

    }
}