import { Route, Routes } from "react-router-dom";
import { LoginForm } from "./components/login-form";
import { SignupForm } from "./components/signup-form";
import { GroupsPage } from "./pages/GroupsPage";
import SearchPage from "./pages/SearchPage";

import { ContextProvider } from "./context/context";
import { Layout } from "./components/layout";
import Homepage from "./pages/homepage";
import { ProtectedRoute } from "./components/protected-route"; // <-- Importiamo il guardiano

function App() {
  return (
    <ContextProvider>
      <Routes>
        {/* Il Layout avvolge tutte le rotte: la Navbar sarà sempre visibile */}
        <Route path="/" element={<Layout />}>
          {/* 🟢 Rotte Pubbliche (accessibili a tutti) */}
          <Route index element={<Homepage />} />
          <Route path="signin" element={<LoginForm />} />
          <Route path="signup" element={<SignupForm />} />

          {/* 🔴 Rotte Protette (accessibili solo con Token) */}
          <Route element={<ProtectedRoute />}>
            <Route path="group" element={<GroupsPage />} />
            {/* Rimosso lo slash iniziale per mantenere la coerenza con le altre rotte figlie */}
            <Route path="ricerca" element={<SearchPage />} />
          </Route>
        </Route>
      </Routes>
    </ContextProvider>
  );
}

export default App;
