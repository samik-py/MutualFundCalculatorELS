import { useEffect, useLayoutEffect, useState, useCallback } from 'react'
import { useLocation } from 'react-router-dom'
import { useOnboarding, TOUR_STEPS } from './OnboardingContext'
import './OnboardingTour.css'

const TOOLTIP_WIDTH = 340
const TOOLTIP_HEIGHT_APPROX = 220
const PADDING = 16

function computeTooltipStyle(rect, position, vpW, vpH) {
  if (!rect) return {}
  const { top, left, width, height, bottom, right } = rect

  switch (position) {
    case 'bottom': {
      let l = left + width / 2 - TOOLTIP_WIDTH / 2
      l = Math.max(PADDING, Math.min(vpW - TOOLTIP_WIDTH - PADDING, l))
      return { top: bottom + PADDING, left: l }
    }
    case 'top': {
      let l = left + width / 2 - TOOLTIP_WIDTH / 2
      l = Math.max(PADDING, Math.min(vpW - TOOLTIP_WIDTH - PADDING, l))
      return { top: Math.max(PADDING, top - TOOLTIP_HEIGHT_APPROX - PADDING), left: l }
    }
    case 'left': {
      const t = top + height / 2 - TOOLTIP_HEIGHT_APPROX / 2
      return {
        top: Math.max(PADDING, Math.min(vpH - TOOLTIP_HEIGHT_APPROX - PADDING, t)),
        left: Math.max(PADDING, left - TOOLTIP_WIDTH - PADDING),
      }
    }
    case 'right': {
      const t = top + height / 2 - TOOLTIP_HEIGHT_APPROX / 2
      return {
        top: Math.max(PADDING, Math.min(vpH - TOOLTIP_HEIGHT_APPROX - PADDING, t)),
        left: Math.min(vpW - TOOLTIP_WIDTH - PADDING, right + PADDING),
      }
    }
    case 'right-overlap': {
      // Tooltip sits at the top-right of the box, left edge overlapping the right edge
      const l = right - 24
      const t = top + 16
      return {
        top: Math.max(PADDING, Math.min(vpH - TOOLTIP_HEIGHT_APPROX - PADDING, t)),
        left: Math.min(vpW - TOOLTIP_WIDTH - PADDING, l),
      }
    }
    default:
      return {}
  }
}

export default function OnboardingTour() {
  const { isActive, currentStep, stepIndex, totalSteps, next, prev, skip } = useOnboarding()
  const location = useLocation()

  const [targetRect, setTargetRect] = useState(null)
  const [ready, setReady] = useState(false)

  // Measure the current target element and store its viewport rect
  const measure = useCallback(() => {
    if (!currentStep?.target) return
    const el = document.querySelector(currentStep.target)
    if (el) setTargetRect(el.getBoundingClientRect())
  }, [currentStep])

  // On step / route change: find the element, scroll it into view instantly,
  // then measure after two animation frames (guarantees layout is settled)
  useLayoutEffect(() => {
    if (!isActive) return

    if (!currentStep?.target) {
      setTargetRect(null)
      setReady(true)
      return
    }

    setReady(false)
    let attempts = 0

    const tryFind = () => {
      const el = document.querySelector(currentStep.target)
      if (el) {
        // instant scroll so the element is in its final position before we measure
        el.scrollIntoView({ behavior: 'instant', block: 'center' })
        requestAnimationFrame(() => {
          requestAnimationFrame(() => {
            setTargetRect(el.getBoundingClientRect())
            setReady(true)
          })
        })
      } else if (attempts < 20) {
        attempts++
        setTimeout(tryFind, 100)
      } else {
        setTargetRect(null)
        setReady(true)
      }
    }

    tryFind()
  }, [isActive, stepIndex, location.pathname]) // eslint-disable-line react-hooks/exhaustive-deps

  // Keep the highlight aligned while the user scrolls or resizes
  useEffect(() => {
    if (!isActive || !currentStep?.target) return
    window.addEventListener('scroll', measure, { passive: true })
    window.addEventListener('resize', measure)
    return () => {
      window.removeEventListener('scroll', measure)
      window.removeEventListener('resize', measure)
    }
  }, [isActive, measure])

  if (!isActive || !currentStep || !ready) return null

  const isCentered = currentStep.position === 'center' || !targetRect
  const tooltipStyle = isCentered
    ? {}
    : computeTooltipStyle(targetRect, currentStep.position, window.innerWidth, window.innerHeight)

  const dots = TOUR_STEPS.map((_, i) => (
    <span
      key={i}
      className={`tour-dot${i === stepIndex ? ' tour-dot--active' : ''}${i < stepIndex ? ' tour-dot--done' : ''}`}
    />
  ))

  return (
    <>
      {isCentered ? (
        <div className="tour-overlay tour-overlay--dim" />
      ) : (
        <div
          className="tour-highlight"
          style={{
            top: targetRect.top - 6,
            left: targetRect.left - 6,
            width: targetRect.width + 12,
            height: targetRect.height + 12,
          }}
        />
      )}

      <div
        className={`tour-tooltip${isCentered ? ' tour-tooltip--center' : ''}`}
        style={isCentered ? {} : tooltipStyle}
      >
        <div className="tour-meta">
          <span className="tour-step-label">{stepIndex + 1} / {totalSteps}</span>
          <button className="tour-skip" onClick={skip}>Skip tour</button>
        </div>

        <h3 className="tour-title">{currentStep.title}</h3>
        <p className="tour-body">{currentStep.body}</p>

        <div className="tour-dots">{dots}</div>

        <div className="tour-nav">
          {stepIndex > 0 && (
            <button className="tour-btn tour-btn--secondary" onClick={prev}>Back</button>
          )}
          <button className="tour-btn tour-btn--primary" onClick={next}>
            {stepIndex === totalSteps - 1 ? 'Done' : 'Next'}
            {stepIndex < totalSteps - 1 && (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="tour-btn__icon">
                <path d="M5 12h14M12 5l7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            )}
          </button>
        </div>
      </div>
    </>
  )
}
