import { useEffect, useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'

function App() {
  const [message, setMessage] = useState('Chargement...')

  useEffect(() => {
    fetch('http://localhost:8080/api/test')
      .then(res => res.text())
      .then(data => setMessage(data))
      .catch(() => setMessage('Impossible de contacter le backend'))
  }, [])

  return (
    <div style={{ fontFamily: 'system-ui', textAlign: 'center', marginTop: '60px' }}>
      <h1>Frontend React connecté au Backend Spring Boot</h1>
      <p style={{ fontSize: '18px', color: '#333' }}>{message}</p>
    </div>
  )
}

export default App
