// src/components/protected-route.tsx
import { Navigate, Outlet } from 'react-router-dom';

export function ProtectedRoute() {
  // Controlliamo se esiste il token. 
  // Nota: se salvi il token nel context, puoi usare il tuo custom hook qui (es. const { token } = useAppContext())
  const token = localStorage.getItem('token'); 

  if (!token) {
    // Se non c'è il token, rimanda l'utente alla homepage (o alla rotta di login)
    return <Navigate to="/" replace />;
  }

  // Se il token c'è, renderizza la rotta figlia
  return <Outlet />;
}