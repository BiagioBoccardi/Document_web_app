import { useEffect, useState } from "react";
import { useContextCast } from "@/context/context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { toast } from "sonner";
import { Users, Plus, Trash2, UserPlus, UserMinus, Crown, ChevronDown, ChevronUp, Search } from "lucide-react";


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

  // ── fetch all groups ──────────────────────────────────────────────────────
  // Moved to context

  // ── fetch users ───────────────────────────────────────────────────────────
  // Moved to context

  useEffect(() => {
    fetchGruppi();
    fetchUsers();
  }, []);

  // ── create group ──────────────────────────────────────────────────────────
  const handleCreate = async () => {
    if (!newGroupName.trim() || !currentUser) return;
    setCreating(true);
    try {
      await createGroup(newGroupName.trim(), currentUser.id, selectedUserIds);
      toast.success(`Gruppo "${newGroupName.trim()}" creato!`);
      setCreateOpen(false);
      setNewGroupName("");
      setSelectedUserIds([]);
    } catch (e) {
      console.error("Create group error:", e);

      if (e instanceof Error && e.message === "Failed to fetch") {
        toast.error("Errore di connessione. Controlla che il server backend sia acceso.");
      } else {
        toast.error((e instanceof Error ? e.message : null) || "Errore durante la creazione del gruppo.");
      }
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

  // ── add member ────────────────────────────────────────────────────────────
  const handleAddMember = async () => {
    if (!addMemberGruppoId || addMemberUserIds.length === 0) return;
    setAddingMember(true);
    try {
      await addMembersToGroup(addMemberGruppoId, addMemberUserIds);
      toast.success("Membri aggiunti!");
      setAddMemberOpen(false);
      setAddMemberUserIds([]);
    } catch {
      toast.error("Errore durante l'aggiunta del membro.");
    } finally {
      setAddingMember(false);
    }
  };

  // ── remove member ─────────────────────────────────────────────────────────
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

  // ── filtered groups ───────────────────────────────────────────────────────
  const filtered = gruppi.filter((g) =>
    g.name.toLowerCase().includes(search.toLowerCase())
  );

  // ── computed values for dialogs ───────────────────────────────────────────
  const groupToAddMemberTo = addMemberGruppoId ? gruppi.find(g => g.id === addMemberGruppoId) : null;
  const availableUsersForAdding = groupToAddMemberTo
    ? users.filter(u => !groupToAddMemberTo.membri.some(m => m.id === u.id))
    : [];
  const selectableUsers = users.filter(u => u.id !== currentUser?.id);

  // ─────────────────────────────────────────────────────────────────────────
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
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-16 rounded-lg border border-border bg-accent/30 animate-pulse" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border py-16 text-center">
            <Users size={36} className="mb-3 text-muted-foreground/40" />
            <p className="text-sm font-medium text-muted-foreground">
              {search ? "Nessun gruppo trovato." : "Nessun gruppo ancora. Creane uno!"}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map((gruppo) => {
              const isExpanded = expandedId === gruppo.id;
              const isOwner = currentUser?.id === gruppo.owner.id;

              return (
                <div
                  key={gruppo.id}
                  className="rounded-lg border border-border bg-card overflow-hidden transition-all"
                >
                  {/* Row */}
                  <div className="flex items-center gap-3 px-4 py-3">
                    {/* Expand toggle */}
                    <button
                      onClick={() => setExpandedId(isExpanded ? null : gruppo.id)}
                      className="flex flex-1 items-center gap-3 text-left min-w-0"
                    >
                      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary font-semibold text-sm">
                        {gruppo.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-foreground truncate">{gruppo.name}</p>
                        <p className="text-xs text-muted-foreground">
                          {gruppo.membri.length} membro{gruppo.membri.length !== 1 ? "i" : ""} · owner: {gruppo.owner.nome}
                        </p>
                      </div>
                    </button>

                    {/* Actions */}
                    <div className="flex items-center gap-1 shrink-0">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-muted-foreground hover:text-foreground"
                        title="Aggiungi membro"
                        onClick={() => {
                          setAddMemberGruppoId(gruppo.id);
                          setAddMemberOpen(true);
                        }}
                      >
                        <UserPlus size={14} />
                      </Button>
                      {isOwner && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7 text-muted-foreground hover:text-destructive"
                          title="Elimina gruppo"
                          onClick={() => {
                            if (window.confirm("Sei sicuro di voler eliminare questo gruppo?")) {
                              handleDelete(gruppo.id);
                            }
                          }}
                        >
                          <Trash2 size={14} />
                        </Button>
                      )}
                      <button
                        onClick={() => setExpandedId(isExpanded ? null : gruppo.id)}
                        className="flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
                      >
                        {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                      </button>
                    </div>
                  </div>

                  {/* Expanded: members list */}
                  {isExpanded && (
                    <div className="border-t border-border bg-accent/20 px-4 py-3">
                      <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                        Membri
                      </p>
                      <ul className="space-y-1.5">
                        {gruppo.membri.map((m) => (
                          <li key={m.id} className="flex items-center justify-between">
                            <div className="flex items-center gap-2 min-w-0">
                              <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground text-xs font-semibold">
                                {m.nome.charAt(0).toUpperCase()}
                              </div>
                              <span className="text-sm text-foreground truncate">{m.nome}</span>
                              {m.id === gruppo.owner.id && (
                                <Crown size={11} className="text-amber-500 shrink-0" />
                              )}
                            </div>
                            {/* Remove member – only owner can, and can't remove themselves */}
                            {isOwner && m.id !== gruppo.owner.id && (
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-6 w-6 text-muted-foreground hover:text-destructive"
                                title="Rimuovi membro"
                                onClick={() => handleRemoveMember(gruppo.id, m.id)}
                              >
                                <UserMinus size={13} />
                              </Button>
                            )}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ── Create group dialog ────────────────────────────────────────────── */}
      <Dialog open={createOpen} onOpenChange={(open) => {
        if (!open) {
          // Reset state on close
          setNewGroupName("");
          setSelectedUserIds([]);
        }
        setCreateOpen(open);
      }}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Nuovo gruppo</DialogTitle>
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

      {/* ── Add member dialog ──────────────────────────────────────────────── */}
      <Dialog open={addMemberOpen} onOpenChange={(open) => {
        if (!open) {
          // Reset state on close
          setAddMemberUserIds([]);
        }
        setAddMemberOpen(open);
      }}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Aggiungi membro</DialogTitle>
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
