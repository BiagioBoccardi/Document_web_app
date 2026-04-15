import { useState } from "react";
import { FileText, ChevronDown, ChevronUp, Layers } from "lucide-react";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export interface SearchResult {
  id?: string;
  documentId?: string;
  filename?: string;
  snippet: string;
  score: number;
}

interface SearchResultCardProps {
  result: SearchResult;
  rank: number;
  onSimilarClick?: () => void;
}

const SNIPPET_LIMIT = 280;

function normalizeScore(score: number): number {
  return score > 1 ? score : score * 100;
}

function getScoreBadgeClass(score: number): string {
  const pct = normalizeScore(score);
  if (pct >= 80) return "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400";
  if (pct >= 60) return "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400";
  return "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400";
}

function getScoreBarClass(score: number): string {
  const pct = normalizeScore(score);
  if (pct >= 80) return "bg-green-500";
  if (pct >= 60) return "bg-yellow-500";
  return "bg-red-500";
}

export default function SearchResultCard({ result, rank, onSimilarClick }: SearchResultCardProps) {
  const [expanded, setExpanded] = useState(false);

  const scorePct = normalizeScore(result.score);
  const isLong = result.snippet.length > SNIPPET_LIMIT;
  const displayedContent =
    isLong && !expanded ? result.snippet.slice(0, SNIPPET_LIMIT) + "…" : result.snippet;

  const showSimilarButton = !!onSimilarClick && !!result.documentId;

  return (
    <Card className="overflow-hidden hover:shadow-md transition-shadow gap-0">
      <CardHeader className="bg-muted/30 py-3 px-4 border-b flex flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3 overflow-hidden">
          <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-xs font-bold shrink-0">
            {rank}
          </span>
          <div className="flex items-center gap-1.5 overflow-hidden">
            <FileText className="w-4 h-4 text-primary shrink-0" />
            <span className="text-sm font-semibold truncate" title={result.filename}>
              {result.filename || "Documento Sconosciuto"}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <div className="hidden sm:flex flex-col items-end gap-1">
            <div className="w-20 h-1.5 rounded-full bg-muted overflow-hidden">
              <div
                className={cn("h-full rounded-full transition-all", getScoreBarClass(result.score))}
                style={{ width: `${Math.min(scorePct, 100)}%` }}
              />
            </div>
          </div>
          <span
            className={cn(
              "text-xs font-bold px-2 py-1 rounded-full",
              getScoreBadgeClass(result.score)
            )}
          >
            {scorePct.toFixed(1)}%
          </span>
        </div>
      </CardHeader>

      <CardContent className="p-4 flex flex-col gap-2">
        {result.documentId && (
          <p className="text-xs text-muted-foreground/60 font-mono">ID: {result.documentId}</p>
        )}

        <div className="text-sm text-muted-foreground leading-relaxed border-l-2 border-primary/40 pl-4 italic whitespace-pre-wrap">
          "{displayedContent}"
        </div>

        {isLong && (
          <Button
            variant="ghost"
            size="xs"
            className="self-start mt-1 text-xs text-muted-foreground"
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? (
              <>
                <ChevronUp className="w-3 h-3" />
                Mostra meno
              </>
            ) : (
              <>
                <ChevronDown className="w-3 h-3" />
                Mostra tutto
              </>
            )}
          </Button>
        )}
      </CardContent>

      {showSimilarButton && (
        <CardFooter className="px-4 py-2 border-t bg-muted/10">
          <Button
            variant="ghost"
            size="xs"
            className="text-xs text-muted-foreground hover:text-foreground"
            onClick={onSimilarClick}
          >
            <Layers className="w-3 h-3" />
            Documenti simili
          </Button>
        </CardFooter>
      )}
    </Card>
  );
}
