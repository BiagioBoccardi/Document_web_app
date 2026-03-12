import { Route, Routes } from "react-router-dom";
import { LoginForm } from "./components/login-form";
import { SignupForm } from "./components/signup-form";
import { GroupsPage } from "./pages/GroupsPage";
import { ContextProvider } from "./context/context";
import { Layout } from "./components/layout";
import Homepage from "./pages/homepage";

function App() {
  return (
    <ContextProvider>
      <Routes>
        <Route path="/" element={<Layout/>}>
          <Route index element={<Homepage/>}/>
          <Route path="group" element={<GroupsPage />}/>
          <Route path="signin" element={<LoginForm />} />
          <Route path="signup" element={<SignupForm />} />
        </Route>
      </Routes>
    </ContextProvider>
  );
}

export default App;