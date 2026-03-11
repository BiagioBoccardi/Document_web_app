import { Button } from "@/components/ui/button";
import { Navbar } from "@/components/navbar";

export function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <main className="flex-1">
        <section className="w-full py-12 md:py-24 lg:py-32 xl:py-48">
          <div className="container px-4 md:px-6">
            <div className="flex flex-col items-center space-y-4 text-center">
              <h1 className="text-3xl font-bold tracking-tighter sm:text-5xl md:text-6xl lg:text-7xl/none">
                Securely Manage and Search Your Documents
              </h1>
              <p className="mx-auto max-w-[700px] text-gray-500 md:text-xl dark:text-gray-400">
                DocuVault is a powerful document management system that allows you to upload, organize, and search your documents with ease. Powered by AI for semantic search.
              </p>
              <div className="space-x-4">
                
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
