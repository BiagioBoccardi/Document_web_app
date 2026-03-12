import React, { useContext, useState, createContext } from "react";
import { useNavigate, useLocation } from "react-router-dom";

// L'API gateway è esposto sulla porta 8080, che instrada al servizio corretto.
const API_URL = "http://127.0.0.1:8081";



interface User {
  id: number;
  nome: string;
  email: string;
  isAdmin: boolean;
}

interface ContextType {
  currentUser: User | null;
  fetchSignIn: (data: { email: string; password: string }) => Promise<void>;
  createUser: (data: Omit<User, "id">) => Promise<void>;
  signOut: () => void;
}

const Context = createContext<ContextType | undefined>(undefined);

export const ContextProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [currentUser, setCurrentUser] = useState<User | null>(() => {



    try {
      const stored = localStorage.getItem("currentUser");
      if (!stored || stored === "undefined") return null;
      return JSON.parse(stored);
    } catch {
      localStorage.removeItem("currentUser");
      return null;
    }
  });
  const navigate = useNavigate();
  const location = useLocation();

  const fetchSignIn = async (formData: { email: string; password: string }) => {
    try {
      const response = await fetch(`${API_URL}/api/v1/sign-in`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error("Login failed");
      }

      const data = await response.json();
      console.log("Login success:", data);

      setCurrentUser(data.user);
      if (data.token) {
        localStorage.setItem("token", data.token);
      }

      // Reindirizza alla pagina di origine se esiste (salvata in ProtectedRoute), altrimenti alla home
      const origin = location.state?.from?.pathname || "/";
      navigate(origin);
    } catch (error) {
      console.error("Dettagli errore login:", error);
      // Mostra l'errore specifico invece di quello generico
      alert(
        `Errore login: ${error instanceof Error ? error.message : "Errore sconosciuto"}. Controlla che il backend sia avviato sulla porta 8080.`,
      );
    }
  };

  const createUser = async (formData: Omit<User, "id">) => {
    try {
      const response = await fetch(`${API_URL}/api/v1/sign-up`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Registrazione fallita");
      }

      alert(
        "Registrazione avvenuta con successo! Ora puoi effettuare il login.",
      );
      navigate("/login");
    } catch (error) {
      console.error("Dettagli errore registrazione:", error);
      alert(
        `Errore registrazione: ${error instanceof Error ? error.message : "Errore sconosciuto"}.`,
      );
    }
  };

  const signOut = () => {
    setCurrentUser(null);
    localStorage.removeItem("token");
    navigate("/login");
  };

  return (
    <Context.Provider value={{ currentUser, fetchSignIn, createUser, signOut }}>
      {children}
    </Context.Provider>
  );
};

export const useContextCast = () => {
  const context = useContext(Context);
  if (context === undefined) {
    throw new Error("useContextCast must be used within a ContextProvider");
  }
  return context;
};
