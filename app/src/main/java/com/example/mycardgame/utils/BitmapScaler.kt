package com.example.mycardgame.utils

import android.graphics.Bitmap

object BitmapScaler {

    fun scaleToFitWidth(b:Bitmap,width: Int):Bitmap{
        val factor:Float = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b,width,(b.height * factor).toInt(),true)
    }

    fun scaleToFitHeight(b:Bitmap,height:Int): Bitmap {
        val factor: Float = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b,(b.width * factor).toInt(),height,true)
    }


}

