import { Outlet } from 'react-router-dom'
import { Navbar } from './Navbar'

export function AppLayout() {
  return (
    <>
      <Navbar />
      <main className="container py-4" style={{ maxWidth: 640 }}>
        <Outlet />
      </main>
    </>
  )
}
