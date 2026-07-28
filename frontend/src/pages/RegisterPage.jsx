import { useState } from 'react'
import { useNavigate } from 'react-router'
import { authApi } from '../api/authApi'

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', name: '' })
  const [error, setError] = useState(null)

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      await authApi.register(form)
      navigate('/login')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="page page-narrow">
      <form className="card" onSubmit={handleSubmit}>
        <h1>회원가입</h1>
        {error && <p className="alert" role="alert">{error}</p>}
        <div className="field">
          <label htmlFor="register-email">이메일</label>
          <input id="register-email" aria-label="이메일" name="email" type="email" value={form.email}
                 onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="register-password">비밀번호</label>
          <input id="register-password" aria-label="비밀번호" name="password" type="password" value={form.password}
                 onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="register-name">이름</label>
          <input id="register-name" aria-label="이름" name="name" value={form.name}
                 onChange={handleChange} required />
        </div>
        <button type="submit" className="btn btn-primary">회원가입</button>
      </form>
    </div>
  )
}
