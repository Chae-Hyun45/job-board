import { useEffect, useState } from 'react'
import { adminApi } from '../../api/adminApi'

export function AdminJobListPage() {
  const [postings, setPostings] = useState([])
  const [error, setError] = useState(null)

  function reload() {
    adminApi.listJobPostings().then(setPostings).catch((err) => setError(err.message))
  }

  useEffect(() => {
    reload()
  }, [])

  async function handleDelete(id) {
    await adminApi.deleteJobPosting(id)
    reload()
  }

  return (
    <div>
      <h1>채용공고 관리</h1>
      {error && <p className="alert" role="alert">{error}</p>}
      {postings.length === 0 ? (
        <p className="empty-state">등록된 채용공고가 없습니다.</p>
      ) : (
        <ul className="admin-list">
          {postings.map((posting) => (
            <li key={posting.id}>
              <span className="item-info">
                <span>{posting.companyName}</span>
                <span className="tag">{posting.status}</span>
              </span>
              <span className="item-actions">
                <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(posting.id)}>삭제</button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
