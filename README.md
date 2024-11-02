# splitwise_clone
This is a splitwise clone made for APP project

# Schema used 

## Expenses table - Stores all expenses
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

## Groups table - Stores all groups
CREATE TABLE expense_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,    
    name VARCHAR(255) NOT NULL UNIQUE,    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  
);
## Participants table - Stores all users/participants
CREATE TABLE participants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

## Group Members junction table - Maps participants to groups
CREATE TABLE group_members (
    group_id INT,
    participant_id INT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, participant_id),
    FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id)
);

## Expense Shares table - Maps expenses to participants and their shares
CREATE TABLE expense_shares (
    expense_id INT,
    participant_id INT,
    share_amount DECIMAL(10,2) NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (expense_id, participant_id),
    FOREIGN KEY (expense_id) REFERENCES expenses(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id)
);

# Add indexes for better query performance
CREATE INDEX idx_expense_created ON expenses(created_at);
CREATE INDEX idx_group_name ON groups(name);
CREATE INDEX idx_participant_name ON participants(name);
CREATE INDEX idx_expense_shares_paid ON expense_shares(is_paid);
