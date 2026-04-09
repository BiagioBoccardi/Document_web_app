import { useEffect, useState } from "react";
import { useContextCast } from "@/context/context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { toast } from "sonner";
import { Users, Plus, Trash2, UserPlus, UserMinus, Crown, ChevronDown, ChevronUp, Search, Loader2 } from "lucide-react";


export function GroupsPage() {
  const { currentUser, gruppi, users, loadingGruppi, fetchGruppi, fetchUsers, createGroup, deleteGroup, addMembersToGroup, removeMemberFromGroup } = useContextCast();

  // Create group dialog
  const [createOpen, setCreateOpen] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");
  const [creating, setCreating] = useState(false);
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);

  // Add member dialog
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [addMemberGruppoId, setAddMemberGruppoId] = useState<number | null>(null);
  const [addMemberUserIds, setAddMemberUserIds] = useState<number[]>([]);
  const [addingMember, setAddingMember] = useState(false);

  // Search
  const [search, setSearch] = useState("");
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    fetchGruppi();
    fetchUsers();
  }, [fetchGruppi, fetchUsers]);

  // ── create group ──────────────────────────────────────────────────────────
  const handleCreate = async () => {
    if (!newGroupName.trim() || !currentUser) return;
    setCreating(true);
    try {
      await createGroup(newGroupName.trim(), currentUser.id, selectedUserIds);
      toast.success(`Gruppo "${newGroupName.trim()}" creato con successo!`);
      setCreateOpen(false);
      setNewGroupName("");
      setSelectedUserIds([]);
    } catch (e) {
      console.error("Errore creazione:", e);
      toast.error(e instanceof Error ? e.message : "Impossibile creare il gruppo.");
    } finally {
      setCreating(false);
    }
  };

  // ── delete group ──────────────────────────────────────────────────────────
  const handleDelete = async (id: number) => {
    try {
      await deleteGroup(id);
      toast.success("Gruppo eliminato.");
    } catch {
      toast.error("Errore durante l'eliminazione.");
    }
  };

  // Funzione che permette l'aggiunta di nuovi membri
  const handleAddMember = async () => {
    if (!addMemberGruppoId || addMemberUserIds.length === 0) return;
    setAddingMember(true);
    try {
      await addMembersToGroup(addMemberGruppoId, addMemberUserIds);
      toast.success("Membri aggiornati correttamente!");
      setAddMemberOpen(false);
      setAddMemberUserIds([]);
    } catch (e) {
      toast.error("Errore durante l'aggiunta dei membri: " + e);
      console.error("Errore durante l'aggiunta dei membri: ", e);
    } finally {
      setAddingMember(false);
    }
  };

  // Funzione che permette la rimozione dei membri (tranne l'owner)
  const handleRemoveMember = async (gruppoId: number, userId: number) => {
    try {
      await removeMemberFromGroup(gruppoId, userId);
      toast.success("Membro rimosso.");
    } catch (e) {
      if (e instanceof Error && e.message === "Non puoi rimuovere il proprietario.") {
        toast.error("Non puoi rimuovere il proprietario.");
      } else {
        toast.error("Errore durante la rimozione.");
      }
    }
  };

  // Filtro ricerca
  const userGroups = gruppi.filter((g) =>
    g.owner.id === currentUser?.id || g.members.some(m => m.id === currentUser?.id)
  );
  const filtered = userGroups.filter((g) =>
    g.name.toLowerCase().includes(search.toLowerCase())
  );

  // -- Logica per utenti selezionabili --
  const groupToAddMemberTo = addMemberGruppoId ? gruppi.find(g => g.id === addMemberGruppoId) : null;
  const availableUsersForAdding = groupToAddMemberTo ? users.filter(u => !groupToAddMemberTo.members.some(m => m.id === u.id)) : [];
  const selectableUsers = users.filter(u => u.id !== currentUser?.id);

  return (
    <div className="min-h-screen bg-background pt-14">
      <div className="mx-auto max-w-3xl px-4 py-10">

        {/* Header */}
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground flex items-center gap-2">
              <Users size={22} />
              Gruppi
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Gestisci i tuoi gruppi e i relativi membri.
            </p>
          </div>
          <Button size="sm" onClick={() => setCreateOpen(true)} className="gap-1.5">
            <Plus size={15} />
            Nuovo gruppo
          </Button>
        </div>

        {/* Search */}
        <div className="relative mb-6">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="Cerca gruppo…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-8 h-9 text-sm"
          />
        </div>

        {/* List */}
        {loadingGruppi ? (
          <div className="flex flex-col items-center justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-primary/40" />
            <p className="mt-2 text-sm text-muted-foreground">Caricamento gruppi...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border py-20 text-center bg-accent/5">
            <Users size={40} className="mb-4 text-muted-foreground/20" />
            <p className="text-sm font-medium text-muted-foreground">
              {search ? "Nessun risultato per questa ricerca." : "Non fai ancora parte di alcun gruppo."}
            </p>
          </div>
        ) : (
          <div className="grid gap-3">
            {filtered.map((gruppo) => {
              const isExpanded = expandedId === gruppo.id;
              const isOwner = currentUser?.id === gruppo.owner.id;

              return (
                <div
                  key={gruppo.id}
                  className={`rounded-xl border transition-all duration-200 ${isExpanded ? 'border-primary/30 ring-1 ring-primary/10 shadow-sm' : 'border-border bg-card hover:border-primary/20'}`}
                >
                  <div className="flex items-center gap-3 px-4 py-3">
                    <button
                      onClick={() => setExpandedId(isExpanded ? null : gruppo.id)}
                      className="flex flex-1 items-center gap-4 text-left min-w-0"
                    >
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary font-bold">
                        {gruppo.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-foreground truncate">{gruppo.name}</p>
                        <p className="text-xs text-muted-foreground flex items-center gap-1.5">
                          <Users size={12} />
                          {gruppo.members.length} {gruppo.members.length === 1 ? "membro" : "membri"}
                          <span className="opacity-40">•</span>
                          owner: <span className="font-medium text-foreground/70">{gruppo.owner.nome}</span>
                        </p>
                      </div>
                    </button>

                    <div className="flex items-center gap-1.5">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 rounded-full"
                        onClick={() => {
                          setAddMemberGruppoId(gruppo.id);
                          setAddMemberOpen(true);
                        }}
                      >
                        <UserPlus size={15} />
                      </Button>

                      {isOwner && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 rounded-full text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                          onClick={() => {
                            if (window.confirm(`Vuoi davvero eliminare il gruppo "${gruppo.name}"?`)) {
                              handleDelete(gruppo.id);
                            }
                          }}
                        >
                          <Trash2 size={15} />
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 rounded-full"
                        onClick={() => setExpandedId(isExpanded ? null : gruppo.id)}
                      >
                        {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                      </Button>
                    </div>
                  </div>

                  {isExpanded && (
                    <div className="border-t border-border bg-accent/5 px-4 py-4 animate-in fade-in slide-in-from-top-1">
                      <p className="mb-3 text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
                        Lista Partecipanti
                      </p>
                      <div className="grid gap-2">
                        {gruppo.members.map((m) => (
                          <div key={m.id} className="flex items-center justify-between group">
                            <div className="flex items-center gap-3 min-w-0">
                              <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-background border border-border text-xs font-bold text-muted-foreground">
                                {m.nome.charAt(0).toUpperCase()}
                              </div>
                              <span className="text-sm font-medium truncate">{m.nome}</span>
                              {m.id === gruppo.owner.id && (
                                <Crown size={12} className="text-amber-500" />
                              )}
                            </div>

                            {isOwner && m.id !== gruppo.owner.id && (
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-destructive"
                                onClick={() => handleRemoveMember(gruppo.id, m.id)}
                              >
                                <UserMinus size={14} />
                              </Button>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Create group dialog */}
      <Dialog open={createOpen} onOpenChange={(open) => {
        if (!open) {
          setNewGroupName("");
          setSelectedUserIds([]);
        }
        setCreateOpen(open);
      }}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Nuovo gruppo</DialogTitle>
            <DialogDescription>Crea un nuovo gruppo con i suoi membri.</DialogDescription>
          </DialogHeader>
          <div className="py-2 space-y-4">
            <Input
              placeholder="Nome del gruppo"
              value={newGroupName}
              onChange={(e) => setNewGroupName(e.target.value)}
              autoFocus
            />

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Seleziona membri</label>
              <div className="h-40 overflow-y-auto border rounded-md p-1 space-y-1 bg-background">
                {selectableUsers.map((user) => (
                  <div
                    key={user.id}
                    className={`flex items-center p-2 rounded cursor-pointer transition-colors ${selectedUserIds.includes(user.id) ? 'bg-accent text-accent-foreground' : 'hover:bg-accent/50'}`}
                    onClick={() => setSelectedUserIds(prev => prev.includes(user.id) ? prev.filter(id => id !== user.id) : [...prev, user.id])}
                  >
                    <input
                      type="checkbox"
                      checked={selectedUserIds.includes(user.id)}
                      readOnly
                      className="mr-3 h-4 w-4 pointer-events-none"
                    />
                    <div className="flex flex-col">
                      <span className="text-sm font-medium">{user.nome}</span>
                      <span className={`text-xs ${selectedUserIds.includes(user.id) ? 'text-accent-foreground/80' : 'text-muted-foreground'}`}>{user.email}</span>
                    </div>
                  </div>
                ))}
                {selectableUsers.length === 0 && <p className="p-2 text-xs text-muted-foreground">Nessun utente disponibile.</p>}
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setCreateOpen(false)}>
              Annulla
            </Button>
            <Button onClick={handleCreate} disabled={!newGroupName.trim() || creating}>
              {creating ? "Creazione…" : "Crea"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Aggiunge i membri alla dialog*/}
      <Dialog open={addMemberOpen} onOpenChange={(open) => {
        if (!open) {
          setAddMemberUserIds([]);
        }
        setAddMemberOpen(open);
      }}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Aggiungi membro</DialogTitle>
            <DialogDescription>Seleziona i membri da aggiungere al gruppo.</DialogDescription>
          </DialogHeader>
          <div className="py-2">
            <div className="h-40 overflow-y-auto border rounded-md p-1 space-y-1 bg-background">
              {availableUsersForAdding.map((user) => (
                <div
                  key={user.id}
                  className={`flex items-center p-2 rounded cursor-pointer transition-colors ${addMemberUserIds.includes(user.id) ? 'bg-accent text-accent-foreground' : 'hover:bg-accent/50'}`}
                  onClick={() => setAddMemberUserIds(prev => prev.includes(user.id) ? prev.filter(id => id !== user.id) : [...prev, user.id])}
                >
                  <input
                    type="checkbox"
                    checked={addMemberUserIds.includes(user.id)}
                    readOnly
                    className="mr-3 h-4 w-4 pointer-events-none"
                  />
                  <div className="flex flex-col">
                    <span className="text-sm font-medium">{user.nome}</span>
                    <span className={`text-xs ${addMemberUserIds.includes(user.id) ? 'text-accent-foreground/80' : 'text-muted-foreground'}`}>{user.email}</span>
                  </div>
                </div>
              ))}
              {availableUsersForAdding.length === 0 && <p className="p-2 text-xs text-muted-foreground">Nessun utente da aggiungere.</p>}
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setAddMemberOpen(false)}>
              Annulla
            </Button>
            <Button onClick={handleAddMember} disabled={addMemberUserIds.length === 0 || addingMember}>
              {addingMember ? "Aggiunta…" : "Aggiungi"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
};

export default GroupsPage
