import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

const STORAGE_KEY = 'els_tour_v1_completed'

export const TOUR_STEPS = [
  {
    id: 'welcome',
    route: '/',
    target: null,
    title: 'Welcome to the ELS Fund Platform',
    body: 'A Goldman Sachs–grade mutual fund analysis suite built on real-time market data, CAPM projections, and AI-driven portfolio optimization. This tour will walk you through every tool in under two minutes.',
    position: 'center',
  },
  {
    id: 'predictor-form',
    route: '/predictor',
    target: '[data-tour="predictor-form"]',
    title: 'Investment Projector',
    body: 'Select any of 21 institutional mutual funds, enter your principal, and drag the time horizon slider. The engine computes your projected future value using live CAPM returns fetched from FRED and Yahoo Finance.',
    position: 'right',
  },
  {
    id: 'ground-truth',
    route: '/predictor',
    target: '[data-tour="ground-truth"]',
    title: 'Live Market Parameters',
    body: 'Every projection uses real data — the 10-Year Treasury yield from FRED and the S&P 500 5-Year CAGR from Yahoo Finance. These are the actual inputs to the CAPM engine, cached daily and auto-refreshed.',
    position: 'top',
  },
  {
    id: 'ai-optimizer',
    route: '/predictor',
    target: '[data-tour="ai-section"]',
    title: 'AI Portfolio Optimizer',
    body: 'Describe your investment goals in plain English — "aggressive growth over 20 years" or "conservative retirement income" — and the AI will return a personalized fund allocation with expected annual returns.',
    position: 'top',
  },
  {
    id: 'fund-compare',
    route: '/compare',
    target: '[data-tour="fund-compare"]',
    title: 'Fund Comparison',
    body: 'Select up to 10 funds from categorized lists and compare their projected growth trajectories side-by-side on an interactive chart. CAPM annual returns are shown for every selected fund.',
    position: 'right-overlap',
  },
  {
    id: 'monte-carlo',
    route: '/compare',
    target: '[data-tour="monte-carlo"]',
    title: 'Monte Carlo Simulation',
    body: 'Run 100–2000 Geometric Brownian Motion simulations (the same model behind Black-Scholes) to visualize a probability-weighted range of outcomes, from the 5th to the 95th percentile.',
    position: 'right-overlap',
  },
  {
    id: 'portfolio-builder',
    route: '/portfolio',
    target: '[data-tour="portfolio-builder"]',
    title: 'Portfolio Builder',
    body: 'Construct two custom portfolios by selecting funds and assigning allocation weights. Compare their projected growth head-to-head on a shared chart to evaluate different strategies.',
    position: 'right-overlap',
  },
  {
    id: 'finish',
    route: '/portfolio',
    target: null,
    title: "You're All Set",
    body: 'You now know every tool on the platform. Explore on your own — and if you ever want a refresher, click Tour in the navigation bar to restart this walkthrough.',
    position: 'center',
  },
]

const OnboardingContext = createContext(null)

export function OnboardingProvider({ children }) {
  const [stepIndex, setStepIndex] = useState(0)
  const [isActive, setIsActive] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const done = localStorage.getItem(STORAGE_KEY)
    if (!done) {
      // Small delay so the landing page has time to mount
      const t = setTimeout(() => setIsActive(true), 600)
      return () => clearTimeout(t)
    }
  }, [])

  const currentStep = TOUR_STEPS[stepIndex]

  const next = useCallback(() => {
    if (stepIndex >= TOUR_STEPS.length - 1) {
      setIsActive(false)
      localStorage.setItem(STORAGE_KEY, 'true')
      return
    }
    const nextStep = TOUR_STEPS[stepIndex + 1]
    if (nextStep.route !== currentStep.route) {
      navigate(nextStep.route)
    }
    setStepIndex((i) => i + 1)
  }, [stepIndex, currentStep, navigate])

  const prev = useCallback(() => {
    if (stepIndex === 0) return
    const prevStep = TOUR_STEPS[stepIndex - 1]
    if (prevStep.route !== currentStep.route) {
      navigate(prevStep.route)
    }
    setStepIndex((i) => i - 1)
  }, [stepIndex, currentStep, navigate])

  const skip = useCallback(() => {
    setIsActive(false)
    localStorage.setItem(STORAGE_KEY, 'true')
  }, [])

  const restart = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setStepIndex(0)
    navigate('/')
    // Slight delay so navigation completes before activating
    setTimeout(() => setIsActive(true), 100)
  }, [navigate])

  return (
    <OnboardingContext.Provider
      value={{
        isActive,
        currentStep,
        stepIndex,
        totalSteps: TOUR_STEPS.length,
        next,
        prev,
        skip,
        restart,
      }}
    >
      {children}
    </OnboardingContext.Provider>
  )
}

export function useOnboarding() {
  return useContext(OnboardingContext)
}
