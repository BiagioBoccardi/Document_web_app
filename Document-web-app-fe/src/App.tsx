import { Route, Routes } from "react-router-dom";
import { LoginForm } from "./components/login-form";
import { SignupForm } from "./components/signup-form";
import { GroupsPage } from "./pages/GroupsPage";
import SearchPage from "./pages/SearchPage";

import DocumentDetailPage from "./pages/DocumentDetailPage";
import DocumentEditPage from "./pages/DocumentEditPage";
import DocumentPreviewPage from "@/pages/DocumentPreviewPage";

import { Layout } from "./components/layout";
import Homepage from "./pages/homepage";
import { ProtectedRoute } from "./components/protected-route"; // <-- Importiamo il guardiano

function App() {
  return (
      <Routes>
        <Route path="/" element={<Layout />}>
          {/*  Rotte Pubbliche (accessibili a tutti) */}
          <Route index element={<Homepage />} />
          <Route path="signin" element={<LoginForm />} />
          <Route path="signup" element={<SignupForm />} />

          {/*  Rotte Protette (accessibili solo con Token) */}
          <Route element={<ProtectedRoute />}>
            <Route path="group" element={<GroupsPage />} />
            <Route path="ricerca" element={<SearchPage />} />
            <Route path="/documents/:id" element={<DocumentDetailPage />} />
            <Route path="/documents/:id/preview" element={<DocumentPreviewPage />} />
            <Route path="/documents/:id/edit" element={<DocumentEditPage />} />
          </Route>
        </Route>
      </Routes>
  );
}

export default App;
