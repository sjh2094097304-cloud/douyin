class ProjectionFrameProvider {

    @Volatile
    private var latest: Bitmap? = null

    fun submitFrame(
        bitmap: Bitmap
    ) {

        val old = latest

        latest = bitmap

        if (
            old != null &&
            old !== bitmap &&
            !old.isRecycled
        ) {
            old.recycle()
        }
    }

    fun latestFrame(): Bitmap? {
        return latest
    }

    fun clear() {
        latest?.recycle()
        latest = null
    }
}