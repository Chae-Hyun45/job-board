import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../context/AuthContext'

const ADMIN_CREDENTIALS = { email: 'admin@jobboard.local', password: 'admin1234!' }

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

  async function handleAdminLogin() {
    setError(null)
    try {
      await login(ADMIN_CREDENTIALS)
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="page page-narrow">
      <form className="card" onSubmit={handleSubmit}>
        <h1>로그인</h1>
        {error && <p className="alert" role="alert">{error}</p>}
        <div className="field">
          <label htmlFor="login-email">이메일</label>
          <input id="login-email" aria-label="이메일" type="email" value={email}
                 onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="login-password">비밀번호</label>
          <input id="login-password" aria-label="비밀번호" type="password" value={password}
                 onChange={(e) => setPassword(e.target.value)} required />
        </div>
        <button type="submit" className="btn btn-primary">로그인</button>
        <button type="button" className="btn btn-secondary" style={{ marginTop: 8 }} onClick={handleAdminLogin}>
          관리자 로그인
        </button>
        <p style={{ marginTop: 14 }}><Link to="/register">회원가입</Link></p>
      </form>
    </div>
  )
}
