
import { Navbar } from "./navbar";
import { Outlet } from "react-router";


export const Layout = () => {
  return (
    <div>
      <Navbar />
      <main className="pt-14">
        <Outlet/>
      </main>
      
    </div>
  );
};
