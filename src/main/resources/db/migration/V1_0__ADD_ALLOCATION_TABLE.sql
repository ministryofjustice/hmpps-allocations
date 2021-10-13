drop table if exists unallocated_cases;

CREATE TABLE unallocated_cases(
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    crn VARCHAR NOT NULL,
    tier VARCHAR NOT NULL,
    sentence_date TIMESTAMP WITH TIME ZONE,
    initial_appointment TIMESTAMP WITH TIME ZONE,
    status VARCHAR NOT NULL
);