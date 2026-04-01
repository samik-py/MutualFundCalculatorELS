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
    body: 'Select up to 5 funds from categorized lists and compare their projected growth trajectories side-by-side on an interactive chart. CAPM annual returns are shown for every selected fund.',
    position: 'right-overlap',
  },
  {
    id: 'portfolios',
    route: '/portfolios',
    target: '[data-tour="portfolios"]',
    title: 'My Portfolios',
    body: 'Create and manage custom portfolios. Add holdings with your cost basis and share count to track real positions — performance is calculated live against current fund values.',
    position: 'right',
  },
  {
    id: 'saved-charts',
    route: '/charts',
    target: '[data-tour="saved-charts"]',
    title: 'Saved Charts',
    body: 'Any fund comparison you generate can be saved here for quick access. Open a saved chart to reload the exact funds, amount, and time horizon — or share it via a direct link.',
    position: 'right',
  },
  {
    id: 'crypto-suite',
    route: '/crypto',
    target: '[data-tour="crypto-suite"]',
    title: 'Crypto Suite',
    body: 'A full cryptocurrency desk powered by Coinbase — live prices, tax lot optimization, stress testing across market scenarios, on-chain metrics, and AI-generated portfolio insights from Gemini.',
    position: 'right',
  },
  {
    id: 'finish',
    route: '/dashboard',
    target: null,
    title: "You're All Set",
    body: 'You now know every tool on the platform. Explore on your own — and if you ever want a refresher, visit your profile page to restart this walkthrough.',
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
