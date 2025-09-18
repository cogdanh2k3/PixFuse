/*
import android.content.Context
import android.media.SoundPool
import android.media.AudioAttributes
import android.util.AttributeSet
import android.view.View

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var soundPool: SoundPool
    private var soundShoot = 0
    private var soundBounce = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(5)
            .build()

        // Load file âm thanh từ res/raw
        soundShoot = soundPool.load(context, R.raw.shoot, 1)
        soundBounce = soundPool.load(context, R.raw.bounce, 1)
    }

    private fun playSound(soundId: Int) {
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }
*/
