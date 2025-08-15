# Verdict System API Documentation

## New Endpoints Added

### Voting Endpoints

#### Cast Vote
- **POST** `/api/v1/case/{id}/vote`
- **Body**: `{"vote": "guilty"}` or `{"vote": "not_guilty"}`
- **Response**:
```json
{
  "success": true,
  "message": "Vote cast successfully",
  "caseId": 1,
  "vote": "guilty",
  "verdict": {
    "status": "Guilty",
    "score": 16,
    "totalVotes": 19,
    "guiltyVotes": 17,
    "notGuiltyVotes": 2,
    "confidence": 89.47
  }
}
```

#### Get Verdict for Case
- **GET** `/api/v1/case/{id}/verdict`
- **Response**:
```json
{
  "caseId": 1,
  "verdict": {
    "status": "Guilty",
    "score": 16,
    "totalVotes": 19,
    "guiltyVotes": 17,
    "notGuiltyVotes": 2,
    "confidence": 89.47
  }
}
```

### Verdict Statistics

#### Global Verdict Stats
- **GET** `/api/v1/verdicts/stats`
- **Response**:
```json
{
  "guilty": 15,
  "notGuilty": 3,
  "onTrial": 12,
  "total": 30,
  "totalVotes": 450,
  "totalGuiltyVotes": 320,
  "totalNotGuiltyVotes": 130,
  "guiltyPercentage": 50.0,
  "notGuiltyPercentage": 10.0,
  "onTrialPercentage": 40.0,
  "averageVerdictScore": 2.5,
  "averageVotesPerCase": 15.0
}
```

### Verdict-Based Search

#### Get Cases by Verdict Status
- **GET** `/api/v1/cases/verdict/{status}`
- **Status**: `guilty`, `not_guilty`, or `on_trial`
- **Query Params**: `page=0&size=20`

#### Get Controversial Cases
- **GET** `/api/v1/cases/controversial?page=0&size=10`
- Returns Cases with close votes (many votes but near 0 score)

## Updated Entity Fields

The `Case` entity now includes:

```json
{
  "id": 1,
  "name": "John Microsoft",
  "email": "support@fake-microsoft.com",
  "phone": "+1-800-123-4567",
  "company": "Fake Microsoft Support",
  "description": "Claims to be from Microsoft...",
  "actions": "Tech Support",
  "reportedBy": "FBI Scam Alert",
  "createdAt": "2024-01-15T10:30:00",
  
  // NEW VERDICT FIELDS
  "verdictScore": 15,
  "totalVotes": 18,
  "guiltyVotes": 16,
  "notGuiltyVotes": 2,
  "lastVotedAt": "2024-02-10T14:22:00",
  
  // COMPUTED FIELDS (from @Transient methods)
  "verdictStatus": "Guilty",
  "verdictConfidence": 88.89,
  "verdictSummary": {
    "status": "Guilty",
    "score": 15,
    "totalVotes": 18,
    "guiltyVotes": 16,
    "notGuiltyVotes": 2,
    "confidence": 88.89
  }
}
```

## Verdict Logic

### Status Determination
- **Guilty**: `verdictScore > 0`
- **Not Guilty**: `verdictScore < 0`
- **On Trial**: `verdictScore = 0`

### Voting System
- **Guilty Vote**: `verdictScore++`, `guiltyVotes++`, `totalVotes++`
- **Not Guilty Vote**: `verdictScore--`, `notGuiltyVotes++`, `totalVotes++`

### Confidence Calculation
```
confidence = (max(guiltyVotes, notGuiltyVotes) / totalVotes) * 100
```

## Frontend Integration

The frontend now displays:
- **Verdict Badge**: Shows current status (Guilty/Not Guilty/On Trial)
- **Vote Count**: Shows total votes and current score
- **Confidence Level**: Percentage confidence in the verdict
- **Voting Buttons**: Allow users to cast guilty/not guilty votes
- **Real-time Updates**: Vote counts update immediately after voting

## Database Schema Updates

```sql
-- New columns added to bad_actors table
ALTER TABLE bad_actors ADD COLUMN verdict_score INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bad_actors ADD COLUMN total_votes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bad_actors ADD COLUMN guilty_votes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bad_actors ADD COLUMN not_guilty_votes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bad_actors ADD COLUMN last_voted_at TIMESTAMP;

-- Index for performance
CREATE INDEX idx_case_verdict ON bad_actors(verdict_score);
```

## Sample Voting Workflow

1. **User searches for a Cases**
2. **Results show current verdict status**
3. **User clicks guilty/not guilty button**
4. **Frontend sends POST to `/api/v1/case/{id}/vote`**
5. **Backend updates vote counts and score**
6. **Frontend updates display with new verdict data**
7. **All users see updated verdict immediately**

This creates a community-driven verification system where users can collectively determine the guilt or innocence of reported bad actors!