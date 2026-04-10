import { useEffect, useState } from "react";
import { FileSearch, AlertCircle } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import SearchResultCard, {
  type SearchResult,
} from "../components/SearchResultCard";
import SearchResultSkeleton from "../components/SearchResultSkeleton";

interface SimilarDocumentsSheetProps {
  open: boolean;
  onClose: () => void;
  documentId: string;
  filename?: string;
}

export default function SimilarDocumentsSheet({
  open,
  onClose,
  documentId,
  filename,
}: SimilarDocumentsSheetProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) return;

    const controller = new AbortController();

    const fetchSimilar = async () => {
      setIsLoading(true);
      setError("");
      setResults([]);

      try {
        const token = localStorage.getItem("token");
        const response = await fetch(`/api/v1/search/similar/${documentId}`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error("Errore nel recupero dei documenti simili.");
        }

        const data = await response.json();
        setResults(data.results ?? []);
      } catch (err: any) {
        if (err.name === "AbortError") return;
        setError(err.message || "Si è verificato un errore imprevisto.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchSimilar();

    return () => controller.abort();
  }, [open, documentId]);

  return (
    <Sheet open={open} onOpenChange={(v) => !v && onClose()}>
      <SheetContent
        side="right"
        className="w-full sm:max-w-lg flex flex-col gap-0 p-0"
      >
        <SheetHeader className="border-b p-4 pb-4 shrink-0">
          <div className="flex items-center gap-2">
            <FileSearch className="w-5 h-5 text-primary shrink-0" />
            <SheetTitle>Documenti Simili</SheetTitle>
          </div>
          <SheetDescription>
            Documenti semanticamente affini a{" "}
            <span className="font-medium text-foreground">
              {filename || documentId}
            </span>
          </SheetDescription>
        </SheetHeader>

        <div className="flex flex-col gap-3 p-4 overflow-y-auto flex-1">
          {isLoading ? (
            <SearchResultSkeleton count={3} />
          ) : error ? (
            <div className="p-6 text-center flex flex-col items-center gap-2">
              <AlertCircle className="w-8 h-8 text-destructive/70" />
              <p className="text-sm font-medium text-destructive">{error}</p>
            </div>
          ) : results.length > 0 ? (
            results.map((res, index) => (
              <SearchResultCard
                key={res.id ?? res.documentId ?? index}
                result={res}
                rank={index + 1}
              />
            ))
          ) : (
            <div className="p-8 text-center text-muted-foreground border border-dashed rounded-lg flex flex-col items-center gap-2">
              <FileSearch className="w-8 h-8 text-muted-foreground/50" />
              <p className="text-sm font-medium">
                Nessun documento simile trovato
              </p>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
