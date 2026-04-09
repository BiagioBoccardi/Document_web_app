    import { useState } from "react";
    import { useContextCast } from "@/context/context";
    import { Card } from "@/components/ui/card";
    import { Button } from "@/components/ui/button";
    import { toast } from "sonner";
    import { Upload, File, AlertCircle, LogIn, UserPlus, Loader2, Trash2 } from "lucide-react";
    import { useNavigate } from "react-router-dom";

    const ALLOWED_EXTENSIONS = ["txt", "pdf", "doc", "docx", "xls", "xlsx"];
    const MAX_SIZE_BYTES = 10 * 1024 * 1024;

    const Homepage = () => {
        const { currentUser, documents, loadingDocuments, uploadDocument, deleteDocument } = useContextCast();
        const navigate = useNavigate();
        const [isDragOver, setIsDragOver] = useState(false);
        const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
        const [uploading, setUploading] = useState(false);

        const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
            e.preventDefault();
            e.stopPropagation();
            setIsDragOver(true);
        };

        const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
            e.preventDefault();
            e.stopPropagation();
            setIsDragOver(false);
        };

        const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
            e.preventDefault();
            e.stopPropagation();
            setIsDragOver(false);
            handleFiles(Array.from(e.dataTransfer.files));
        };

        const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
            handleFiles(Array.from(e.target.files || []));
        };

        const handleFiles = (files: File[]) => {
            const valid: File[] = [];
            const invalid: string[] = [];

            for (const file of files) {
                const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
                if (!ALLOWED_EXTENSIONS.includes(ext)) {
                    invalid.push(`${file.name} (estensione non supportata)`);
                } else if (file.size > MAX_SIZE_BYTES) {
                    invalid.push(`${file.name} (supera 10MB)`);
                } else {
                    valid.push(file);
                }
            }

            if (invalid.length > 0) toast.error(`File non accettati: ${invalid.join(", ")}`);
            if (valid.length > 0) {
                setSelectedFiles(prev => [...prev, ...valid]);
                toast.info(`${valid.length} file selezionato/i`);
            }
        };

        const handleUpload = async () => {
            if (selectedFiles.length === 0) {
                toast.error("Seleziona almeno un file");
                return;
            }

            setUploading(true);
            let successCount = 0;
            let errorCount = 0;

            for (const file of selectedFiles) {
                try {
                    await uploadDocument(file);
                    successCount++;
                } catch (e) {
                    errorCount++;
                    toast.error(`Errore ${file.name}: ${e instanceof Error ? e.message : "errore sconosciuto"}`);
                }
            }

            if (successCount > 0) {
                toast.success(`${successCount} file caricato/i con successo`);
                setSelectedFiles([]);
            }
            if (errorCount > 0) toast.error(`${errorCount} file non caricato/i`);
            setUploading(false);
        };

        const handleDelete = async (id: string, filename: string) => {
            try {
                await deleteDocument(id);
                toast.success(`"${filename}" eliminato`);
            } catch {
                toast.error("Errore durante l'eliminazione");
            }
        };

        const removeSelected = (index: number) => {
            setSelectedFiles(prev => prev.filter((_, i) => i !== index));
        };

        return !currentUser ? (
            <div className="flex items-center justify-center min-h-screen bg-linear-to-b from-gray-50 to-gray-100">
                <Card className="p-12 text-center max-w-2xl">
                    <AlertCircle className="w-16 h-16 mx-auto mb-6 text-orange-500" />
                    <h1 className="text-3xl font-bold mb-2">Utente Non Loggato</h1>
                    <p className="text-gray-600 mb-6 text-lg">
                        Accedi al tuo account o registrati per accedere ai tuoi documenti.
                    </p>
                    <div className="bg-gray-50 p-6 rounded-lg mb-8 text-left">
                        <h2 className="font-semibold text-gray-900 mb-3">Funzionalità disponibili dopo il login:</h2>
                        <ul className="space-y-2 text-gray-700 text-sm">
                            <li className="flex items-center gap-2">
                                <span className="w-2 h-2 bg-blue-600 rounded-full"></span>
                                Caricamento e gestione documenti
                            </li>
                            <li className="flex items-center gap-2">
                                <span className="w-2 h-2 bg-blue-600 rounded-full"></span>
                                Sincronizzazione file nel cloud
                            </li>
                            <li className="flex items-center gap-2">
                                <span className="w-2 h-2 bg-blue-600 rounded-full"></span>
                                Accesso ai tuoi dati personali
                            </li>
                        </ul>
                    </div>
                    <div className="flex flex-col sm:flex-row gap-4">
                        <Button onClick={() => navigate("/signin")} className="flex-1 bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2">
                            <LogIn className="w-5 h-5" /> Accedi
                        </Button>
                        <Button onClick={() => navigate("/signup")} variant="outline" className="flex-1 flex items-center justify-center gap-2">
                            <UserPlus className="w-5 h-5" /> Registrati
                        </Button>
                    </div>
                </Card>
            </div>
        ) : (
            <div className="min-h-screen p-8">
                <div className="max-w-4xl mx-auto">
                    <h1 className="text-3xl font-bold mb-8">Benvenuto, {currentUser.nome}!</h1>

                    {/* Upload Card */}
                    <Card className="p-8 mb-8">
                        <h2 className="text-2xl font-semibold mb-6 flex items-center gap-2">
                            <Upload className="w-6 h-6" />
                            Carica documenti
                        </h2>
                        <div
                            onDragOver={handleDragOver}
                            onDragLeave={handleDragLeave}
                            onDrop={handleDrop}
                            className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors cursor-pointer ${
                                isDragOver ? "border-blue-500 bg-blue-50" : "border-gray-300 bg-gray-50 hover:border-gray-400"
                            }`}
                        >
                            <Upload className="w-12 h-12 mx-auto mb-4 text-gray-400" />
                            <h3 className="text-lg font-medium text-gray-900 mb-2">Trascina i file qui</h3>
                            <p className="text-gray-600 mb-4">oppure</p>
                            <label>
                                <input type="file" multiple onChange={handleFileInput} className="hidden"
                                    accept=".txt,.pdf,.doc,.docx,.xls,.xlsx" />
                                <Button type="button" variant="outline" className="cursor-pointer">
                                    Seleziona file
                                </Button>
                            </label>
                            <p className="text-sm text-gray-500 mt-4">
                                Supportati: TXT, PDF, DOC, DOCX, XLS, XLSX (Max 10MB)
                            </p>
                        </div>

                        {selectedFiles.length > 0 && (
                            <div className="mt-8">
                                <h3 className="text-lg font-semibold mb-4">File selezionati ({selectedFiles.length})</h3>
                                <div className="space-y-2 max-h-64 overflow-y-auto">
                                    {selectedFiles.map((file, index) => (
                                        <div key={index} className="flex items-center justify-between bg-gray-100 p-4 rounded-lg">
                                            <div className="flex items-center gap-3">
                                                <File className="w-5 h-5 text-gray-500" />
                                                <div>
                                                    <p className="font-medium text-sm">{file.name}</p>
                                                    <p className="text-xs text-gray-600">{(file.size / 1024).toFixed(2)} KB</p>
                                                </div>
                                            </div>
                                            <button onClick={() => removeSelected(index)} className="text-red-500 hover:text-red-700 font-semibold text-sm">
                                                Rimuovi
                                            </button>
                                        </div>
                                    ))}
                                </div>
                                <div className="flex gap-4 mt-6">
                                    <Button onClick={handleUpload} disabled={uploading} className="flex-1 bg-blue-600 hover:bg-blue-700">
                                        {uploading ? <><Loader2 className="w-4 h-4 mr-2 animate-spin" />Caricamento...</> : "Carica file"}
                                    </Button>
                                    <Button onClick={() => setSelectedFiles([])} variant="outline" className="flex-1" disabled={uploading}>
                                        Cancella tutto
                                    </Button>
                                </div>
                            </div>
                        )}
                    </Card>

                    {/* Lista Documenti */}
                    <Card className="p-8">
                        <h2 className="text-2xl font-semibold mb-6 flex items-center gap-2">
                            <File className="w-6 h-6" />
                            I tuoi documenti
                        </h2>
                        {loadingDocuments ? (
                            <div className="flex items-center justify-center py-12">
                                <Loader2 className="w-8 h-8 animate-spin text-gray-400" />
                            </div>
                        ) : documents.length === 0 ? (
                            <div className="text-center py-12 text-gray-500">
                                <File className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                                <p>Nessun documento caricato</p>
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {documents.map((doc) => (
                                    <div key={doc.id} className="flex items-center justify-between bg-gray-50 p-4 rounded-lg">
                                        <div className="flex items-center gap-3">
                                            <File className="w-5 h-5 text-gray-500" />
                                            <div>
                                                <p className="font-medium text-sm">{doc.filename}</p>
                                                <p className="text-xs text-gray-500">
                                                    {new Date(doc.uploadDate).toLocaleDateString("it-IT")} — {(doc.metadata.size / 1024).toFixed(2)} KB
                                                </p>
                                            </div>
                                        </div>
                                        <button onClick={() => handleDelete(doc.id, doc.filename)} className="text-red-500 hover:text-red-700">
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </Card>
                </div>
            </div>
        );
    };

    export default Homepage;