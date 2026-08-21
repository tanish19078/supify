import { createContext, useState } from 'react'

export const UserRoleContext = createContext(null)

export const roleLabels = {
  buyer: 'Buyer',
  supplier: 'Supplier',
  verifier: 'Verifier',
}

const roles = Object.keys(roleLabels)

export function UserRoleProvider({ children }) {
  const [role, setRole] = useState('buyer')

  function cycleRole() {
    setRole((current) => roles[(roles.indexOf(current) + 1) % roles.length])
  }

  return (
    <UserRoleContext.Provider value={{ role, setRole, cycleRole }}>
      {children}
    </UserRoleContext.Provider>
  )
}
