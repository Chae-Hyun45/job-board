import { useEffect, useState } from 'react'
import { authApi } from '../api/authApi'
import { AuthContext } from './AuthContextInstance'
import { useContext } from 'react'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    authApi.me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(credentials) {
    const loggedInUser = await authApi.login(credentials)
    setUser(loggedInUser)
    return loggedInUser
  }

  async function logout() {
    await authApi.logout()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
