import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './App.css'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import { OnboardingProvider } from './components/onboarding/OnboardingContext'
import OnboardingTour from './components/onboarding/OnboardingTour'
import ProtectedRoute from './components/ProtectedRoute'
import TickerTape from './components/TickerTape'
import LandingPage from './pages/LandingPage'
import PredictorPage from './pages/PredictorPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import PortfoliosPage from './pages/PortfoliosPage'
import PortfolioDetailPage from './pages/PortfolioDetailPage'
import PortfolioPage from './pages/PortfolioPage'
import CompareChartsPage from './pages/CompareChartsPage'
import SavedChartsPage from './pages/SavedChartsPage'
import DashboardPage from './pages/DashboardPage'
import CryptoSuitePage from './pages/CryptoSuitePage'
import ProfilePage from './pages/ProfilePage'

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <OnboardingProvider>
            <OnboardingTour />
          <TickerTape />
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/predictor" element={<PredictorPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/portfolio" element={<PortfolioPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/portfolios"
              element={
                <ProtectedRoute>
                  <PortfoliosPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/portfolios/:id"
              element={
                <ProtectedRoute>
                  <PortfolioDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/compare"
              element={
                <ProtectedRoute>
                  <CompareChartsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/charts"
              element={
                <ProtectedRoute>
                  <SavedChartsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/crypto"
              element={
                <ProtectedRoute>
                  <CryptoSuitePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
          </Routes>
          </OnboardingProvider>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
