import { Route, Routes } from "react-router"
import { Layout } from "./components/layout"
import { Homepage } from "./pages/homepage"
import { SignupForm } from "./components/signup-form"
import { LoginForm } from "./components/login-form"
import { ContextProvider } from "./context/context"


function App() {

  return (
    <ContextProvider>
      <Routes>
        <Route path="/" element={<Layout/>}>
          <Route index path="sign-up" element={<SignupForm/>}/>
          <Route path="sign-in" element={<LoginForm/>}/>
          <Route element={<Homepage/>}/>
        </Route>
        
      </Routes>
    </ContextProvider>
  )
}

export default App