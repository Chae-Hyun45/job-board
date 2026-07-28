import { useEffect, useState } from 'react'
import { jobApi } from '../api/jobApi'
import { adminApi } from '../api/adminApi'
import { JobCard } from '../components/JobCard'
import { useAuth } from '../context/AuthContext'

const EMPLOYMENT_TYPE_OPTIONS = [
  { value: '', label: '전체' },
  { value: 'FULL_TIME', label: '정규직' },
  { value: 'CONTRACT', label: '계약직' },
  { value: 'INTERN', label: '인턴' },
  { value: 'PART_TIME', label: '파트타임' },
]

export function JobListPage() {
  const { user } = useAuth()
  const [postings, setPostings] = useState([])
  const [keyword, setKeyword] = useState('')
  const [location, setLocation] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [error, setError] = useState(null)

  function reload() {
    jobApi.list({ keyword, location, employmentType })
      .then((page) => setPostings(page.content))
      .catch((err) => setError(err.message))
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword, location, employmentType])

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
      <h1>채용공고</h1>
      {error && <p className="alert" role="alert">{error}</p>}
      {user?.role === 'ADMIN' && (
        <div className="toolbar">
          <button type="button" className="btn btn-secondary" onClick={handleAddDummy}>더미데이터 추가</button>
          <button type="button" className="btn btn-secondary" onClick={handleDeleteDummy}>더미데이터 삭제</button>
        </div>
      )}
      <div className="filter-bar">
        <div className="field">
          <label htmlFor="keyword">검색어</label>
          <input id="keyword" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="회사명 검색" />
        </div>
        <div className="field">
          <label htmlFor="location">지역</label>
          <input id="location" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="지역" />
        </div>
        <div className="field">
          <label htmlFor="employmentType">고용형태</label>
          <select id="employmentType" value={employmentType} onChange={(e) => setEmploymentType(e.target.value)}>
            {EMPLOYMENT_TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
      </div>
      {postings.length === 0 ? (
        <p className="empty-state">등록된 채용공고가 없습니다.</p>
      ) : (
        <ul className="job-grid">
          {postings.map((posting) => (
            <JobCard key={posting.id} posting={posting} />
          ))}
        </ul>
      )}
    </div>
  )
}
