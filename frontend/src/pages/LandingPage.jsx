import { useEffect } from 'react'
import Navbar from '../components/Navbar'
import TickerTape from '../components/TickerTape'
import HeroSection from '../components/HeroSection'

function useScrollReveal() {
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) entry.target.classList.add('visible')
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
    )
    document.querySelectorAll('.reveal').forEach((el) => observer.observe(el))
    return () => observer.disconnect()
  }, [])
}

export default function LandingPage() {
  useScrollReveal()

  return (
    <>
      <Navbar />
      <TickerTape />
      <main>
        <HeroSection />
      </main>
    </>
  )
}
