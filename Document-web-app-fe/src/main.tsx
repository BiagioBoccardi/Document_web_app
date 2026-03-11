import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { BrowserRouter } from 'react-router-dom'
import { ContextProvider } from './context/context.tsx'
import { Toaster } from './components/ui/sonner.tsx'

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <StrictMode>
      <ContextProvider>
        <App />
        <Toaster />
      </ContextProvider>
    </StrictMode>
  </BrowserRouter>
)
