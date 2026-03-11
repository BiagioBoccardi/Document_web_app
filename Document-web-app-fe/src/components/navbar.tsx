import { Button } from "@/components/ui/button";
import { User } from "lucide-react";
import { Link } from "react-router-dom";

export function Navbar() {
  return (
    <header className="px-4 lg:px-6 h-14 flex items-center fixed top-0 left-0 right-0 bg-white z-10 shadow-sm">
      <Link className="flex items-center justify-center mr-6" to="/">
        <span className="sr-only">Document Web App</span>
        <span className="font-semibold text-lg">DocuVault</span>
      </Link>
      <nav className="flex gap-4 sm:gap-6">
        <Link
          className="text-sm font-medium hover:underline underline-offset-4"
          to="/"
        >
          Home
        </Link>
        <Link
          className="text-sm font-medium hover:underline underline-offset-4"
          to="/group"
        >
          Groups
        </Link>
      </nav>
      <div className="ml-auto flex gap-4 sm:gap-6">
        <Link to="/login">
          <Button variant="ghost" size="icon" className="rounded-full">
            <User className="h-5 w-5" />
          </Button>
        </Link>
      </div>
    </header>
  );
}
