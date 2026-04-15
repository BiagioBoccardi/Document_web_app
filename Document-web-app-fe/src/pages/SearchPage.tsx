import { useState, useRef, useEffect } from "react";
import { Search, Loader2, FileText, SearchX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import SearchResultCard, {
  type SearchResult,
} from "@/components/SearchResultCard";
import SearchResultSkeleton from "@/components/SearchResultSkeleton";
import SimilarDocumentsSheet from "@/search_service/SimilarDocumentsSheet";

interface SimilarTarget {
  documentId: string;
  filename?: string;
}

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [topK, setTopK] = useState<number>(5);
  const [error, setError] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [hasSearched, setHasSearched] = useState(false);
  const [lastQuery, setLastQuery] = useState("");

  const [similarTarget, setSimilarTarget] = useState<SimilarTarget | null>(
    null,
  );

  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  const handleSearch = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!query.trim()) {
      setError("Inserisci un concetto o una frase da cercare.");
      return;
    }
    if (topK < 1 || topK > 50) {
      setError("Il numero di risultati (Top K) deve essere tra 1 e 50.");
      return;
    }

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    setError("");
    setIsLoading(true);
    setHasSearched(true);
    setLastQuery(query.trim());
    setResults([]);

    try {
      const token = localStorage.getItem("token");

      const response = await fetch("/api/v1/search", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ query, topK }),
        signal: abortController.signal,
      });

      if (!response.ok) {
        const body = await response.text();
        throw new Error(
          `Errore ${response.status}: ${body || response.statusText}`,
        );
      }

      const data = await response.json();
      setResults(data.results ?? []);
      setIsLoading(false);
    } catch (err: unknown) {
  if (err instanceof Error && err.name === "AbortError") {
    console.log("Richiesta di ricerca precedente annullata.");
    return;
  }

  setError(err instanceof Error ? err.message : "Si è verificato un errore imprevisto.");
  setResults([]);
  setIsLoading(false);
}
  };

  return (
    <div className="container mx-auto py-8 px-4 max-w-4xl flex flex-col gap-6">
      <div className="text-center">
        <h1 className="text-3xl font-bold tracking-tight mb-2">
          Ricerca Semantica
        </h1>
        <p className="text-muted-foreground">
          Cerca concetti, argomenti o paragrafi all'interno dei tuoi documenti
          analizzandone il significato.
        </p>
      </div>

      <Card className="shadow-sm border-border">
        <CardHeader className="pb-4">
          <CardTitle className="text-lg">Parametri di Ricerca</CardTitle>
          <CardDescription>
            Inserisci cosa stai cercando e quanti risultati vuoi ottenere.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSearch} className="flex flex-col gap-4">
            <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-end">
              <div className="grid w-full gap-1.5 flex-1">
                <Label htmlFor="query">Cosa stai cercando?</Label>
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="query"
                    placeholder="Es. 'Procedure per ferie e permessi'..."
                    className="pl-9"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    disabled={isLoading}
                  />
                </div>
              </div>

              <div className="grid w-full sm:w-32 gap-1.5">
                <Label htmlFor="topK">Risultati (Top K)</Label>
                <Input
                  id="topK"
                  type="number"
                  min="1"
                  max="50"
                  value={topK}
                  onChange={(e) => setTopK(Number(e.target.value))}
                  disabled={isLoading}
                />
              </div>

              <Button
                type="submit"
                className="w-full sm:w-auto min-w-25"
                disabled={isLoading}
              >
                {isLoading ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <>
                    <Search className="w-4 h-4 mr-2" />
                    Cerca
                  </>
                )}
              </Button>
            </div>

            {error && (
              <p className="text-sm font-medium text-destructive mt-1">
                {error}
              </p>
            )}
          </form>
        </CardContent>
      </Card>

      {hasSearched && (
        <div className="flex flex-col gap-4 mt-2">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold flex items-center gap-2">
              <FileText className="w-5 h-5" />
              Risultati
              {!isLoading && results.length > 0 && (
                <span className="text-base font-normal text-muted-foreground">
                  ({results.length}) per:{" "}
                  <span className="italic">"{lastQuery}"</span>
                </span>
              )}
            </h2>
          </div>

          {isLoading ? (
            <SearchResultSkeleton count={topK > 3 ? 3 : topK} />
          ) : results.length > 0 ? (
            results.map((res, index) => (
              <SearchResultCard
                key={res.id ?? res.documentId ?? index}
                result={res}
                rank={index + 1}
                onSimilarClick={
                  res.documentId
                    ? () =>
                        setSimilarTarget({
                          documentId: res.documentId!,
                          filename: res.filename,
                        })
                    : undefined
                }
              />
            ))
          ) : (
            <div className="p-10 text-center text-muted-foreground border border-dashed rounded-lg flex flex-col items-center gap-3">
              <SearchX className="w-10 h-10 text-muted-foreground/50" />
              <div>
                <p className="font-medium">Nessun documento trovato</p>
                <p className="text-sm mt-1">
                  Nessun risultato per{" "}
                  <span className="italic">"{lastQuery}"</span>. Prova con
                  parole diverse o concetti più ampi.
                </p>
              </div>
            </div>
          )}
        </div>
      )}

      <SimilarDocumentsSheet
        open={!!similarTarget}
        onClose={() => setSimilarTarget(null)}
        documentId={similarTarget?.documentId ?? ""}
        filename={similarTarget?.filename}
      />
    </div>
  );
}
