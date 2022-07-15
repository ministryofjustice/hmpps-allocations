alter table unallocated_cases add column provider_code VARCHAR NOT NULL DEFAULT 'N03';
alter table unallocated_cases add column team_code VARCHAR;