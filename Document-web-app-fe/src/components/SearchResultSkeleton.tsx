import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function SingleSkeleton() {
  return (
    <Card className="overflow-hidden gap-0">
      <CardHeader className="bg-muted/30 py-3 px-4 border-b flex flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Skeleton className="w-6 h-6 rounded-full" />
          <Skeleton className="h-4 w-48" />
        </div>
        <Skeleton className="h-6 w-16 rounded-full" />
      </CardHeader>
      <CardContent className="p-4 flex flex-col gap-2">
        <Skeleton className="h-3 w-40" />
        <div className="flex flex-col gap-1.5 pl-4 border-l-2 border-muted">
          <Skeleton className="h-3 w-full" />
          <Skeleton className="h-3 w-full" />
          <Skeleton className="h-3 w-4/5" />
        </div>
      </CardContent>
    </Card>
  );
}

interface SearchResultSkeletonProps {
  count?: number;
}

export default function SearchResultSkeleton({ count = 3 }: SearchResultSkeletonProps) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <SingleSkeleton key={i} />
      ))}
    </>
  );
}
