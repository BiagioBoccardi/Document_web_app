import { Outlet } from "react-router";


export function Layout() {

    return (
        <div className="flex justify-center">
            
            <Outlet/>
        </div>
    )
}