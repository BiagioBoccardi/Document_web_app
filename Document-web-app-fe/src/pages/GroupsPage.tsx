import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast"; // Assicurati di avere questo hook (shadcn standard)
import { useContextCast } from "@/context/context";
import { useNavigate } from "react-router-dom";
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Pencil, Trash2 } from "lucide-react";
import React, { useEffect } from "react";

// Dati di esempio, in un'applicazione reale arriverebbero da un'API
const mockGroups = [
  { id: 1, name: "Team Marketing", owner: "Mario Rossi" },
  { id: 2, name: "Sviluppatori Frontend", owner: "Luigi Verdi" },
  { id: 3, name: "Progetto Alpha", owner: "Anna Bianchi" },
];

export function GroupsPage() {
  const { currentUser } = useContextCast();
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    // 1. Se l'utente non è registrato (non loggato)
    if (!currentUser) {
      toast({
        variant: "destructive",
        title: "Accesso Negato",
        description: "Devi registrarti per accedere ai gruppi.",
      });
      navigate("/signup"); // Reindirizza alla registrazione come richiesto
      return;
    }

    // 2. Se l'utente è loggato ma non ha la mail corretta (proprietario)
    // Controlliamo se la mail finisce con @gmail.com (o la logica specifica che preferisci)
    if (!currentUser.email.endsWith("@gmail.com")) {
      toast({
        variant: "destructive",
        title: "Permesso Negato",
        description:
          "Solo i proprietari (Gmail) possono accedere a questa sezione.",
      });
      navigate("/"); // Reindirizza alla home
    }
  }, [currentUser, navigate, toast]);

  // Evita di mostrare il contenuto mentre avviene il reindirizzamento
  if (!currentUser || !currentUser.email.endsWith("@gmail.com")) return null;

  const handleEdit = (groupId: number) => {
    console.log(`Editing group ${groupId}`);
    // Qui andrebbe la logica per aprire un modale o navigare a una pagina di modifica
  };

  const handleDelete = (groupId: number) => {
    console.log(`Deleting group ${groupId}`);
    // Qui andrebbe la logica per mostrare un dialog di conferma e poi chiamare l'API
  };

  return (
    <div className="container mx-auto py-10">
      <h2 className="text-2xl font-bold tracking-tight mb-4">I tuoi Gruppi</h2>
      <div className="rounded-md border">
        <Table>
          <TableCaption></TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead>Proprietario</TableHead>
              <TableHead className="text-right">Azioni</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {mockGroups.map((group) => (
              <TableRow key={group.id}>
                <TableCell className="font-medium">{group.name}</TableCell>
                <TableCell>{group.owner}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() => handleEdit(group.id)}
                      aria-label={`Modifica gruppo ${group.name}`}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="destructive"
                      size="icon"
                      onClick={() => handleDelete(group.id)}
                      aria-label={`Elimina gruppo ${group.name}`}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
