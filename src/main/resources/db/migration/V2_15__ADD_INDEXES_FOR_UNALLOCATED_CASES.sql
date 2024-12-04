CREATE INDEX IDX_CRN_TEAM_CODE_CONVICTION_NUMBER
    ON public.unallocated_cases (crn, team_code, conviction_number);