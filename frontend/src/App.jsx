import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './App.css'
import LandingPage from './pages/LandingPage'
import PredictorPage from './pages/PredictorPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/predictor" element={<PredictorPage />} />
      </Routes>
    </BrowserRouter>
  )
}
