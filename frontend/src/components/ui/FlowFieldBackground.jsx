import { useEffect, useRef } from 'react'

const PARTICLE_COUNT = 800
const SPEED = 0.8
const TRAIL_OPACITY = 0.04
const INTERACTION_RADIUS = 150
const GOLD = '#d1a153'
const CYAN = '#67e8f9'

class Particle {
  constructor(width, height, color) {
    this.width = width
    this.height = height
    this.color = color
    this.reset(true)
  }

  reset(randomizeAge = false) {
    this.x = Math.random() * this.width
    this.y = Math.random() * this.height
    this.vx = 0
    this.vy = 0
    this.life = Math.random() * 200 + 100
    this.age = randomizeAge ? Math.random() * this.life : 0
  }

  update(mouse) {
    // Flow field: angle from cosine/sine interference pattern
    const angle =
      (Math.cos(this.x * 0.005) + Math.sin(this.y * 0.005)) * Math.PI

    this.vx += Math.cos(angle) * 0.2 * SPEED
    this.vy += Math.sin(angle) * 0.2 * SPEED

    // Mouse repulsion
    const dx = mouse.x - this.x
    const dy = mouse.y - this.y
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (dist < INTERACTION_RADIUS && dist > 0) {
      const force = (INTERACTION_RADIUS - dist) / INTERACTION_RADIUS
      this.vx -= dx * force * 0.05
      this.vy -= dy * force * 0.05
    }

    // Apply velocity with friction
    this.x += this.vx
    this.y += this.vy
    this.vx *= 0.95
    this.vy *= 0.95

    this.age++
    if (this.age > this.life) this.reset()

    // Wrap around edges
    if (this.x < 0) this.x = this.width
    if (this.x > this.width) this.x = 0
    if (this.y < 0) this.y = this.height
    if (this.y > this.height) this.y = 0
  }

  draw(ctx) {
    // Fade in for first 15%, hold, fade out for last 15%
    const t = this.age / this.life
    let alpha
    if (t < 0.15) {
      alpha = t / 0.15
    } else if (t > 0.85) {
      alpha = (1 - t) / 0.15
    } else {
      alpha = 1
    }
    alpha = Math.max(0, Math.min(1, alpha)) * 0.9

    // Glow bloom: draw a faint 4×4 halo first, then a sharp 2×2 core on top
    ctx.globalAlpha = alpha * 0.25
    ctx.fillStyle = this.color
    ctx.fillRect(this.x - 1, this.y - 1, 4, 4)

    ctx.globalAlpha = alpha
    ctx.fillRect(this.x, this.y, 2, 2)
  }
}

export default function FlowFieldBackground() {
  const canvasRef = useRef(null)
  const containerRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const container = containerRef.current
    if (!canvas || !container) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let width = container.clientWidth
    let height = container.clientHeight
    let particles = []
    let animationId
    const mouse = { x: -9999, y: -9999 }

    const init = () => {
      const dpr = window.devicePixelRatio || 1
      width = container.clientWidth
      height = container.clientHeight
      canvas.width = width * dpr
      canvas.height = height * dpr
      canvas.style.width = `${width}px`
      canvas.style.height = `${height}px`
      ctx.scale(dpr, dpr)

      // Flood-fill with solid background so first frames aren't transparent
      ctx.globalAlpha = 1
      ctx.fillStyle = '#041024'
      ctx.fillRect(0, 0, width, height)

      particles = []
      for (let i = 0; i < PARTICLE_COUNT; i++) {
        // ~20% cyan, 80% gold
        const color = i < PARTICLE_COUNT * 0.2 ? CYAN : GOLD
        particles.push(new Particle(width, height, color))
      }
    }

    const animate = () => {
      // Trail effect: paint semi-transparent navy over canvas each frame
      ctx.fillStyle = `rgba(4, 16, 36, ${TRAIL_OPACITY})`
      ctx.fillRect(0, 0, width, height)

      for (const p of particles) {
        p.update(mouse)
        p.draw(ctx)
      }

      ctx.globalAlpha = 1
      animationId = requestAnimationFrame(animate)
    }

    const onResize = () => {
      cancelAnimationFrame(animationId)
      init()
      animate()
    }

    const onMouseMove = (e) => {
      const rect = canvas.getBoundingClientRect()
      mouse.x = e.clientX - rect.left
      mouse.y = e.clientY - rect.top
    }

    const onMouseLeave = () => {
      mouse.x = -9999
      mouse.y = -9999
    }

    init()
    animate()

    window.addEventListener('resize', onResize)
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('mouseleave', onMouseLeave)

    return () => {
      cancelAnimationFrame(animationId)
      window.removeEventListener('resize', onResize)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseleave', onMouseLeave)
    }
  }, [])

  return (
    <div
      ref={containerRef}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        zIndex: -1,
        overflow: 'hidden',
        background: '#041024',
      }}
    >
      <canvas ref={canvasRef} style={{ display: 'block' }} />
    </div>
  )
}
