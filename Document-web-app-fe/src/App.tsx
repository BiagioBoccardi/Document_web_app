import { Route, Routes } from "react-router-dom";
import { LoginForm } from "./components/login-form";
import { SignupForm } from "./components/signup-form";
import { Home } from "./pages/home";
import { GroupsPage } from "./pages/GroupsPage";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout><Home /></Layout>} />
      <Route path="/group" element={<Layout><ProtectedRoute><GroupsPage /></ProtectedRoute></Layout>} />
      <Route path="/login" element={<LoginForm />} />
      <Route path="/signup" element={<SignupForm />} />
    </Routes>
  );
}

export default App;