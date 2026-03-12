import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useContextCast } from "@/context/context";
import { LogIn, LogOut, UserCircle, Users, Menu, X } from "lucide-react";
import { useState } from "react";
import { Link, useLocation } from "react-router-dom";

export const Navbar = () => {
  const { currentUser, signOut } = useContextCast();
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-14 border-b border-border bg-background/80 backdrop-blur-sm">
      <div className="mx-auto flex h-full max-w-5xl items-center justify-between px-4">
        {/* Logo */}
        <Link
          to="/"
          className="flex items-center gap-2 font-semibold tracking-tight text-foreground hover:opacity-80 transition-opacity"
        >
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-primary text-primary-foreground text-xs font-bold">
            D
          </span>
          <span className="hidden sm:inline">DocuVault</span>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden sm:flex items-center gap-1">
          {currentUser && (
            <Link
              to="/group"
              className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                isActive("/group")
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground hover:text-foreground hover:bg-accent/50"
              }`}
            >
              <Users size={15} />
              Gruppi
            </Link>
          )}
        </nav>

        {/* Auth Area */}
        <div className="hidden sm:flex items-center gap-2">
          {currentUser ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex items-center gap-2"
                >
                  <UserCircle size={17} className="text-muted-foreground" />
                  <span className="max-w-[120px] truncate text-sm font-medium">
                    {currentUser.nome}
                  </span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuLabel className="font-normal">
                  <div className="flex flex-col gap-0.5">
                    <span className="text-sm font-semibold">{currentUser.nome}</span>
                    <span className="text-xs text-muted-foreground truncate">
                      {currentUser.email}
                    </span>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={signOut}
                  className="text-destructive focus:text-destructive cursor-pointer"
                >
                  <LogOut size={14} className="mr-2" />
                  Esci
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/signin">Accedi</Link>
              </Button>
              <Button size="sm" asChild>
                <Link to="/signup">Registrati</Link>
              </Button>
            </>
          )}
        </div>

        {/* Mobile Hamburger */}
        <button
          className="sm:hidden rounded-md p-1.5 text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
          onClick={() => setMobileOpen((v) => !v)}
          aria-label="Menu"
        >
          {mobileOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="sm:hidden border-t border-border bg-background px-4 py-3 flex flex-col gap-2 shadow-md">
          {currentUser ? (
            <>
              <div className="flex items-center gap-2 py-1.5 px-1">
                <UserCircle size={17} className="text-muted-foreground" />
                <div className="flex flex-col">
                  <span className="text-sm font-semibold">{currentUser.nome}</span>
                  <span className="text-xs text-muted-foreground">{currentUser.email}</span>
                </div>
              </div>
              <Link
                to="/group"
                onClick={() => setMobileOpen(false)}
                className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive("/group")
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground hover:text-foreground hover:bg-accent/50"
                }`}
              >
                <Users size={15} />
                Gruppi
              </Link>
              <button
                onClick={() => { signOut(); setMobileOpen(false); }}
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 transition-colors"
              >
                <LogOut size={15} />
                Esci
              </button>
            </>
          ) : (
            <>
              <Link
                to="/signin"
                onClick={() => setMobileOpen(false)}
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors"
              >
                <LogIn size={15} />
                Accedi
              </Link>
              <Link
                to="/signup"
                onClick={() => setMobileOpen(false)}
                className="flex items-center justify-center rounded-md px-3 py-2 text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
              >
                Registrati
              </Link>
            </>
          )}
        </div>
      )}
    </header>
  );
};