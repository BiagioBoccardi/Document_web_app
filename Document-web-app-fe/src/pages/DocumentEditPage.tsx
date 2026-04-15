import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useContextCast } from "@/context/context";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

const DocumentEditPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { fetchDocumentById, updateDocument } = useContextCast();

    const [filename, setFilename] = useState("");
    const [content, setContent] = useState("");
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const load = async () => {
            if (!id) return;
            try {
                const data = await fetchDocumentById(id);
                const doc = data as { filename: string; content: string };
                setFilename(doc.filename);
                setContent(doc.content);
            } catch (_e) {
                toast.error("Errore caricamento documento");
            } finally {
                setLoading(false);
            }
        };
        load();
    }, [id, fetchDocumentById]);

    const handleSave = async () => {
        if (!id) return;
        setSaving(true);
        try {
            await updateDocument(id, filename, content);
            toast.success("Documento aggiornato");
            navigate(`/documents/${id}`);
        } catch (_e) {
            toast.error("Errore durante salvataggio");
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center min-h-screen">
                <Loader2 className="w-8 h-8 animate-spin" />
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto p-8">
            <Card className="p-6 space-y-4">
                <h1 className="text-2xl font-bold">Modifica Documento</h1>
                <Input
                    value={filename}
                    onChange={(e) => setFilename(e.target.value)}
                    placeholder="Nome file"
                />
                <Textarea
                    value={content}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value)}
                    rows={15}
                    placeholder="Contenuto documento..."
                />
                <div className="flex gap-4">
                    <Button onClick={handleSave} disabled={saving}>
                        {saving ? "Salvataggio..." : "Salva"}
                    </Button>
                    <Button variant="outline" onClick={() => navigate(-1)}>
                        Annulla
                    </Button>
                </div>
            </Card>
        </div>
    );
};

export default DocumentEditPage;