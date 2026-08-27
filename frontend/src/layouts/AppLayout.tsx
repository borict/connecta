import { Outlet } from 'react-router-dom'
import { MessageUnreadProvider } from '../messages/MessageUnreadContext'
import { UnreadCountProvider } from '../notifications/UnreadCountContext'
import { Navbar } from './Navbar'

export function AppLayout() {
  return (
    <UnreadCountProvider>
      <MessageUnreadProvider>
        <Navbar />
        <main className="container py-4" style={{ maxWidth: 640 }}>
          <Outlet />
        </main>
      </MessageUnreadProvider>
    </UnreadCountProvider>
  )
}
