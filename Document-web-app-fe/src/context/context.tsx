
import type { IGruppo, INotification, IUser, IDocument } from "@/types";
import { createContext, useCallback, useContext, useEffect, useState } from "react";

import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

const API_URL = "http://localhost:80";
const DOCUMENT_SERVICE_URL = "http://localhost:8082";


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
  unreadCount: number;
  loadingNotifications: boolean;
  fetchNotifications: () => Promise<void>;
  markAsRead: (uuid: string) => Promise<void>;
  deleteNotification: (uuid: string) => Promise<void>;
  createGroup: (name: string, ownerId: number, memberIds: number[]) => Promise<void>;
  deleteGroup: (id: number) => Promise<void>;
  addMembersToGroup: (groupId: number, userIds: number[]) => Promise<void>;
  removeMemberFromGroup: (groupId: number, userId: number) => Promise<void>;
  documents: IDocument[];
  loadingDocuments: boolean;
  fetchDocuments: () => Promise<void>;
  fetchDocumentById: (id: string) => Promise<any>;
  downloadDocument: (id: string, filename: string) => Promise<void>;
  uploadDocument: (file: File) => Promise<void>;
  updateDocument: (id: string, filename: string, content: string) => Promise<void>;
  deleteDocument: (id: string) => Promise<void>;
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
  const [documents, setDocuments] = useState<IDocument[]>([]);
  const [loadingDocuments, setLoadingDocuments] = useState(false);
  const [gruppi, setGruppi] = useState<IGruppo[]>([]);
  const [users, setUsers] = useState<IUser[]>([]);
  const [loadingGruppi, setLoadingGruppi] = useState(false);
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
    if (!currentUser) {
      setLoadingGruppi(false);
      return;
    }

    if (loadingGruppi) return;
    setLoadingGruppi(true);
    try {
      const res = await fetch(`${API_URL}/api/v1/gruppi`, {
        headers: {
          "X-User-ID": currentUser.id.toString()
        }
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error("Errore nel recupero gruppi: " + errorText);
      }

      const data: IGruppo[] = await res.json();
      setGruppi(data);
    } catch (e) {
      console.error("Errore :", e);
      toast.error("Impossibile caricare i gruppi.");
    } finally {
      setLoadingGruppi(false);
    }
  }, [currentUser?.id]);

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

  const createGroup = async (name: string, ownerId: number, members: number[]) => {
    const res = await fetch(`${API_URL}/api/v1/gruppi`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: name.trim(),
        ownerId: ownerId,
        members: members 
      }),
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Errore server ${res.status}: ${errText}`);
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

  const fetchDocuments = useCallback(async () => {
      if (!currentUser || loadingDocuments) return;
      setLoadingDocuments(true);
      try {
          const token = localStorage.getItem("token");
          const headers: Record<string, string> = {
              "X-User-Id": currentUser.id.toString()
          };
          if (token) headers["Authorization"] = `Bearer ${token}`;

          const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/documents`, { headers });
          if (!res.ok) throw new Error("Errore nel recupero documenti");
          const data = await res.json();
          setDocuments(data);
      } catch (e) {
          console.error("Errore fetchDocuments:", e);
          toast.error("Impossibile caricare i documenti.");
      } finally {
          setLoadingDocuments(false);
      }
  }, [currentUser?.id]);

  const fetchDocumentById = useCallback(async (id: string) => {
      if (!currentUser) throw new Error("Utente non autenticato");

      const token = localStorage.getItem("token");
      const headers: Record<string, string> = {
          "X-User-Id": currentUser.id.toString()
      };
      if (token) headers["Authorization"] = `Bearer ${token}`;

      const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/documents/${id}`, {
          headers,
      });

      if (!res.ok) {
          throw new Error("Errore nel recupero documento");
      }

      return await res.json();
  }, [currentUser?.id]);

  const downloadDocument = useCallback(async (id: string, filename: string) => {
      if (!currentUser) throw new Error("Utente non autenticato");

      const token = localStorage.getItem("token");

      if (!token) throw new Error("Token mancante");

      const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/documents/${id}/download`, {
          method: "GET",
          headers: {
              "Authorization": `Bearer ${token ?? ""}`,
              "X-User-Id": currentUser.id.toString()
          }
      });

      if (!res.ok) {
          const msg = await res.text().catch(() => "");
          throw new Error(msg || "Errore download documento");
      }

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);

      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();

      window.URL.revokeObjectURL(url);
  }, [currentUser?.id]);

  const uploadDocument = useCallback(async (file: File) => {
      if (!currentUser) throw new Error("Utente non autenticato");

      const token = localStorage.getItem("token");
      const headers: Record<string, string> = {
          "X-User-Id": currentUser.id.toString()
      };
      if (token) headers["Authorization"] = `Bearer ${token}`;

      const formData = new FormData();
      formData.append("file", file);

      const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/documents`, {
          method: "POST",
          headers,
          body: formData,
      });

      if (!res.ok) {
          const error = await res.json().catch(() => ({}));
          throw new Error(error.message || `Errore upload: ${res.status}`);
      }

      await fetchDocuments();
  }, [currentUser?.id, fetchDocuments]);

  const updateDocument = useCallback(async (id: string, filename: string, content: string) => {
      if (!currentUser) throw new Error("Utente non autenticato");

      const token = localStorage.getItem("token");
      const headers: Record<string, string> = {
          "Content-Type": "application/json",
          "X-User-Id": currentUser.id.toString()
      };
      if (token) headers["Authorization"] = `Bearer ${token}`;

      const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/documents/${id}`, {
          method: "PUT",
          headers,
          body: JSON.stringify({ filename, content }),
      });

      if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || "Errore aggiornamento documento");
      }

      await fetchDocuments();
  }, [currentUser?.id, fetchDocuments]);

  const deleteDocument = useCallback(async (id: string) => {
      if (!currentUser) throw new Error("Utente non autenticato");

      const token = localStorage.getItem("token");
      const headers: Record<string, string> = {
          "X-User-Id": currentUser.id.toString()
      };
      if (token) headers["Authorization"] = `Bearer ${token}`;

      const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/documents/${id}`, {
          method: "DELETE",
          headers,
      });

      if (!res.ok && res.status !== 204) {
          throw new Error("Errore durante l'eliminazione");
      }

      setDocuments(prev => prev.filter(d => d.id !== id));
  }, [currentUser?.id]);

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
      toast.error("Errore caricamento notifiche!");
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
        fetchNotifications();
        fetchGruppi();
        fetchUsers();
        fetchDocuments();
    }
  }, [currentUser?.id, fetchNotifications, fetchGruppi, fetchUsers, fetchDocuments]);
    

  return (
    <Context.Provider value={{
    currentUser, fetchSignIn, createUser, signOut,
    gruppi, users, loadingGruppi,
    notifications, unreadCount, loadingNotifications,
    fetchNotifications, markAsRead, deleteNotification,
    fetchGruppi, fetchUsers,
    documents, loadingDocuments, fetchDocuments, fetchDocumentById, downloadDocument, uploadDocument, updateDocument, deleteDocument,
    createGroup, deleteGroup, addMembersToGroup, removeMemberFromGroup
  }}>
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
