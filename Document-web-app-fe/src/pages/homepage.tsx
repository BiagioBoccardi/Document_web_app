import { useState } from "react";
import { useContextCast } from "@/context/context";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { Upload, File, AlertCircle, LogIn, UserPlus } from "lucide-react";
import { useNavigate } from "react-router-dom";

const Homepage = () => {
  const { currentUser } = useContextCast();
  const navigate = useNavigate();
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);

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

    const files = Array.from(e.dataTransfer.files);
    handleFiles(files);
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    handleFiles(files);
  };

  const handleFiles = (files: File[]) => {
    // Aggiorna l'elenco dei file selezionati
    setUploadedFiles((prev) => [...prev, ...files]);
    toast.info(`${files.length} file selezionato/i`);
  };

  const handleUpload = () => {
    if (uploadedFiles.length === 0) {
      toast.error("Seleziona almeno un file");
      return;
    }

    // Blocco l'upload per ora
    toast.error("Caricamento file disabilitato al momento");
    console.log("Upload bloccato. File da caricare:", uploadedFiles);
  };

  const removeFile = (index: number) => {
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const clearAllFiles = () => {
    setUploadedFiles([]);
    toast.success("File cancellati");
  };

  return !currentUser ? (
    // Sezione Utente Non Loggato
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-b from-gray-50 to-gray-100">
      <Card className="p-12 text-center max-w-2xl">
        <AlertCircle className="w-16 h-16 mx-auto mb-6 text-orange-500" />
        <h1 className="text-3xl font-bold mb-2">Utente Non Loggato</h1>
        <p className="text-gray-600 mb-6 text-lg">
          Accedi al tuo account o registrati per accedere ai tuoi documenti e
          effettuare il caricamento di file.
        </p>

        <div className="bg-gray-50 p-6 rounded-lg mb-8 text-left">
          <h2 className="font-semibold text-gray-900 mb-3">
            Funzionalità disponibili dopo il login:
          </h2>
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
          <Button
            onClick={() => navigate("/signin")}
            className="flex-1 bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2"
          >
            <LogIn className="w-5 h-5" />
            Accedi
          </Button>
          <Button
            onClick={() => navigate("/signup")}
            variant="outline"
            className="flex-1 flex items-center justify-center gap-2"
          >
            <UserPlus className="w-5 h-5" />
            Registrati
          </Button>
        </div>
      </Card>
    </div>
  ) : (
    // Sezione Utente Loggato
    <div className="min-h-screen p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">
          Benvenuto, {currentUser.nome}!
        </h1>

        {/* Sezione Dropzone */}
        <Card className="p-8 mb-8">
          <h2 className="text-2xl font-semibold mb-6 flex items-center gap-2">
            <Upload className="w-6 h-6" />
            Carica i tuoi documenti
          </h2>

          {/* Area Drag and Drop */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors cursor-pointer ${
              isDragOver
                ? "border-blue-500 bg-blue-50"
                : "border-gray-300 bg-gray-50 hover:border-gray-400"
            }`}
          >
            <Upload className="w-12 h-12 mx-auto mb-4 text-gray-400" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Trascina i file qui
            </h3>
            <p className="text-gray-600 mb-4">oppure</p>
            <label>
              <input
                type="file"
                multiple
                onChange={handleFileInput}
                className="hidden"
              />
              <Button
                type="button"
                variant="outline"
                className="cursor-pointer"
              >
                Seleziona file
              </Button>
            </label>
            <p className="text-sm text-gray-500 mt-4">
              Supportati: PDF, DOC, DOCX, XLS, XLSX (Max 10MB per file)
            </p>
          </div>

          {/* Lista File Selezionati */}
          {uploadedFiles.length > 0 && (
            <div className="mt-8">
              <h3 className="text-lg font-semibold mb-4">
                File selezionati ({uploadedFiles.length})
              </h3>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {uploadedFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between bg-gray-100 p-4 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <File className="w-5 h-5 text-gray-500" />
                      <div>
                        <p className="font-medium text-sm">{file.name}</p>
                        <p className="text-xs text-gray-600">
                          {(file.size / 1024).toFixed(2)} KB
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => removeFile(index)}
                      className="text-red-500 hover:text-red-700 font-semibold text-sm"
                    >
                      Rimuovi
                    </button>
                  </div>
                ))}
              </div>

              {/* Pulsanti Azione */}
              <div className="flex gap-4 mt-6">
                <Button
                  onClick={handleUpload}
                  className="flex-1 bg-blue-600 hover:bg-blue-700"
                >
                  Carica file
                </Button>
                <Button
                  onClick={clearAllFiles}
                  variant="outline"
                  className="flex-1"
                >
                  Cancella tutto
                </Button>
              </div>
            </div>
          )}
        </Card>

        {/* Info Sezione */}
        <Card className="p-6 bg-blue-50 border-blue-200">
          <p className="text-sm text-blue-900">
            <strong>Nota:</strong> La funzionalità di caricamento è attualmente
            disabilitata. Puoi selezionare i file, ma l'upload non verrà
            completato.
          </p>
        </Card>
      </div>
    </div>
  );
};

export default Homepage;
