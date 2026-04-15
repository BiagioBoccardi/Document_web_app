import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useContextCast } from "@/context/context";
import { ArrowLeft } from "lucide-react";

interface DocumentDetail {
  id: string;
  filename: string;
  uploadDate: string;
  content: string;
}

export default function DocumentPreviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { fetchDocumentById, downloadDocument } = useContextCast();

  const [url, setUrl] = useState<string | null>(null);
  const [doc, setDoc] = useState<DocumentDetail | null>(null);

  const isPreviewable = (filename: string) => {
    return filename.endsWith(".pdf") || filename.endsWith(".txt");
  };

  useEffect(() => {
    const load = async () => {
      if (!id) return;
      try {
        const data = await fetchDocumentById(id);
        setDoc(data as DocumentDetail);

        if (!isPreviewable((data as DocumentDetail).filename)) return;

        const token = localStorage.getItem("token");
        const res = await fetch(
          `http://localhost:8082/api/v1/documents/${id}/download`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        const blob = await res.blob();
        setUrl(URL.createObjectURL(blob));
      } catch (e) {
        console.error(e);
      }
    };
    load();
  }, [id, fetchDocumentById]);

  if (!doc) return <p>Caricamento...</p>;

  if (!isPreviewable(doc.filename)) {
    return (
      <div className="p-8">
        <p className="mb-4">Preview non disponibile per questo tipo di file</p>
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />Indietro
        </Button>
        <Button onClick={() => downloadDocument(id!, doc.filename)}>
          Scarica file
        </Button>
      </div>
    );
  }

  if (!url) return <p>Caricamento preview...</p>;

  return (
    <div className="w-full h-screen flex flex-col">
      <div className="p-2 border-b flex gap-2">
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />Indietro
        </Button>
        <Button onClick={() => downloadDocument(id!, doc.filename)}>
          Scarica file
        </Button>
      </div>
      <iframe src={url} className="flex-1 w-full" />
    </div>
  );
}