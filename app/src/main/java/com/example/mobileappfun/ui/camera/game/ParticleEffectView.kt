package com.example.mobileappfun.ui.camera.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti burst particle system. Spawns 60 particles with physics
 * (gravity, rotation, fade) on correct gesture match.
 */
class ParticleEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var size: Float,
        var color: Int,
        var alpha: Float,
        var life: Float, // 1.0 -> 0.0
        var decayRate: Float
    )

    companion object {
        private const val PARTICLE_COUNT = 60
        private const val GRAVITY = 800f // pixels/sec^2
        private val CONFETTI_COLORS = intArrayOf(
            Color.rgb(255, 215, 0),   // Gold
            Color.rgb(76, 175, 80),   // Green
            Color.rgb(33, 150, 243),  // Blue
            Color.rgb(255, 87, 34),   // Deep Orange
            Color.rgb(156, 39, 176),  // Purple
            Color.rgb(244, 67, 54),   // Red
            Color.rgb(0, 188, 212),   // Cyan
            Color.rgb(255, 193, 7)    // Amber
        )
    }

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastFrameTime = 0L
    private var isAnimating = false

    /**
     * Trigger a confetti burst centered at (cx, cy).
     */
    fun burst(cx: Float = width / 2f, cy: Float = height * 0.4f) {
        particles.clear()

        repeat(PARTICLE_COUNT) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 300f + Random.nextFloat() * 500f
            particles.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 200f, // bias upward
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                    size = 8f + Random.nextFloat() * 12f,
                    color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
                    alpha = 1f,
                    life = 1f,
                    decayRate = 0.3f + Random.nextFloat() * 0.4f
                )
            )
        }

        lastFrameTime = System.nanoTime()
        isAnimating = true
        invalidate()
    }

    fun clear() {
        particles.clear()
        isAnimating = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isAnimating || particles.isEmpty()) return

        val now = System.nanoTime()
        val dt = ((now - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameTime = now

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()

            // Update physics
            p.vy += GRAVITY * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.rotationSpeed * dt
            p.life -= p.decayRate * dt
            p.alpha = p.life.coerceIn(0f, 1f)

            if (p.life <= 0f || p.y > height + 50) {
                iterator.remove()
                continue
            }

            // Draw particle as a small rotated rectangle
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt()
            paint.style = Paint.Style.FILL

            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawRect(-p.size / 2, -p.size / 4, p.size / 2, p.size / 4, paint)
            canvas.restore()
        }

        if (particles.isNotEmpty()) {
            invalidate() // Continue animation loop
        } else {
            isAnimating = false
        }
    }
}
