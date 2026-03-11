import React from "react";
import { Navbar } from "./navbar";
import { Toaster } from "@/components/ui/toaster";

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div>
      <Navbar />
      <main className="pt-14">
        {children}
      </main>
      <Toaster />
    </div>
  );
};
