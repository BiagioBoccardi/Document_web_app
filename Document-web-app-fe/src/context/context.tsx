import type { IUser } from "@/types";
import { useContext, useEffect, useState, createContext } from "react";
import { useNavigate } from "react-router";

const API_URL = import.meta.env.VITE_API_URL;

interface UserPayload {
    nome: string;
    email: string;
    password: string;
    isAdmin: boolean;
}

interface SingInPayload {
    email: string;
    password: string;
}


interface ContextType {
    users: IUser[];
    currentUser: IUser | null;
    fetchSignIn: (payload: SingInPayload) => Promise<void>;
    fetchSignOut: () => void;
    createUser: (payload: UserPayload) => Promise<void>;
    fetchUsers: () => void;
}

const UserContext = createContext<ContextType | null>(null);

export function ContextProvider({ children }: { children: React.ReactNode }) {
    const [users, setUsers] = useState<IUser[]>([]);
    const [currentUser, setCurrentUser] = useState<IUser | null>(null);
    const navigate = useNavigate();

    const fetchUsers = async () => {
        try {
            const res = await fetch(`${API_URL}/api/v1/users`);
            if (!res.ok) throw new Error();
            const data = await res.json();
            setUsers(data);
        } catch (error) {
            console.error("Errore caricamento users!", error);
        }
    };

    const fetchSignIn = async (credentials: SingInPayload) => {
        try {
            const res = await fetch(`${API_URL}/api/v1/sign-in`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(credentials),
            });

            if (!res.ok) {
                const errorData = await res.json();
                throw new Error(errorData.message || "Login fallito");
            }

            const data = await res.json(); 
            setCurrentUser(data.user);
            navigate("/homepage");

        } catch (error) {
            console.error("Errore login:", error);
            throw error;
        }
    };

    const fetchSignOut = () => {
        localStorage.removeItem("token");
        setCurrentUser(null);
        navigate("/sign-in");
    };

    const createUser = async (payload: UserPayload) => {
        try {
            const res = await fetch(`${API_URL}/api/v1/sign-up`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
      
            if (!res.ok) {
                const error = await res.text();
                throw new Error(error);
            }

        } catch (err) {
            console.error(err);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    return (
        <UserContext.Provider value={{ users, currentUser, fetchSignIn, fetchSignOut, createUser, fetchUsers }}>
            {children}
        </UserContext.Provider>
    );

}
export function useContextCast() {
    const ctx = useContext(UserContext);
    if (!ctx) {
        throw new Error("useContextCast must be used within a UserProvider");
    }
    return ctx;
}