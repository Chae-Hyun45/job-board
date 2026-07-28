import { useEffect, useState } from 'react'
import { jobApi } from '../api/jobApi'
import { JobCard } from '../components/JobCard'

const EMPLOYMENT_TYPE_OPTIONS = [
  { value: '', label: '전체' },
  { value: 'FULL_TIME', label: '정규직' },
  { value: 'CONTRACT', label: '계약직' },
  { value: 'INTERN', label: '인턴' },
  { value: 'PART_TIME', label: '파트타임' },
]

export function JobListPage() {
  const [postings, setPostings] = useState([])
  const [keyword, setKeyword] = useState('')
  const [location, setLocation] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [error, setError] = useState(null)

  useEffect(() => {
    jobApi.list({ keyword, location, employmentType })
      .then((page) => setPostings(page.content))
      .catch((err) => setError(err.message))
  }, [keyword, location, employmentType])

  return (
    <div>
      <h1>채용공고</h1>
      {error && <p role="alert">{error}</p>}
      <div>
        <label htmlFor="keyword">검색어</label>
        <input id="keyword" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="회사명 검색" />
        <label htmlFor="location">지역</label>
        <input id="location" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="지역" />
        <label htmlFor="employmentType">고용형태</label>
        <select id="employmentType" value={employmentType} onChange={(e) => setEmploymentType(e.target.value)}>
          {EMPLOYMENT_TYPE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>
      <ul>
        {postings.map((posting) => (
          <JobCard key={posting.id} posting={posting} />
        ))}
      </ul>
    </div>
  )
}
