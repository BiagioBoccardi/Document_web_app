import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useContextCast } from "@/context/context";

export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { currentUser } = useContextCast();
  const location = useLocation();

  if (!currentUser) {
    // Se l'utente non è loggato, reindirizza al login
    // Passiamo 'state' con la location attuale per poterci tornare dopo il login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};
