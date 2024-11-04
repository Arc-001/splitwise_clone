# Splitwise Clone

A simple expense sharing application that helps you split bills with friends and keep track of shared expenses.

## Database Schema

The application uses the following database structure to manage expenses, groups, and participants:

### Expenses Table
Stores all expense records
```sql
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Groups Table
Manages expense sharing groups
```sql
CREATE TABLE expense_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,    
    name VARCHAR(255) NOT NULL UNIQUE,    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  
);
```

### Participants Table
Stores user information
```sql
CREATE TABLE participants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Group Members Table
Junction table mapping participants to groups
```sql
CREATE TABLE group_members (
    group_id INT,
    participant_id INT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, participant_id),
    FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id)
);
```

### Expense Shares Table
Maps expenses to participants with their respective shares
```sql
CREATE TABLE expense_shares (
    expense_id INT,
    participant_id INT,
    share_amount DECIMAL(10,2) NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (expense_id, participant_id),
    FOREIGN KEY (expense_id) REFERENCES expenses(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id)
);
```

## Indexes

The following indexes have been created to optimize query performance:

```sql
CREATE INDEX idx_expense_created ON expenses(created_at);
CREATE INDEX idx_group_name ON groups(name);
CREATE INDEX idx_participant_name ON participants(name);
CREATE INDEX idx_expense_shares_paid ON expense_shares(is_paid);
```

## Features

- Create and manage expense groups
- Add participants to groups
- Record expenses and split them among group members
- Track payments and outstanding balances
- View expense history and settlement status

## Entity Relationships

1. A participant can be part of multiple groups
2. A group can have multiple participants
3. An expense belongs to one group
4. An expense can be split among multiple participants
5. Each expense share tracks the payment status for individual participants

## Setup

1. Create a database
2. Execute the SQL scripts in the following order:
   - Create tables
   - Create indexes
3. Configure your database connection

