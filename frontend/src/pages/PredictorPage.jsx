import { useEffect } from 'react'
import Navbar from '../components/Navbar'
import TickerTape from '../components/TickerTape'
import PredictorForm from '../components/PredictorForm'
import AISection from '../components/AISection'

function useScrollReveal() {
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible')
          }
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
    )

    const elements = document.querySelectorAll('.reveal')
    elements.forEach((el) => observer.observe(el))

    return () => observer.disconnect()
  }, [])
}

export default function PredictorPage() {
  useScrollReveal()

  return (
    <>
      <Navbar />
      <TickerTape />
      <main>
        <PredictorForm />
        <AISection />
      </main>
    </>
  )
}
