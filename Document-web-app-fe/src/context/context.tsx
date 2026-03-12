
import React, { useContext, useState, createContext } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "sonner";

// L'API gateway è esposto sulla porta 8080, che instrada al servizio corretto.
const API_URL = "http://127.0.0.1:8080";

interface User {
  id: number;
  nome: string;
  email: string;
  passwordHash: string;
  
}

interface ContextType {
  currentUser: User | null;
  fetchSignIn: (data: { email: string; passwordHash: string }) => Promise<void>;
  createUser: (data: Omit<User, "id">) => Promise<void>;
  signOut: () => void;
}

const Context = createContext<ContextType | undefined>(undefined);

export const ContextProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const navigate = useNavigate();

  const fetchSignIn = async (formData: { email: string; passwordHash: string }) => {
    try {
      const response = await fetch(`${API_URL}/api/v1/users/sign-in`, {
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
      
      setCurrentUser(data);
      localStorage.setItem("currentUser", JSON.stringify(data));
      if (data.token) {
        localStorage.setItem("token", data.token);
      }
      
      console.log("Login success:", data);
      toast.success("Login avvenuto con successo!");
      navigate("/");
    } catch (error) {
      console.error("Dettagli errore login:", error);
      toast.error("errore: " + error)
      
    }
  };

  const createUser = async (formData: Omit<User, "id">) => {
    try {
      const response = await fetch(`${API_URL}/api/v1/users/sign-up`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || "Registrazione fallita");
      }

      toast.success("Registrazione avvenuta con successo!");
      console.log("Registrazione success:", response);
      navigate("/signin");
    } catch (error) {
      toast.error("errore: " + error)
      console.error("Dettagli errore registrazione:", error);

      alert(
        `Errore registrazione: ${error instanceof Error ? error.message : "Errore sconosciuto"}.`,
      );
    }
  };

  const signOut = () => {
    setCurrentUser(null);
    localStorage.removeItem("currentUser");
    localStorage.removeItem("token");
    toast.success("Logout avvenuto con successo!");
    navigate("/signin");
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
