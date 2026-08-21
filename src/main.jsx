import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './app/App'
import { UserRoleProvider } from './entities/user/model/UserRoleContext'
import './app/styles.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <UserRoleProvider>
        <App />
      </UserRoleProvider>
    </BrowserRouter>
  </StrictMode>,
)
