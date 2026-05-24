package com.mireru.app.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCropper @Inject constructor() {

    /**
     * 表示座標系での選択矩形をビットマップ座標系に変換して切り抜く
     *
     * @param bitmap     元画像
     * @param imageRect  画面上での画像表示領域（px）
     * @param selectRect 画面上での選択領域（px）
     */
    fun crop(
        bitmap: Bitmap,
        imageRect: RectF,
        selectRect: RectF
    ): Bitmap? {
        // 表示スケール
        val scaleX = bitmap.width / imageRect.width()
        val scaleY = bitmap.height / imageRect.height()

        // 画像内の絶対座標に変換
        val left   = ((selectRect.left   - imageRect.left) * scaleX).toInt()
        val top    = ((selectRect.top    - imageRect.top)  * scaleY).toInt()
        val right  = ((selectRect.right  - imageRect.left) * scaleX).toInt()
        val bottom = ((selectRect.bottom - imageRect.top)  * scaleY).toInt()

        val safeRect = Rect(
            left.coerceIn(0, bitmap.width  - 1),
            top.coerceIn(0,  bitmap.height - 1),
            right.coerceIn(1, bitmap.width),
            bottom.coerceIn(1, bitmap.height)
        )
        if (safeRect.width() < 4 || safeRect.height() < 4) return null

        return Bitmap.createBitmap(
            bitmap,
            safeRect.left, safeRect.top,
            safeRect.width(), safeRect.height()
        )
    }
}
