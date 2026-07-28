import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      await login({ email, password })
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h1>로그인</h1>
      {error && <p role="alert">{error}</p>}
      <label htmlFor="login-email">이메일</label>
      <input id="login-email" aria-label="이메일" type="email" value={email}
             onChange={(e) => setEmail(e.target.value)} required />
      <label htmlFor="login-password">비밀번호</label>
      <input id="login-password" aria-label="비밀번호" type="password" value={password}
             onChange={(e) => setPassword(e.target.value)} required />
      <button type="submit">로그인</button>
      <p><Link to="/register">회원가입</Link></p>
    </form>
  )
}
