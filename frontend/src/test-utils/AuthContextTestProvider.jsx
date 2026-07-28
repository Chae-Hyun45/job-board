import { AuthContext } from '../context/AuthContextInstance'

export function AuthContextTestProvider({ value, children }) {
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
