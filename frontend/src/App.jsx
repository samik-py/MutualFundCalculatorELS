import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './App.css'
import LandingPage from './pages/LandingPage'
import PredictorPage from './pages/PredictorPage'
import ComparePage from './pages/ComparePage'
import PortfolioPage from './pages/PortfolioPage'
import Navbar from './components/Navbar'
import TickerTape from './components/TickerTape'
import { OnboardingProvider } from './components/onboarding/OnboardingContext'
import OnboardingTour from './components/onboarding/OnboardingTour'

export default function App() {
  return (
    <BrowserRouter>
      <OnboardingProvider>
        <Navbar />
        <TickerTape />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/predictor" element={<PredictorPage />} />
          <Route path="/compare" element={<ComparePage />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
        </Routes>
        <OnboardingTour />
      </OnboardingProvider>
    </BrowserRouter>
  )
}
