
import type { IGruppo, INotification, IUser } from "@/types";
import { createContext, useCallback, useContext, useEffect, useState } from "react";

import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

const API_URL = "http://localhost:80";

interface ContextType {
  currentUser: IUser | null;
  fetchSignIn: (data: { email: string; passwordHash: string }) => Promise<void>;
  createUser: (data: Omit<IUser, "id">) => Promise<void>;
  signOut: () => void;
  gruppi: IGruppo[];
  users: IUser[];
  notifications: INotification[]
  loadingGruppi: boolean;
  fetchGruppi: () => Promise<void>;
  fetchUsers: () => Promise<void>;
  fetchDocuments: () => Promise<void>;
  unreadCount: number;
  loadingNotifications: boolean;
  fetchNotifications: () => Promise<void>;
  markAsRead: (uuid: string) => Promise<void>;
  deleteNotification: (uuid: string) => Promise<void>;
  createGroup: (name: string, ownerId: number, memberIds: number[]) => Promise<void>;
  deleteGroup: (id: number) => Promise<void>;
  addMembersToGroup: (groupId: number, userIds: number[]) => Promise<void>;
  removeMemberFromGroup: (groupId: number, userId: number) => Promise<void>;
}

const Context = createContext<ContextType | undefined>(undefined);

export const ContextProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [currentUser, setCurrentUser] = useState<IUser | null>(() => {
    try {
      const stored = localStorage.getItem("currentUser");
      if (!stored || stored === "undefined") return null;
      return JSON.parse(stored);
    } catch {
      localStorage.removeItem("currentUser");
      return null;
    }
  });
  const [gruppi, setGruppi] = useState<IGruppo[]>([]);
  const [users, setUsers] = useState<IUser[]>([]);
  const [loadingGruppi, setLoadingGruppi] = useState(true);
  const [notifications, setNotifications] = useState<INotification[]>([]);
  const [loadingNotifications, setLoadingNotifications] = useState(false);
  const unreadCount = notifications.filter(n => n.stato !== 'READ').length;
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

      setCurrentUser(data.user);
      localStorage.setItem("currentUser", JSON.stringify(data.user));

      if (data.token) {
        localStorage.setItem("token", data.token);
      }

      toast.success("Login avvenuto con successo!");
      navigate("/");
    } catch (error) {
      console.error("Dettagli errore login:", error);
      toast.error("errore: " + error)

    }
  };

  const createUser = async (formData: Omit<IUser, "id">) => {
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
    }
  };

  const signOut = () => {
    setCurrentUser(null);
    localStorage.removeItem("currentUser");
    localStorage.removeItem("token");
    toast.success("Logout avvenuto con successo!");
    navigate("/signin");
  };

  const fetchGruppi = useCallback(async () => {
    if (!currentUser || loadingGruppi) return;

    setLoadingGruppi(true);
    try {
      const res = await fetch(`${API_URL}/api/v1/gruppi`, {
        headers: {
          "X-User-ID": currentUser.id.toString()
        }
      });
      if (!res.ok) throw new Error();
      const data: IGruppo[] = await res.json();
      setGruppi(data);
    } catch {
      toast.error("Impossibile caricare i gruppi.");
    } finally {
      setLoadingGruppi(false);
    }
  }, []);

  const fetchUsers = useCallback(async () => {
    try {
      const res = await fetch(`${API_URL}/api/v1/users`);
      if (res.ok) {
        const data = await res.json();
        setUsers(data);
      } else {
        setUsers([]);
      }
    } catch {
      console.error("Errore nel caricamento utenti");
      setUsers([]);
    }
  }, []);

  const createGroup = async (name: string, ownerId: number, memberIds: number[]) => {
    const res = await fetch(`${API_URL}/api/v1/gruppi`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: name.trim(),
        owner: { id: ownerId }
      }),
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Errore server ${res.status}: ${errText}`);
    }

    const newGroup: IGruppo = await res.json();

    // Aggiunge i membri selezionati
    if (memberIds.length > 0 && newGroup?.id) {
      await Promise.all(memberIds.map(uid =>
        fetch(`${API_URL}/api/v1/gruppi/${newGroup.id}/membri`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ userId: uid }),
        })
      ));
    }

    await fetchGruppi();
  };

  const deleteGroup = async (id: number) => {
    const res = await fetch(`${API_URL}/api/v1/gruppi/${id}`, 
      { method: "DELETE" });
    if (!res.ok) throw new Error();
    await fetchGruppi();
  };

  const addMembersToGroup = async (groupId: number, userIds: number[]) => {
    await Promise.all(userIds.map(uid =>
      fetch(`${API_URL}/api/v1/gruppi/${groupId}/membri`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: uid }),
      })
    ));
    await fetchGruppi();
  };

  const removeMemberFromGroup = async (groupId: number, userId: number) => {
    const res = await fetch(`${API_URL}/api/v1/gruppi/${groupId}/membri`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId }),
    });
    if (res.status === 409) throw new Error("Non puoi rimuovere il proprietario.");
    if (!res.ok) throw new Error();
    await fetchGruppi();
  };

  const fetchDocuments = async () => {
    const token = localStorage.getItem("token");
    const res = await fetch(`${API_URL}/api/v1/documents`, {
      headers: {
        "Authorization": `Bearer ${token}`,
        "X-User-ID": currentUser?.id.toString() || ""
      }
    });
    if (!res.ok) throw new Error;
    const data = await res.json();
    return data;
  };

  const fetchNotifications = useCallback(async () => {
    if (!currentUser?.id || loadingNotifications) return;
    setLoadingNotifications(true);
    try {
      const res = await fetch(`${API_URL}/api/v1/notifications`, {
        headers: { "X-User-ID": String(currentUser.id) }
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
      setNotifications(data);
    } catch (error) {
      console.error("Errore caricamento notifiche: ", error);
      toast.error("Errore caricamento notifiche: " + error);
    } finally {
      setLoadingNotifications(false);
    }
  }, [currentUser?.id]);

  const markAsRead = async (uuid: string) => {
    if (!currentUser) return;
    try {
      const res = await fetch(`${API_URL}/api/v1/notifications/${uuid}/read`, {
        method: "PUT",
        headers: { "X-User-ID": currentUser.id.toString() }
      });
      if (!res.ok) throw new Error();
      
      // Aggiorna lo stato locale senza rifare la fetch
      setNotifications(prev => prev.map(n => 
        n.uuid === uuid ? { ...n, stato: 'READ' as const, readAt: new Date().toISOString() } : n
      ));
    } catch (error) {
      toast.error("Errore nel segnare la notifica come letta: " + error);
    }
  };

  const deleteNotification = async (uuid: string) => {
    if (!currentUser) return;
    try {
      const res = await fetch(`${API_URL}/api/v1/notifications/${uuid}`, {
        method: "DELETE",
        headers: { "X-User-ID": currentUser.id.toString() }
      });
      if (!res.ok) throw new Error();
      
      setNotifications(prev => prev.filter(n => n.uuid !== uuid));
      toast.success("Notifica eliminata.");
    } catch (error) {
      toast.error("Errore durante l'eliminazione: " + error);
    }
  };

  useEffect(() => {
    const userId = currentUser?.id;
    
    if (userId && userId !== 0) {
      console.log("Inizializzazione dati per utente:", userId);
      fetchNotifications();
      fetchGruppi();
      fetchUsers();
    }
  }, [currentUser?.id, fetchNotifications, fetchGruppi, fetchUsers]);
    

  return (
    <Context.Provider value={{ currentUser, fetchSignIn, createUser, signOut, gruppi, users, loadingGruppi, notifications, unreadCount, loadingNotifications, fetchNotifications, markAsRead, deleteNotification, fetchGruppi, fetchUsers, fetchDocuments, createGroup, deleteGroup, addMembersToGroup, removeMemberFromGroup }}>
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
