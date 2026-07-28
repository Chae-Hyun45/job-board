import { Link } from 'react-router'

const EMPLOYMENT_TYPE_LABEL = {
  FULL_TIME: '정규직',
  CONTRACT: '계약직',
  INTERN: '인턴',
  PART_TIME: '파트타임',
}

export function JobCard({ posting }) {
  return (
    <li>
      <Link to={`/jobs/${posting.id}`} className="job-card">
        <h2>{posting.companyName}</h2>
        <div className="job-meta">
          <span className="muted">{posting.location}</span>
          <span className="tag">{EMPLOYMENT_TYPE_LABEL[posting.employmentType] ?? posting.employmentType}</span>
        </div>
      </Link>
    </li>
  )
}
