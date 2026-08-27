import { Outlet } from 'react-router-dom'
import { UnreadCountProvider } from '../notifications/UnreadCountContext'
import { Navbar } from './Navbar'

export function AppLayout() {
  return (
    <UnreadCountProvider>
      <Navbar />
      <main className="container py-4" style={{ maxWidth: 640 }}>
        <Outlet />
      </main>
    </UnreadCountProvider>
  )
}
