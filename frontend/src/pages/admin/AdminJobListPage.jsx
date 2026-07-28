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

  async function handleAddDummy() {
    await adminApi.createDummyJobPostings()
    reload()
  }

  async function handleDeleteDummy() {
    await adminApi.deleteDummyJobPostings()
    reload()
  }

  return (
    <div>
      <h1>채용공고 관리</h1>
      {error && <p role="alert">{error}</p>}
      <button type="button" onClick={handleAddDummy}>더미데이터 추가</button>
      <button type="button" onClick={handleDeleteDummy}>더미데이터 삭제</button>
      <ul>
        {postings.map((posting) => (
          <li key={posting.id}>
            <span>{posting.companyName}</span> ({posting.status})
            <button type="button" onClick={() => handleDelete(posting.id)}>삭제</button>
          </li>
        ))}
      </ul>
    </div>
  )
}
