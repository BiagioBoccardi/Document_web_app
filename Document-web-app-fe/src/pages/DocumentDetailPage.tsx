import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useContextCast } from "@/context/context";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Loader2, FileText, ArrowLeft, Edit } from "lucide-react";

const DocumentDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { fetchDocumentById, downloadDocument } = useContextCast();

    const [doc, setDoc] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const load = async () => {
            if (!id) return;

            try {
                const data = await fetchDocumentById(id);
                setDoc(data);
            } catch (e) {
                console.error(e);
            } finally {
                setLoading(false);
            }
        };

        load();
    }, [id]);

    if (loading) {
        return (
            <div className="flex justify-center items-center min-h-screen">
                <Loader2 className="w-8 h-8 animate-spin" />
            </div>
        );
    }

    if (!doc) {
        return (
            <div className="text-center mt-20">
                Documento non trovato
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto p-8">
            <Button variant="outline" onClick={() => navigate(-1)}>
                <ArrowLeft className="w-4 h-4 mr-2" />
                Indietro
            </Button>

            <Card className="mt-6 p-6">
                <div className="flex justify-between items-center mb-4">
                    <h1 className="text-2xl font-bold flex items-center gap-2">
                        <FileText /> {doc.filename}
                    </h1>

                    <div className="flex gap-2">
                        {/* PREVIEW */}
                        <Button onClick={() => navigate(`/documents/${id}/preview`)}>
                            Preview
                        </Button>

                        {/* DOWNLOAD (solo se il file esiste) */}
                        <Button onClick={() => {
                                if (!doc?.filename || !id) return;
                                downloadDocument(id, doc.filename);
                            }}>
                            Download
                        </Button>

                        {/* EDIT */}
                        <Button onClick={() => navigate(`/documents/${id}/edit`)}>
                            <Edit className="w-4 h-4 mr-2" />
                            Modifica
                        </Button>

                    </div>
                </div>

                <p className="text-gray-500 text-sm mb-4">
                    Upload: {new Date(doc.uploadDate).toLocaleString("it-IT")}
                </p>

                <div className="bg-gray-50 p-4 rounded-lg whitespace-pre-wrap">
                    {doc.content}
                </div>
            </Card>
        </div>
    );
};

export default DocumentDetailPage;